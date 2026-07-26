# Security policy

Rune Companion never asks for a Jagex password, bank PIN, authenticator code,
session token, or recovery information. It uses public OSRS data and information
the user enters manually.

## Reporting a vulnerability

Please use GitHub's private vulnerability reporting or a private repository
security advisory. Do not place exploit details, personal data, credentials, or
signing material in a public issue.

Include the affected version, Android version, reproduction steps, impact, and
any proposed remediation. Reports involving phishing, unsafe overlay behavior,
arbitrary URL loading, backup disclosure, exported Android components, or update
signing are treated as high priority.

## Supported versions

Security fixes are applied to the latest development version. This project is
currently a personal, sideloaded Android application and is not an official
Jagex, RuneLite, or OSRS Wiki product.

## Release integrity

Release signing keys and passwords must never be committed to this repository.
Signed builds read these values only from local environment variables:

- `RUNE_COMPANION_KEYSTORE_FILE`
- `RUNE_COMPANION_KEYSTORE_PASSWORD`
- `RUNE_COMPANION_KEY_ALIAS`
- `RUNE_COMPANION_KEY_PASSWORD`

Keep the keystore offline with a separate encrypted backup. Android will reject
updates signed by a different key.
