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
# 0. check first: find every hook and see what broke, without building
python3 patcher.py com.anthropic.claude_26100220.xapk --check

# 1. build from the current Claude
python3 tools/release.py build com.anthropic.claude_26100220.xapk -o Clawdio.apk

# 2. bump the version in src/cat/clawdio/mods/Mods.java (VERSION) — the channel must match it

# 3. publish: APK + version.json + catalog.json into the Download branch, and a GitHub Release
python3 tools/release.py publish Clawdio.apk --notes "Что нового" --release

# 4. check that the channel matches the sources
python3 tools/release.py check
```

`--check` runs the patch phase and stops: it prints every hook it found and every one it could
not find, without building or signing. Use it when Claude updates — it turns "the patcher broke"
into a list of the exact places to repair, and that list is what belongs in an issue.

After a successful build, put the version code of the Claude you built against into
`config.json` (`claude_tested`): the app compares it with the installed Claude and warns users
when Claude has moved on.

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

1. Take the new Claude XAPK and run `python3 patcher.py new.xapk --check` — the list of missing
   hooks is the work list.
2. If the patcher fails, the error names the missing hook. Fix it in `patcher.py` (the `patch_*`
   functions) and check again.
3. Bump `VERSION` in `Mods.java`, put the new Claude's version code into `claude_tested` in
   `config.json`, then `publish` and `check`.

Users on the new Claude version can send the journal (*Mods → Journal*) — it lists the Claude
version the app started with and what every hook did. *Mods → Claude compatibility* shows the
same information as a warning when the installed Claude is newer than the tested one, and copies a
ready-to-send report.

## Obfuscated resource names

Since v1.261106.19 Claude ships with **resource names obfuscated by R8 to `APKTOOL_DUMMYVAL_*`**:
`public.xml` keeps every resource id, but the names are gone. This breaks any hook that refers
to a resource by its name. The patcher does not rely on the original names for its own files:

- the app label points at `@string/clawdio_app_name` (ours, defined in `res/values/clawdio_strings.xml`);
- the icon aliases and the launcher activity references use `@string/clawdio_app_name` too;
- our `res/` files go into the APK as-is under their own names.

**Not recoverable** without the original `public.xml` keys: the entry points into Claude's own UI
(the side-menu / “Моды” item lived in a resource that no longer has a stable name), the
`TextStringSimpleElement` Compose text node for the String-translation filter, and the old
REST chat engine body and Luna font-family wiring. Those three are optional and degrade
gracefully. The launcher shortcut `Clawdio → Моды` in `res/xml/clawdio_shortcuts.xml` is our
own entry point and does not depend on any of them.

If Anthropic restores readable names, the optional hooks return by themselves.
