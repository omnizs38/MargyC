# Credits

Clawdio is a continuation of **MargyC** — a mod for the Claude Android app by
[narezany](https://t.me/narezany).

## Who did what

| | |
|---|---|
| Original mod, most of the code, the first idea | [narezany](https://t.me/narezany) — [MargyC](https://github.com/narezy/MargyC) |
| This build: the fork, the release tooling, the update channel | [omnizs38](https://github.com/omnizs38) |

## What changed in this fork

The project was dormant since October 2026 and depended entirely on its author's infrastructure:
updates, the mod catalog, the Telegram channel and the signing key all pointed at one person.
This fork makes the project stand on its own:

- **A new brand.** The app is called **Clawdio**, the package is `cat.clawdio.claude`, the code
  lives in `cat.clawdio.mods`. MargyC stays installed under `cat.narezany.claude` — the two apps
  coexist.
- **The build is described by data, not by code.** `config.json` holds the brand, the package, the
  update channel, the author and the donation details; `patcher.py --config` builds any variant
  from it. Forking the project is now a config change, not a code change.
- **Its own update channel.** `Download` branch plus GitHub Releases, with SHA-256 and signing-key
  checks before an update is offered. Any folder with the same files can serve updates
  (*Mods → Updates → Update server*).
- **Release tooling that does not need one person.** `tools/release.py` builds, signs, publishes
  and verifies a release; CI checks that the code compiles, that the channel matches the sources
  and that the original mod has not moved.
- **The plugin API is versioned and documented** (`docs/plugins.md`); mods are built with
  `tools/build_mod.py` from a folder with a `manifest.json`.

## What did not change

The author's rights are intact, and the [NOTICE](NOTICE) is kept verbatim:

- the attribution **narezany** with the link <https://t.me/narezany> is in the footer of the Mods
  screen and in the "Write to the author" row;
- the "Support the author" block keeps the author's card and YooMoney details exactly as they
  were — not removed, not replaced, not hidden;
- the author's Telegram channel is still linked from the Mods screen.

Everything else — the fork's own name, channel, maintainer and donation details — is added *next
to* the author's, which the license explicitly allows.

## Licenses

| | |
|---|---|
| Code | GNU GPL v3, with the [additional terms](NOTICE) of the original author |
| Icons on the Mods screen | [Material Icons](https://github.com/google/material-design-icons) by Google, Apache License 2.0 |
| Claude | a trademark of Anthropic; Clawdio is not affiliated with Anthropic |
