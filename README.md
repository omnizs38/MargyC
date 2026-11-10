<div align="center">

<img src="assets/icon.png" width="128" alt="Clawdio icon">

# Clawdio

**Clawdio — a mod for the Claude Android app.**<br>
Russian UI, your own themes, meme models, a Clawd pet on the composer and mods you write yourself.

> A community continuation of [MargyC](https://github.com/narezy/MargyC) by
> [narezany](https://t.me/narezany). The author keeps the attribution and his donation
> details — see [CREDITS.md](CREDITS.md) and [NOTICE](NOTICE).

<a href="https://github.com/omnizs38/MargyC/raw/Download/Clawdio.apk"><img src="https://img.shields.io/badge/Download-APK-8FD2B1?style=for-the-badge&logo=android&logoColor=white" alt="Download APK"></a>
<a href="docs/plugins.md"><img src="https://img.shields.io/badge/Write_a_mod-docs-D97757?style=for-the-badge&logo=readthedocs&logoColor=white" alt="Write a mod"></a>

<img src="https://img.shields.io/badge/version-1.4.0-555?style=flat-square" alt="version 1.4.0">
<img src="https://img.shields.io/badge/Android-10%2B-555?style=flat-square&logo=android" alt="Android 10+">
<img src="https://img.shields.io/badge/arch-arm64--v8a-555?style=flat-square" alt="arm64-v8a">
<img src="https://img.shields.io/badge/license-GPL--3.0-555?style=flat-square" alt="GPL-3.0">

<br><br>

<img src="assets/screenshot-code.jpg" width="300" alt="Clawd and a new Code session">
&nbsp;&nbsp;
<img src="assets/screenshot-models.jpg" width="300" alt="A meme model in the model picker">

</div>

## Features

| | | |
|---|---|---|
| 🇷🇺 **Russian, Belarusian, Kazakh** | the whole app in Russian, including model descriptions that come from the server, plus Belarusian and Kazakh; pick the language right in Mods |
| 🖼️ **Theme gallery** | ready themes in one tap: AMOLED, Material You, Catppuccin, Nord, Dracula, Gruvbox, Tokyo Night, Sepia |
| 🌄 **Chat wallpaper** | ready gradient presets or your own photo behind the chat, with dimming |
| 🎯 **App icon** | Clawdio, Claude orange, forest green, AMOLED or pixel Clawd on the home screen |
| 👋 **Greetings & splash** | your own home screen greetings by time of day, Clawd instead of Claude's logo on the loading screen, seasonal snow, leaves, petals or stars |
| 🕶️ **Streamer mode** | your name and email hidden everywhere: settings, profile and the home screen greeting |
| 🔤 **Font** | your font in the whole app: system, serif, Claude's serif, monospace or your own .ttf/.otf |
| 🎨 **Accent color & themes** | recolor Claude's orange, or every color of the dark and light themes. Share a theme as plain text — or just ask Claude to write one: it knows the format and your phone's Material You palette |
| 🧠 **System prompt presets** | hidden context sent with every message: the built-in presets tell Claude about the mod, your device, every Clawdio feature and your enabled mods — or write your own |
| 🤡 **Meme models** | add "Fable 6969" to the model picker. A real model of your choice answers, with its own system prompt on top. Share models as text |
| 🦀 **Clawd pet** | the app's own animated Clawd sits on top of the message box in chat and Code, follows it as it grows, can be dragged around and jumps when tapped. While Claude answers he types on a laptop, then jumps or dances — with Claude's own animations |
| 🔒 **App lock** | open Claude with your fingerprint, face or PIN; hide the chat in recent apps |
| 📝 **Conversation export** | "Download conversation (.md)" in the chat's ⋮ menu; open a .md on the Mods screen to read it as a chat and continue it in Claude |
| 🔍 **Search in chat** | "Find in chat" in the same ⋮ menu: the whole conversation with matches highlighted, arrows jump between them; a character counter at the message box |
| 📊 **Claude in numbers** | messages, active days, streak, busiest day, favourite model and an hours chart — counted on the phone only |
| 🎮 **Clawd widget & game** | a home screen widget with Clawd and today's messages; a mini-game where Clawd jumps over bugs |
| 🧩 **Custom mods** | install `.mcmod` plugins: compiled dex with a manifest, settings drawn by the app, or pick one from the built-in catalog. [Write your own](docs/plugins.md) |
| 🔄 **Updates** | Clawdio checks for new versions itself, downloads the APK and checks its signature before offering it; turn on the beta channel to get betas first |
| 🧭 **Claude compatibility** | if Claude updates under the mod, the Mods screen says so and copies a report with versions and the journal — send it and it gets fixed |
| 🌐 **Russian or English** | the Mods screen follows the app language |
| 💥 **Crash log & journal** | if the app crashes, the next start shows the stack trace; the Clawdio journal at the bottom of the Mods screen shows what the hooks did, to copy and send |

Everything lives in **Моды** (Mods) in the side menu. Changes that need a restart pile up, and a bar at
the bottom of the Mods screen restarts Claude once when you are done.

## Install

1. Download [`Clawdio.apk`](https://github.com/omnizs38/MargyC/raw/Download/Clawdio.apk) from the
   [`Download`](https://github.com/omnizs38/MargyC/tree/Download) branch.
2. Install it. Clawdio is a separate app (`cat.clawdio.claude`), so the original Claude and the
   author's MargyC stay untouched.
3. Sign in **with email** (a code arrives by mail). Google sign-in is tied to the original app and
   does not work in a mod.

Updates install over the previous version: the app downloads the new APK, checks its SHA-256 and
its signing key, and only then opens the system installer. A build signed with a different key
would not install over the current one, so it is not offered.

> The first build of this fork is signed with a different key than the author's MargyC, so it does
> not install over it. The two apps have different package names and can live side by side.

## Build from source

You need Python 3.8+ and JDK 17+. The other tools (apktool, APKEditor, d8, uber-apk-signer,
android.jar) are downloaded to `~/.cache/claude-mods` on first run.

```sh
python3 patcher.py com.anthropic.claude_<version>.xapk -o Clawdio.apk
```

The input is the original Claude as XAPK / APKS / APKM, a folder of splits or an APK.
`--abi all` keeps every architecture (only `arm64-v8a` by default).

The patcher finds every hook by strings and signatures (kotlinx.serialization descriptors,
`toString()` of data classes, Compose internals), not by obfuscated names, so it has a chance to
survive Claude updates — and when it cannot find something, it says exactly what.

To see what broke without building anything:

```sh
python3 patcher.py com.anthropic.claude_<version>.xapk --check
```

It prints every hook it found and every one it could not find — the work list for a new Claude.

**Brand, package and channel come from [`config.json`](config.json)** — no code edits needed to
build your own variant: `python3 patcher.py input.xapk --config my-config.json`.

**Signing key.** The key is not in this repository: with it anyone could sign an "update" that
installs over Clawdio. Put yours into `keystore/` (ignored by git) or point `CLAWDIO_KEYSTORE`
(+ `CLAWDIO_KEY_ALIAS`, `CLAWDIO_KEY_PASS`) to it. Without a key the patcher creates a new one, and
the result will not install over a Clawdio signed with another key.

## Release

```sh
python3 tools/release.py build claude.xapk -o Clawdio.apk
python3 tools/release.py publish Clawdio.apk --notes "Что нового" --release
python3 tools/release.py check
```

`publish` puts the APK, `version.json` and `catalog.json` into the `Download` branch and (with
`GITHUB_TOKEN`) creates a matching GitHub Release. `check` verifies the published channel against
the sources: the version in `version.json` must match `Mods.VERSION` and the SHA-256 must match
the APK. Details in [docs/RELEASING.md](docs/RELEASING.md).

**Your own channel.** Point the app at any folder with the same files: *Mods → Updates → Update
server*. See [docs/SELF-HOST.md](docs/SELF-HOST.md).

## Repository

| Path | | |
|---|---|---|
| [`config.json`](config.json) | brand, package, update channel, author and donation details of this build |
| [`patcher.py`](patcher.py) | XAPK → merged APK → manifest, resource and smali patches → build → sign |
| [`src/cat/clawdio/mods`](src/cat/clawdio/mods) | the mod itself: Mods screen, themes, prompt, meme models, Clawd, plugin loader |
| [`src/cat/clawdio/mods/api`](src/cat/clawdio/mods/api) | the API for custom mods |
| [`docs/plugins.md`](docs/plugins.md) | how to write a mod |
| [`examples/hello`](examples/hello) | an example mod |
| [`tools/build_mod.py`](tools/build_mod.py) | builds a `.mcmod` |
| [`tools/release.py`](tools/release.py) | builds and publishes a release |
| [`tools/typecheck.py`](tools/typecheck.py) | compiles the mod's code without the original APK |
| [`res/values-ru`](res/values-ru) | the Russian translation |

## Support the author

The author's donation details are in the app: *Моды → Поддержать автора*. They stay there, as the
license requires.

## Credits

- Original mod and most of the code: [narezany](https://t.me/narezany) — [MargyC](https://github.com/narezy/MargyC)
- This build and its release tooling: [omnizs38](https://github.com/omnizs38)

Full details in [CREDITS.md](CREDITS.md).

## Links

- Source code: [github.com/omnizs38/MargyC](https://github.com/omnizs38/MargyC)
- Author of the original mod: [@narezany](https://t.me/narezany)
- The author's channel: [@margyclaude](https://t.me/margyclaude)

## License

Clawdio is free software under the [GNU GPL version 3](LICENSE). You may change and share it, with
the source. Under the [additional terms](NOTICE) (GPL section 7b), every copy and fork must keep the
author attribution (narezany) and the author's donation details on the Mods screen. A fork may add
its own author and its own donation details next to them.

<sub>Icons on the Mods screen: [Material Icons](https://github.com/google/material-design-icons) by Google, Apache License 2.0.</sub>

<sub>Clawdio is an unofficial fan modification. It is not affiliated with, endorsed by or supported by
Anthropic. Claude is a trademark of Anthropic. Use at your own risk.</sub>
