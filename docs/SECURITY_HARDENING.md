# Rune Companion security model

## Trust boundaries

- Rune Companion has no Jagex login, game session, accessibility, screen-capture,
  SMS, contacts, storage, microphone, camera, or location access.
- The floating panel requires Android's user-granted overlay permission. The
  overlay service is private to the app and cannot be started by another app.
- Network traffic is read-only HTTPS to exact, allowlisted OSRS, OSRS Wiki,
  Jagex, GitHub, and Star Miners hosts. System certificate authorities are used;
  user-installed certificate authorities and cleartext HTTP are rejected.
- Public feeds are untrusted input. Responses are size-limited before parsing.
- OSRS Wiki pages render without JavaScript, local-file access, content-provider
  access, mixed HTTP content, third-party cookies, or WebView debugging.

## Private data

Player names and companion settings live in Android private app storage. Android
cloud backup is limited to Rune Companion preference files and only runs when
the device supports encrypted backup. Device-to-device transfer remains allowed
so users can preserve their setup.

Manual exports use versioned AES-256-GCM authenticated encryption. New RC2
exports use PBKDF2-HMAC-SHA256 with 310,000 iterations and a 32-byte random salt.
RC1 exports remain readable for migration. Imports are bounded, type-checked,
fully parsed before writing, and rolled back if Android cannot commit every file.

Encrypted exports placed on the clipboard are marked sensitive and automatically
removed after two minutes if they have not been replaced.

## Anti-phishing and platform protections

- External URLs require HTTPS, an exact host match, no embedded credentials, and
  the standard TLS port.
- Android 12+ hides other apps' overlay windows while Rune Companion is open.
- Obscured touches are rejected and Android 13+ recents screenshots are disabled.
- Notification details are private on the lock screen.
- Components are explicitly exported only where Android requires it.
- Android 11 is the minimum supported release to avoid older task-affinity and
  clipboard weaknesses.

## Build and update security

- Release builds disable debugging, run R8 code/resource shrinking, and support
  APK signature schemes v2-v4 when release signing variables are present.
- Keystores, APKs, AABs, and signing properties are ignored by Git.
- CI runs unit tests, Android lint, manifest/security assertions, release
  assembly, dependency review, and CodeQL Kotlin analysis.
- Dependabot checks Gradle and GitHub Actions dependencies weekly.

The current phone installation is debug-signed. Moving to a protected release
key requires one controlled backup/uninstall/reinstall/import migration because
Android does not allow an installed package to change signing identity.
