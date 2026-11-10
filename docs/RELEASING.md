# Releasing

Everything here is scripted; no manual steps, no secrets in the repository.

## One-time setup

1. **A signing key.** The key is what makes an update install over the previous build, so it must
   be the same every time and must never be committed.

   ```sh
   keytool -genkeypair -keystore keystore/clawdio.jks -alias clawdio \
     -storepass "$PASS" -keypass "$PASS" -keyalg RSA -keysize 2048 -validity 36500 \
     -dname "CN=Clawdio, O=Clawdio"
   ```

   `keystore/` is in `.gitignore`. In CI, keep the key as a base64 secret and restore it in the
   job:

   ```sh
   echo "$KEYSTORE_B64" | base64 -d > keystore/clawdio.jks
   ```

   The patcher reads `CLAWDIO_KEYSTORE`, `CLAWDIO_KEY_ALIAS` and `CLAWDIO_KEY_PASS`.

2. **A GitHub token** with `contents: write` (for pushing to the channel branch) and
   `workflow`/`repo` scope for releases: `GITHUB_TOKEN`.

## A release

```sh
# 1. build from the current Claude
python3 tools/release.py build com.anthropic.claude_26100220.xapk -o Clawdio.apk

# 2. bump the version in src/cat/clawdio/mods/Mods.java (VERSION) — the channel must match it

# 3. publish: APK + version.json + catalog.json into the Download branch, and a GitHub Release
python3 tools/release.py publish Clawdio.apk --notes "Что нового" --release

# 4. check that the channel matches the sources
python3 tools/release.py check
```

`publish` writes the version, the SHA-256 of the APK and the file size into `version.json`:

```json
{"release": {"version": "1.4.0", "beta": 0, "apk": "Clawdio.apk", "notes": "...",
             "sha256": "...", "size": 30900000}}
```

A beta goes to the `beta` key and gets the file name `Clawdio_beta.apk`:

```sh
python3 tools/release.py publish Clawdio.apk --version 1.5 --beta 1 --notes "Что нового"
```

Betas are only offered to users who switched the beta channel on in *Mods → Updates*.

## The update channel

Two places, one content:

- **`Download` branch** — `Clawdio.apk`, `version.json`, `catalog.json`. The app reads
  `version.json` over HTTPS and downloads the APK from the same branch.
- **GitHub Releases** — the same APK as a release asset. If `version.json` cannot be fetched, the
  app falls back to the latest release.

Both are filled by `tools/release.py publish --release`.

Before an update is offered, the app checks the APK:

- the SHA-256 must match the one in `version.json`;
- the signing certificate must be the same as the installed app's — a build signed with another
  key would not install over the current one, so it is refused with an explanation.

## What CI checks

| Workflow | What it does |
|---|---|
| `ci.yml` | the mod's code compiles (`tools/typecheck.py`), the Python scripts parse, `config.json` and the resources are valid, the version in the code is in the README |
| `channel.yml` | the published channel matches the sources (version and SHA-256), and the original mod is watched: if it moves again, an issue is opened and the change is noted |

## When Claude updates

The patcher looks for hooks by strings and signatures, so it survives most updates — but not all.
When it fails, it says exactly what it could not find.

1. Take the new Claude XAPK and run `python3 tools/release.py build new.xapk -o Clawdio.apk`.
2. If the patcher fails, the error names the missing hook. Fix it in `patcher.py` (the `patch_*`
   functions) and build again.
3. Bump `VERSION` in `Mods.java`, then `publish` and `check`.

Users on the new Claude version can send the journal (*Mods → Journal*) — it lists the Claude
version the app started with and what every hook did.
