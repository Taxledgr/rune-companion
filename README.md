# Rune Companion

Rune Companion is an Android companion overlay for Old School RuneScape mobile. The first module shows community-scouted Shooting Stars from the OSRS Star Miners live map.

> [!IMPORTANT]
> Rune Companion is an independent community project. It is not affiliated with, endorsed by, or sponsored by Jagex, RuneScape, Old School RuneScape, RuneLite, or Star Miners.

## Version 0.2

- Live Shooting Star worlds, tiers, locations, callers, and report ages
- Search by world, location, or caller
- Tier filters
- Saved preferred worlds and multi-tier alert filters
- Matching-star notifications with duplicate suppression
- Reliable background checks through Android WorkManager
- A compact, draggable Android overlay
- One-minute refresh interval and manual refresh
- Direct link to the official Star Miners live map
- No account login and no collection of personal information

## Safety boundary

Rune Companion is intentionally a passive companion:

- It does not read or modify the OSRS process.
- It does not inspect game memory or network traffic.
- It does not inject code into the game client.
- It does not automate taps, gestures, or gameplay.
- It does not use Accessibility Services.
- It does not capture the screen or use OCR in version 0.2.

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

## Permissions

| Permission | Purpose |
| --- | --- |
| Internet | Fetch the read-only Shooting Stars feed |
| Display over other apps | Show the user-controlled floating panel |
| Foreground service | Keep an enabled overlay alive while the app is backgrounded |
| Notifications | Display matching-star alerts and Android's required foreground-service notification |

## Roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md) for the prioritised, rules-conscious feature list.

## Licence

[MIT](LICENSE)
