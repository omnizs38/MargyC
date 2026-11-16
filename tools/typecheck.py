#!/usr/bin/env python3
"""Проверка компиляции кода мода без оригинального APK.

    python3 tools/typecheck.py

Компилирует src/ как это делает patcher.py (javac --release 8 против android.jar),
но вместо настоящего Names.java берёт заглушку tools/typecheck/Names.java.
Нужен JDK 17+; android.jar и r8 уже лежат в vendor/ — сеть не нужна.
"""
import pathlib
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))
import patcher  # noqa: E402


def tools():
    # vendor/ рядом с репозиторием: без сети; иначе кэш ~/.cache/claude-mods
    return patcher.ensure_tools()


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
