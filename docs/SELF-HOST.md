# Running your own update channel

The app does not care where the updates come from: it reads `version.json` from a folder you
choose. That folder can be a GitHub branch, a static site, a home server or anything that serves
files over HTTPS.

## The layout

```
<your folder>/
  version.json     what is available
  Clawdio.apk      the build (the name must match "apk" in version.json)
  catalog.json     optional: the mod catalog
  *.mcmod          optional: the mods themselves
```

`version.json`:

```json
{
  "release": {
    "version": "1.4.0",
    "beta": 0,
    "apk": "Clawdio.apk",
    "notes": "Что нового",
    "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "size": 30900000
  }
}
```

| Field | | |
|---|---|---|
| `version` | version of the mod, `major.minor.patch` | |
| `beta` | `0` for a release, `1`, `2`, … for betas | |
| `apk` | file name next to `version.json`, or a full URL | |
| `notes` | shown in the update window | |
| `sha256` | optional but recommended: the app checks the downloaded file against it | |
| `size` | optional, informational | |

A second key `beta` holds the beta build:

```json
{"release": {...}, "beta": {"version": "1.5", "beta": 1, "apk": "Clawdio_beta.apk", "notes": "..."}}
```

Betas are only offered to users who turned the beta channel on.

## Pointing the app at it

*Mods → Updates → Update server* — the address of the folder, without a trailing slash:

```
https://raw.githubusercontent.com/owner/repo/Download
https://mysite.example.com/clawdio
```

Leave it empty to go back to the channel of the installed build.

## Rules worth keeping

- **The signing key decides everything.** An update only installs over the current app if it is
  signed with the same key. The app checks this before offering an update and refuses a build
  signed with another one — it would fail to install anyway.
- **Serve over HTTPS.** Plain HTTP is blocked by Android for anything but localhost.
- **Keep `version.json` and the APK in sync.** The app caches the answer for four hours; a broken
  `version.json` means no updates at all.
- **Version numbers must grow.** The app compares `1.4.0` as `1 * 1000000 + 4 * 10000 + 0 * 100`,
  and a release of the same version is always newer than its own beta.

## Publishing without the scripts

`tools/release.py publish` does this for a GitHub repository, but the format is simple enough to
write by hand: put the files in a folder, compute the SHA-256 (`sha256sum Clawdio.apk`), and write
`version.json`.
