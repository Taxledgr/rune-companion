# Installing Rune Companion on Android

## Official download

Download Rune Companion only from the public
[Taxledgr/rune-companion releases page](https://github.com/Taxledgr/rune-companion/releases).
For version 1.10.7, select `rune-companion-1.10.7-debug.apk`. The
automatically generated source-code ZIP and TAR archives are not installable
Android apps.

Version 1.10.7 is a debug-signed public testing build. A permanent
release-signed build is still recommended before treating Rune Companion as a
production app.

## Install

1. Download the APK on the Android phone.
2. Open the completed download.
3. If Android blocks it, allow **Install unknown apps** for the browser or file
   manager used to open the APK.
4. Confirm that Android identifies the app as **Rune Companion**, then install
   it.
5. Turn **Install unknown apps** back off afterward if it is not needed for
   anything else.
6. Open Rune Companion and grant only the overlay and notification permissions
   needed for the selected features.

Rune Companion does not request a Jagex login, Accessibility Service access,
screen capture, contacts, SMS, microphone, camera, or broad file access.

## Update without losing saved data

Install a newer APK directly over the existing Rune Companion installation.
Do not uninstall the old version first, and do not clear its app data. Android
will preserve saved accounts, profiles, filters, journal entries, and progress
when the new APK uses the same application ID and signing key.

If Android reports that the update is incompatible, stop rather than
uninstalling. That message usually means the APK was signed with a different
key.

## Verify version 1.10.7

The expected SHA-256 checksum is:

```text
D105BFB9E3949E583A0AFBC59B4287FD268D08435F06997430379A756C44DFDF
```

On Windows, verify a downloaded copy with:

```powershell
Get-FileHash .\rune-companion-1.10.7-debug.apk -Algorithm SHA256
```

The reported hash must exactly match the value above.
