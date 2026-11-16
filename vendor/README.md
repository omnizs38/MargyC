# Инструменты сборки

Здесь лежат всё, что нужно патчеру, чтобы собрать APK **без интернета**:

| Файл | Зачем |
|---|---|
| `apktool.jar` | разбирает и собирает APK (smali, ресурсы, манифест) |
| `APKEditor.jar` | склеивает XAPK / APKS / APKM в один APK |
| `r8.jar` (`d8`) | переводит классы мода в `classesN.dex` |
| `uber-apk-signer.jar` | zipalign и подпись готового APK |
| `android.jar` | API 35 для `javac` и `d8` |

`SHA256SUMS` — контрольные суммы. Патчер сверяет их перед сборкой и не даст
собрать APK подменённым инструментом.

Обновить инструмент: положить новый файл в `vendor/`, поправить `TOOLS` (или
`ANDROID_SHA256`) в `patcher.py` и пересчитать `SHA256SUMS`:

```sh
python3 - <<'PY'
import pathlib, patcher
for name, (_, expected) in patcher.TOOLS.items():
    print(patcher.sha256(pathlib.Path("vendor") / name), name)
print(patcher.sha256(pathlib.Path("vendor/android.jar")), "android.jar")
PY
```

Если `vendor/` нет (например, в архиве исходников без бинарников), патчер
скачает те же файлы с теми же хешами в `~/.cache/claude-mods`. Явная папка
задаётся через `CLAUDE_MODS_TOOLS`.

Лицензии инструментов: Apktool — Apache-2.0, APKEditor — Apache-2.0,
uber-apk-signer — Apache-2.0, R8 — BSD-3-Clause, `android.jar` — Android SDK
License. Файлы распространяются как есть, только для сборки.
