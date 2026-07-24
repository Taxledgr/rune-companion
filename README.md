# Rune Companion

Rune Companion is a passive Android companion for Old School RuneScape mobile. It combines community-scouted Shooting Stars with local timers, manual gameplay journals, public prices and hiscores, calculators, and quick-reference links.

> [!IMPORTANT]
> Rune Companion is an independent community project. It is not affiliated with, endorsed by, or sponsored by Jagex, RuneScape, Old School RuneScape, RuneLite, or Star Miners.

## Version 0.4

- Live Shooting Star worlds, tiers, locations, callers, arrival windows, and estimated depletion windows
- Tier, members/F2P, official server-region, world, and location filters
- Saved world, tier, and favourite-location alerts with quiet hours
- Configurable alert sounds through Android notification settings
- Reliable background checks through Android WorkManager
- A draggable Android overlay with full and compact modes
- One-minute refresh interval and manual refresh
- Direct link to the official Star Miners live map
- Farming, birdhouse, daily, and custom reminders
- Manual boss/raid stopwatch and Slayer kill counter
- Local quest, diary, collection-log goal, and gear/inventory loadout checklists
- Grand Exchange item watchlist using the OSRS Wiki public prices API
- Player lookup using Jagex's public OSRS hiscores
- Saved player profile with automatic hiscore updates, local progress baseline, and per-skill XP gains
- OSRS XP, cumulative drop-rate, and supply-cost calculators
- Clue, fairy-ring, teleport, quest, diary, and boss reference links
- No account login, ad SDK, analytics, or collection of personal information

## Safety boundary

Rune Companion is intentionally a passive companion:

- It does not read or modify the OSRS process.
- It does not inspect game memory or network traffic.
- It does not inject code into the game client.
- It does not automate taps, gestures, or gameplay.
- It does not use Accessibility Services.
- It does not capture the screen or use OCR.

The overlay uses Android's standard `TYPE_APPLICATION_OVERLAY` window after the user grants the system "display over other apps" permission. See [docs/COMPLIANCE.md](docs/COMPLIANCE.md) for the design rules.

## Star Miners data

The app makes a read-only request no more than once per minute to the public feed used by [map.starminers.site](https://map.starminers.site/). Star Miners owns and operates that service. Its feed is public but undocumented, so it may change or become unavailable without notice.

Rune Companion:

- identifies itself with a descriptive user agent;
- caches the latest successful response in memory;
- does not submit or alter Star Miners data;
- attributes Star Miners in the app; and
- provides a button to open the official map as a fallback.

Please use the Star Miners service respectfully. If its maintainers publish an official API or usage policy, this project should migrate to it.

## Alert timing

The app and active overlay refresh through a shared, rate-limited feed cache once per minute. When both are closed, Android WorkManager schedules a network-constrained check at intervals of at least 15 minutes. Android may delay background work for battery optimisation, so background alerts are useful but not guaranteed to be immediate.

Star depletion text is an estimate based on the reported tier and seven minutes per continuously mined layer. Players pausing, mining progress before the report, and stale community reports can all make the real time differ.

## Other public data

- Members/F2P and physical server-region labels come from the [official OSRS world list](https://oldschool.runescape.com/slu?order=wlmAp).
- Grand Exchange prices come from the [OSRS Wiki real-time prices API](https://prices.runescape.wiki/).
- Player levels, ranks, and XP come from the [official OSRS hiscores](https://secure.runescape.com/m=hiscore_oldschool/overall).

All timers, journal entries, loadouts, goals, and watchlists are stored locally on the phone.

## Automatic player stats

Enter an OSRS display name under **Settings → Tracked player**. Rune Companion immediately loads public skill levels, ranks, and XP, then:

- refreshes at most every 10 minutes while the app is open;
- asks Android WorkManager to refresh every 15 minutes when the app is closed;
- stores the latest and baseline snapshots locally; and
- shows total and per-skill XP gained since the baseline.

Android may delay background work because of battery optimisation or network conditions, and the official hiscores can lag behind recent in-game XP. No Jagex login, password, or game-client access is used.

## Build

Requirements:

- Android Studio with Android SDK 35
- JDK 17 or newer

Open the repository in Android Studio, let Gradle sync, and run the `app` configuration on an Android 9.0 (API 28) or newer device.

From a terminal:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

If a cloud-sync client locks Gradle's generated files, use an external build directory:

```powershell
.\gradlew.bat -PruneCompanionBuildDir="$env:TEMP\rune-companion-build" testDebugUnitTest lintDebug assembleDebug
```

## Install on an Android phone

### With Android Studio or ADB

1. On the phone, enable Developer options by tapping **Build number** seven times.
2. Enable **USB debugging** and connect the phone by USB.
3. Approve the computer prompt on the phone.
4. Run:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

### Without ADB

Copy the versioned APK to the phone, open it from the Files app, and allow **Install unknown apps** for that Files app when Android asks. Leave Play Protect enabled. After installation, grant notifications and **Display over other apps** only if you want alerts and the floating panel.

## Permissions

| Permission | Purpose |
| --- | --- |
| Internet | Fetch the read-only Shooting Stars feed |
| Display over other apps | Show the user-controlled floating panel |
| Foreground service | Keep an enabled overlay alive while the app is backgrounded |
| Notifications | Display star matches, gameplay timers, and Android's required foreground-service notification |

## Roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md) for the prioritised, rules-conscious feature list.

## Licence

[MIT](LICENSE)
