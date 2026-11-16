# Native libraries

`arm64-v8a` libraries used when the input Claude APK is a **split base** without the
ABI split (it has no `lib/` directory). They come from the previous full Claude build
of the same app family (1.261002.20) and are the same libraries the new code loads:
`libsqliteJni`, `libvoiceclient`, `libwebrtc_apm`, `libopus_jni`, `libsentry`,
`libandroidmath`, `libandroidx.graphics.path`, `libdatastore_shared_counter`.

If the input APK has its own `lib/`, these are not used. `patcher.py --abi` still
removes every architecture except the selected one.

`SHA256SUMS` is checked by CI together with the build tools.
