# Security

## The signing key

The key that signs the APK is what makes an update install over the previous build. Because of
that, it is treated as a secret:

- the key is **not** in the repository (`keystore/` is in `.gitignore`);
- in CI it is stored as a base64 secret and written to `keystore/` only inside the job;
- the app checks the signing certificate of every update before offering it, so a build signed
  with a different key is refused with an explanation instead of failing at install time;
- if the key leaks, every build signed with it is compromised — rotate it, bump the version and
  tell users to reinstall manually.

Never sign a build with the key of another project, and never publish a key in an issue or a
commit.

## Reporting a problem

Open an issue on GitHub. For a crash, send the journal (*Mods → Journal*): it lists the Claude
version the app started with and what every hook did, and it is copied to the clipboard with one
tap. There is no analytics and nothing leaves the phone unless you send it yourself.

## What the app sends over the network

| | |
|---|---|
| Update check | `version.json` from the update channel (the `Download` branch of the repository, or the server you set) |
| Mod catalog | `catalog.json` and the `.mcmod` files from the same channel |
| Everything else | nothing: chats, themes, statistics and the journal stay on the phone |

The update channel can be pointed anywhere: *Mods → Updates → Update server*.
