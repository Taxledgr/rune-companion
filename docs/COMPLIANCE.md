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

Saved alert filters, reminders, journal entries, loadouts, goals, stopwatches, watchlists, and hiscore snapshots remain on the device. Price and hiscore lookups contain only the item query or player name the user deliberately enters. Version 0.4 does not request screen-capture or Accessibility Service permission and contains no OCR implementation.

The app also reads Jagex's public world list and hiscores, the OSRS Wiki public prices API, and opens reference pages in the user's browser. It never signs into a Jagex account or submits input to the OSRS client.

## References

- [Jagex third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)
- [Jagex rules](https://legal.jagex.com/docs/rules)
- [Android application overlay permission](https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW)
- [Star Miners live map](https://map.starminers.site/)
- [Official OSRS world list](https://oldschool.runescape.com/slu?order=wlmAp)
- [OSRS Wiki prices API](https://prices.runescape.wiki/)
- [Official OSRS hiscores](https://secure.runescape.com/m=hiscore_oldschool/overall)

Rules and platform policies can change. This file records the project's engineering boundary; it is not legal advice or a guarantee that Jagex will approve a particular feature.
