#!/usr/bin/env python3
"""Проверка переводов без сборки APK.

    python3 tools/check_translations.py
    python3 tools/check_translations.py --values path/to/decoded/res/values

Проверяет, что переводы — корректный XML, что каждый ключ перевода есть в
английском эталоне translations/en (иначе он молча никуда не применится) и что
плейсхолдеры (%1$s, %d, %%) совпадают с английскими. С --values дополнительно
показывает, сколько строк применится к конкретной разобранной версии Claude.
"""
import argparse
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))
import patcher  # noqa: E402

PLACEHOLDER = re.compile(r"%(?:\d+\$[sd]|[sd%]|\.\d+f)")


def strings(path):
    if not path.exists():
        return {}
    root = ET.parse(path).getroot()
    return {e.get("name"): "".join(e.itertext()) for e in root if e.tag == "string"}


def plurals(path):
    if not path.exists():
        return {}
    out = {}
    for e in ET.parse(path).getroot():
        if e.tag == "plurals":
            out[e.get("name")] = sorted((i.get("quantity") or "other", "".join(i.itertext())) for i in e)
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--values", help="папка res/values, разобранная apktool, для отчёта о покрытии")
    a = ap.parse_args()

    base = ROOT / "translations"
    en = strings(base / "en" / "strings.xml")
    if not en:
        sys.exit("translations/en/strings.xml пуст или не найден")
    en_names = [e.get("name") for e in ET.parse(base / "en" / "strings.xml").getroot() if e.tag == "string"]
    if len(en_names) != len(set(en_names)):
        sys.exit("translations/en/strings.xml: повторяются имена")

    problems = 0
    for d in sorted(p for p in base.iterdir() if p.is_dir() and p.name != "en"):
        ru = strings(d / "strings.xml")
        missing = sorted(set(ru) - set(en))
        if missing:
            problems += 1
            print(f"{d.name}: ключей нет в translations/en — {len(missing)}: {', '.join(missing[:5])}…")
        for name, value in ru.items():
            if name in en and sorted(PLACEHOLDER.findall(value)) != sorted(PLACEHOLDER.findall(en[name])):
                problems += 1
                print(f"{d.name}: {name}: плейсхолдеры не совпадают\n  en: {en[name]}\n  {d.name}: {value}")

        plural_en = plurals(base / "en" / "plurals.xml")
        plural_ru = plurals(d / "plurals.xml")
        unknown = sorted(set(plural_ru) - set(plural_en))
        if unknown:
            problems += 1
            print(f"{d.name}: множественных нет в en — {', '.join(unknown)}")

        if a.values:
            fresh = strings(pathlib.Path(a.values) / "strings.xml")
            pairs = patcher.align_translations(
                [(n, v) for n, v in en.items()], [(n, v) for n, v in fresh.items()])
            covered = sum(1 for i, (n, _) in enumerate(en.items()) if n in ru and i in pairs)
            print(f"{d.name}: применится {covered} из {len(ru)} строк "
                  f"(всего в Claude {len(fresh)}, переведено {len(ru)})")

    if problems:
        sys.exit(f"ошибок в переводах: {problems}")
    print("переводы в порядке")


if __name__ == "__main__":
    main()
