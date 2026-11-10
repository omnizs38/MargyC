# Contributing

Thanks for helping. Clawdio is a mod for somebody else's app: every change is a patch against
Claude's own code, which changes with every update of Claude. Small, careful steps matter.

## Before you start

- **Read [CREDITS.md](CREDITS.md)** — the code is GPL-3.0 with the original author's additional
  terms. His attribution and his donation details must stay in the app; your own may be added next
  to them.
- **Open an issue first** for anything bigger than a bug fix, so two people don't do the same work.

## The build

```sh
python3 patcher.py com.anthropic.claude_<version>.xapk -o Clawdio.apk
```

Needs Python 3.8+, JDK 17+ and the original Claude APK (XAPK / APKS / APKM / a folder of splits /
an APK). The build tools are downloaded to `~/.cache/claude-mods` on the first run.

Without the original APK you can still compile the mod's own code:

```sh
python3 tools/typecheck.py
```

## Where things live

| | |
|---|---|
| `src/cat/clawdio/mods/` | the mod: Mods screen, themes, prompt, meme models, Clawd, plugins |
| `src/cat/clawdio/mods/api/` | the plugin API — mods are written against it |
| `patcher.py` | patches Claude's smali, manifest and resources; every hook lives in a `patch_*` function |
| `config.json` | brand, package, update channel, author, donation details |
| `res/` | the mod's own resources and the translations of Claude's strings |
| `tools/` | build, typecheck, release and channel-checking scripts |

## Making a change

- **The patcher looks for hooks by strings and signatures**, never by obfuscated class names:
  those change with every Claude update. If a hook cannot be found, the patcher stops with a
  message naming what it looked for — that message is the bug report.
- **Strings in the mod's screens are written in Russian** and translated in `L.java` (the
  `put("русский", "english")` table). Add the English side for every new string.
- **New behaviour in a plugin** goes behind the plugin API, not into the mod's core: the API is
  what other people's mods are written against.
- **Bump `VERSION` in `Mods.java`** when you change the app, and publish with
  `tools/release.py publish` — the channel check compares the two.

## Style

- Java 8 source level, four spaces, no AndroidX, no libraries: the mod runs inside Claude's
  process.
- Comments in the language the file already uses (the mod's code is in Russian).
- Keep the app working without any network: the channel, the catalog and the theme gallery all
  have to fail quietly.

## Pull requests

- One topic per pull request.
- `python3 tools/typecheck.py` must pass; CI runs the same check.
- Say in the description which Claude version you built against and what you changed.
