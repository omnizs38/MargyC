#!/usr/bin/env python3
"""Сборка и публикация релиза Clawdio.

    python3 tools/release.py build claude.xapk -o Clawdio.apk
    python3 tools/release.py publish Clawdio.apk --version 1.4.0 --notes "Что нового"
    python3 tools/release.py check

Канал обновлений — ветка Download в репозитории: там лежат APK, version.json и catalog.json.
publish кладёт туда новую сборку и (если задан GITHUB_TOKEN) создаёт релиз на GitHub.
check сверяет опубликованное с исходниками: хеш APK, версия в version.json и версия в коде.
"""
import argparse
import hashlib
import json
import os
import pathlib
import subprocess
import sys
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))
import patcher  # noqa: E402

CHANNEL_FILES = ("version.json", "catalog.json")


def git(*args, **kw):
    r = subprocess.run(["git", *args], cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                       text=True, encoding="utf-8", errors="replace", **kw)
    if r.returncode != 0:
        raise patcher.PatchError(f"git {' '.join(args[:3])}: {r.stderr.strip() or r.stdout.strip()}")
    return r.stdout.strip()


def config():
    return json.loads((ROOT / "config.json").read_text(encoding="utf-8"))


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            h.update(block)
    return h.hexdigest()


def code(version, beta=0):
    """Номер сборки: как в Mods.code — релиз всегда новее своей беты."""
    p = [int(x) for x in version.split(".")]
    while len(p) < 3:
        p.append(0)
    return p[0] * 1000000 + p[1] * 10000 + p[2] * 100 + (beta if beta > 0 else 99)


def app_version():
    """Версия из кода мода: src/cat/clawdio/mods/Mods.java."""
    src = (ROOT / "src" / "cat" / "clawdio" / "mods" / "Mods.java").read_text(encoding="utf-8")
    for line in src.splitlines():
        if "static final String VERSION" in line:
            return line.split('"')[1]
    raise patcher.PatchError("не нашёл VERSION в Mods.java")


# ------------------------------------------------------------------- сборка

def build(a):
    cmd = [sys.executable, str(ROOT / "patcher.py"), a.input, "-o", a.output]
    if a.config:
        cmd += ["--config", a.config]
    if a.abi != "arm64-v8a":
        cmd += ["--abi", a.abi]
    patcher.run(cmd)
    print(f"собрано: {a.output}")


# ---------------------------------------------------------------- публикация

def read_channel(branch, repo):
    """Что уже лежит в ветке канала."""
    out = {}
    for name in CHANNEL_FILES:
        url = f"https://raw.githubusercontent.com/{repo}/{branch}/{name}"
        try:
            with urllib.request.urlopen(url, timeout=20) as r:
                out[name] = r.read().decode("utf-8-sig")  # utf-8-sig: с BOM и без него
        except Exception:
            out[name] = None
    return out


def publish(a):
    cfg = config()
    repo = cfg.get("repo") or ""
    branch = a.branch or cfg.get("channel_branch") or "Download"
    if not repo:
        raise patcher.PatchError("в config.json нет repo")
    apk = pathlib.Path(a.apk)
    if not apk.exists():
        raise patcher.PatchError(f"нет файла {apk}")

    version = a.version or app_version()
    beta = a.beta
    apk_name = f"{cfg.get('brand', 'Clawdio')}{'_beta' if beta else ''}.apk"
    digest = sha256(apk)

    current = read_channel(branch, repo)
    try:
        versions = json.loads(current["version.json"]) if current.get("version.json") else {}
    except Exception:
        versions = {}
    if not isinstance(versions, dict):
        versions = {}

    key = "beta" if beta else "release"
    entry = {
        "version": version,
        "beta": beta,
        "apk": apk_name,
        "notes": a.notes or "",
        "sha256": digest,
        "size": apk.stat().st_size,
    }
    versions[key] = entry
    version_json = json.dumps(versions, ensure_ascii=False, indent=2) + "\n"

    files = {apk_name: apk, "version.json": pathlib.Path(tempfile_name(version_json))}
    catalog = pathlib.Path(a.catalog) if a.catalog else None
    if catalog and catalog.exists():
        files["catalog.json"] = catalog
    elif current.get("catalog.json"):
        files["catalog.json"] = pathlib.Path(tempfile_name(current["catalog.json"]))

    commit_files(files, branch, f"{cfg.get('brand', 'Clawdio')} {version}"
                  + (f" beta {beta}" if beta else "") + ": новая сборка")

    if a.release:
        create_release(cfg, repo, version, beta, a.notes or "", apk, apk_name, digest)
    print(f"опубликовано: {repo}@{branch} — {apk_name} ({digest[:12]}…)")


def tempfile_name(text):
    import tempfile
    fd, path = tempfile.mkstemp(prefix="clawdio-", suffix=".tmp")
    with os.fdopen(fd, "w", encoding="utf-8") as f:
        f.write(text)
    return path


def commit_files(files, branch, message):
    """Кладёт файлы в ветку канала, не трогая рабочую копию."""
    import tempfile
    parent = None
    try:
        git("fetch", "origin", branch)
        parent = f"origin/{branch}"
    except patcher.PatchError:
        parent = None  # ветки канала ещё нет — начинаем с пустого дерева

    with tempfile.TemporaryDirectory() as tmp:
        index = pathlib.Path(tmp) / "index"
        env = dict(os.environ, GIT_INDEX_FILE=str(index))
        run = lambda *args: subprocess.run(["git", *args], cwd=ROOT, env=env, stdout=subprocess.PIPE,
                                            stderr=subprocess.PIPE, text=True, encoding="utf-8",
                                            errors="replace", check=True)
        if parent:
            run("read-tree", parent)
        else:
            run("read-tree", "--empty")
        for name, path in files.items():
            blob = subprocess.run(["git", "hash-object", "-w", str(path)], cwd=ROOT, stdout=subprocess.PIPE,
                                  text=True, encoding="utf-8", check=True).stdout.strip()
            run("update-index", "--add", "--cacheinfo", f"100644,{blob},{name}")
        tree = run("write-tree").stdout.strip()
        args = ["commit-tree", tree, "-m", message]
        if parent:
            args += ["-p", parent]
        commit = subprocess.run(["git", *args], cwd=ROOT, stdout=subprocess.PIPE, text=True,
                                encoding="utf-8", check=True).stdout.strip()
        subprocess.run(["git", "push", "origin", f"{commit}:refs/heads/{branch}"], cwd=ROOT,
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=True)


