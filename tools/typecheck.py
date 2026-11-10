#!/usr/bin/env python3
"""Проверка компиляции кода мода без оригинального APK.

    python3 tools/typecheck.py

Компилирует src/ как это делает patcher.py (javac --release 8 против android.jar),
но вместо настоящего Names.java берёт заглушку tools/typecheck/Names.java.
Нужен JDK 17+; android.jar и r8 скачиваются в ~/.cache/claude-mods при первом запуске.
"""
import pathlib
import shutil
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))
import patcher  # noqa: E402


def tools():
    d = patcher.tools_dir()
    if not (d / "android.jar").exists():
        z = d / "platform.zip"
        patcher.download(patcher.PLATFORM_ZIP, z)
        import zipfile
        with zipfile.ZipFile(z) as zf:
            entry = next(n for n in zf.namelist() if n.endswith("/android.jar"))
            (d / "android.jar").write_bytes(zf.read(entry))
        z.unlink()
    if not shutil.which("javac"):
        raise patcher.PatchError("javac не найден, нужна JDK 17+")
    return d


def main():
    try:
        t = tools()
        sources = sorted(str(p) for p in (ROOT / "src").rglob("*.java"))
        sources.append(str(ROOT / "tools" / "typecheck" / "Names.java"))
        with tempfile.TemporaryDirectory() as tmp:
            classes = pathlib.Path(tmp) / "classes"
            classes.mkdir()
            patcher.run(["javac", "--release", "8", "-nowarn", "-encoding", "UTF-8",
                         "-cp", t / "android.jar", "-d", classes, *sources])
        print(f"ok: {len(sources) - 1} файлов собраны")
    except patcher.PatchError as e:
        sys.exit(f"error: {e}")


if __name__ == "__main__":
    main()
