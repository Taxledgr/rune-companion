# Compliance design notes

Rune Companion is designed as a passive, user-controlled Android companion rather than a replacement game client.

## Project rules

1. Do not automate gameplay, taps, gestures, or decisions.
2. Do not inject into, hook, decompile, modify, or impersonate the official OSRS client.
3. Do not read process memory, intercept game traffic, or access private client state.
4. Do not use Android Accessibility Services to interact with the game.
5. Keep information displays general-purpose and user initiated.
6. Treat external community feeds as read-only, rate-limited dependencies.
7. Review Jagex's current rules before adding any feature that interprets live gameplay.

## Current Android behaviour

The app fetches public Shooting Stars reports over HTTPS and displays them in its own activity, Android notifications, or an Android application-overlay window. The user must explicitly grant notification and overlay permissions. The user must also start the overlay, and a persistent Android notification is shown while it is running.

Saved alert filters and duplicate-notification identifiers remain on the device. Version 0.2 does not request screen-capture permission and contains no OCR implementation.

## References

- [Jagex third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)
- [Jagex rules](https://legal.jagex.com/docs/rules)
- [Android application overlay permission](https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW)
- [Star Miners live map](https://map.starminers.site/)

Rules and platform policies can change. This file records the project's engineering boundary; it is not legal advice or a guarantee that Jagex will approve a particular feature.