def create_release(cfg, repo, version, beta, notes, apk, asset_name, digest):
    """Релиз на GitHub: та же сборка, но и в Releases (через API)."""
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if not token:
        print("GITHUB_TOKEN не задан — релиз на GitHub пропущен")
        return
    tag = f"v{version}" + (f"-beta{beta}" if beta else "")
    body = json.dumps({
        "tag_name": tag,
        "name": f"{cfg.get('brand', 'Clawdio')} {version}" + (f" beta {beta}" if beta else ""),
        "body": notes or "",
        "draft": False,
        "prerelease": bool(beta),
    }).encode("utf-8")
    req = urllib.request.Request(f"https://api.github.com/repos/{repo}/releases", data=body,
                                 headers={"Authorization": f"Bearer {token}",
                                          "Content-Type": "application/json",
                                          "User-Agent": "clawdio-release"})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            release = json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raise patcher.PatchError(f"GitHub API: {e.code} {e.read().decode('utf-8', 'replace')[:200]}")

    upload = f"https://uploads.github.com/repos/{repo}/releases/{release['id']}/assets?name={asset_name}"
    data = pathlib.Path(apk).read_bytes()
    boundary = "----clawdio"
    head = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"asset\"; filename=\"{asset_name}\"\r\n"
            "Content-Type: application/vnd.android.package-archive\r\n\r\n").encode("utf-8")
    tail = f"\r\n--{boundary}--\r\n".encode("utf-8")
    req = urllib.request.Request(upload, data=head + data + tail, headers={
        "Authorization": f"Bearer {token}",
        "Content-Type": f"multipart/form-data; boundary={boundary}",
        "User-Agent": "clawdio-release",
    })
    try:
        with urllib.request.urlopen(req, timeout=600) as r:
            r.read()
    except urllib.error.HTTPError as e:
        raise patcher.PatchError(f"загрузка в релиз: {e.code} {e.read().decode('utf-8', 'replace')[:200]}")
    print(f"релиз: {release['html_url']}")


# ------------------------------------------------------------------- проверка

def check(a):
    """Сверяет канал с исходниками: версия в коде, версия в version.json, хеш APK."""
    cfg = config()
    repo = cfg.get("repo") or ""
    branch = a.branch or cfg.get("channel_branch") or "Download"
    problems = []

    want = app_version()
    if not a.version:
        a.version = want

    channel = read_channel(branch, repo)
    if not channel.get("version.json"):
        print(f"в ветке {branch} нет version.json: сборка ещё не выложена")
        return
    versions = json.loads(channel["version.json"])
    release = versions.get("release") or {}
    beta = versions.get("beta") or {}

    if not release and not beta:
        print("канал пуст: сборка ещё не выложена")
        return

    if release.get("version") != want:
        problems.append(f"version.json: релиз {release.get('version')}, а в коде {want}")
    for key, entry in (("release", release), ("beta", beta)):
        if not entry:
            continue
        url = f"https://raw.githubusercontent.com/{repo}/{branch}/{entry.get('apk', '')}"
        try:
            with urllib.request.urlopen(url, timeout=120) as r:
                h = hashlib.sha256()
                for block in iter(lambda: r.read(1 << 20), b""):
                    h.update(block)
            digest = h.hexdigest()
        except Exception as e:
            problems.append(f"{key}: не скачался APK ({e})")
            continue
        if entry.get("sha256") and entry["sha256"] != digest:
            problems.append(f"{key}: хеш APK не совпадает с version.json")
        print(f"{key}: {entry.get('version')} — {entry.get('apk')} {digest[:12]}…")

    for p in problems:
        print(f"ошибка: {p}")
    if problems:
        sys.exit(1)
    print("канал в порядке")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)

    b = sub.add_parser("build", help="собрать APK из оригинального Claude")
    b.add_argument("input", help="XAPK/APKS/APKM или APK оригинального Claude")
    b.add_argument("-o", "--output", default="Clawdio.apk")
    b.add_argument("--config", help="файл настроек сборки")
    b.add_argument("--abi", default="arm64-v8a")

    p = sub.add_parser("publish", help="положить сборку в ветку канала и сделать релиз")
    p.add_argument("apk")
    p.add_argument("--version", help="версия (по умолчанию из кода)")
    p.add_argument("--beta", type=int, default=0, help="номер беты (0 — релиз)")
    p.add_argument("--notes", default="", help="что нового")
    p.add_argument("--branch", help="ветка канала (по умолчанию из config.json)")
    p.add_argument("--catalog", help="свой catalog.json")
    p.add_argument("--release", action="store_true", help="создать ещё и релиз на GitHub")

    c = sub.add_parser("check", help="сверить опубликованный канал с исходниками")
    c.add_argument("--branch")
    c.add_argument("--version")

    a = ap.parse_args()
    try:
        if a.cmd == "build":
            build(a)
        elif a.cmd == "publish":
            publish(a)
        else:
            check(a)
    except patcher.PatchError as e:
        sys.exit(f"error: {e}")


if __name__ == "__main__":
    main()
