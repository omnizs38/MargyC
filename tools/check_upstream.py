#!/usr/bin/env python3
"""Следит за оригинальным модом: если там снова начали править — заводим issue.

    python3 tools/check_upstream.py            # проверить
    python3 tools/check_upstream.py --update   # проверить и запомнить новый коммит

Состояние лежит в .github/upstream.json. Issue заводится через API GitHub, если задан
GITHUB_TOKEN (в CI его даёт github.token).
"""
import argparse
import json
import os
import pathlib
import urllib.error
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
STATE = ROOT / ".github" / "upstream.json"
DEFAULT_UPSTREAM = "narezy/MargyC"


def api(url, token=None):
    headers = {"User-Agent": "clawdio-watch"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read().decode("utf-8"))


def latest(repo):
    commits = api(f"https://api.github.com/repos/{repo}/commits?per_page=1")
    if not commits:
        raise SystemExit("у оригинального репозитория нет коммитов")
    c = commits[0]
    return c["sha"], c["commit"]["committer"]["date"]


def known():
    if STATE.exists():
        try:
            return json.loads(STATE.read_text(encoding="utf-8")).get("sha", "")
        except Exception:
            return ""
    return ""


def open_issue(repo, sha, date, old, token):
    if not token:
        print("GITHUB_TOKEN не задан — issue не заводил")
        return
    body = (f"Оригинальный репозиторий {repo} обновился: `{sha}` ({date}).\n\n"
            f"Стоит посмотреть, что изменилось, и перенести в эту сборку.\n\n"
            f"```\ngh api repos/{repo}/compare/{old}...{sha}\n```\n")
    req = urllib.request.Request(
        f"https://api.github.com/repos/{os.environ.get('GITHUB_REPOSITORY', '')}/issues",
        data=json.dumps({"title": "Оригинальный мод снова обновляют", "body": body}).encode("utf-8"),
        headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json",
                 "User-Agent": "clawdio-watch"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            issue = json.loads(r.read().decode("utf-8"))
        print(f"issue: {issue.get('html_url')}")
    except urllib.error.HTTPError as e:
        print(f"issue не создался: {e.code} {e.read().decode('utf-8', 'replace')[:200]}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--repo", default=os.environ.get("UPSTREAM_REPO", DEFAULT_UPSTREAM))
    ap.add_argument("--update", action="store_true", help="запомнить новый коммит")
    ap.add_argument("--issue", action="store_true", help="завести issue, если upstream изменился")
    a = ap.parse_args()

    sha, date = latest(a.repo)
    old = known()
    print(f"upstream {a.repo}: {sha} ({date})")
    if not old:
        print("первый запуск — запоминаю состояние")
    elif old != sha:
        print(f"было {old} — изменилось")
        if a.issue:
            open_issue(a.repo, sha, date, old, os.environ.get("GITHUB_TOKEN"))
    else:
        print("изменений нет")
    if a.update and old != sha:
        STATE.parent.mkdir(parents=True, exist_ok=True)
        STATE.write_text(json.dumps({"sha": sha, "date": date}, indent=2) + "\n", encoding="utf-8")
        print(f"записано: {STATE}")


if __name__ == "__main__":
    main()
