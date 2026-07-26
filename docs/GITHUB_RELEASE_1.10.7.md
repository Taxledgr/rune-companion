# Rune Companion 1.10.7 — public testing build

Rune Companion is a passive Android companion for Old School RuneScape mobile.
Version 1.10.7 adds a stateful in-overlay Wiki reader: minimise a loaded Wiki
page to the companion bubble, then tap the bubble to resume the same article,
browser history, and scroll position.

This release also includes the configurable companion overlay, Shooting Star
reports and travel guidance, quest preparation and walkthroughs, public
Hiscores tracking, local journals, timers, filters, and OSRS Wiki references.

## Download

Download **`rune-companion-1.10.7-debug.apk`** from the Assets section below.
The source-code ZIP and TAR archives are not installable Android apps.

## Install

1. Download and open the APK on the Android phone.
2. Allow **Install unknown apps** for the browser or file manager if Android
   requests it.
3. Confirm the app name is **Rune Companion** and install it.
4. Turn **Install unknown apps** back off afterward if it is no longer needed.
5. When updating, install over the existing app. Do not uninstall it or clear
   its data first.

Full instructions: [Installing Rune Companion on Android](https://github.com/Taxledgr/rune-companion/blob/v1.10.7/docs/INSTALLING.md)

## Integrity

SHA-256:

```text
D105BFB9E3949E583A0AFBC59B4287FD268D08435F06997430379A756C44DFDF
```

The same checksum is attached as
`rune-companion-1.10.7-debug.apk.sha256`.

## Testing and security status

- 79 unit tests passed.
- Debug and release builds completed.
- Android debug and release lint passed.
- Manifest and security invariant checks passed.
- GitHub Actions build, dependency review, and CodeQL checks passed.
- The tested updater preserved existing private preferences on the development
  phone.

This APK is a **debug-signed public testing build**, not a permanently
release-signed production build. Rune Companion does not request a Jagex login,
Accessibility Service access, screen capture, contacts, SMS, microphone,
camera, or broad file access.
