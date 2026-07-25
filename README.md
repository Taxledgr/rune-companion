# Rune Companion

Rune Companion is a passive Android companion for Old School RuneScape mobile. It combines community-scouted Shooting Stars with local timers, manual gameplay journals, public prices and hiscores, calculators, and quick-reference links.

> [!IMPORTANT]
> Rune Companion is an independent community project. It is not affiliated with, endorsed by, or sponsored by Jagex, RuneScape, Old School RuneScape, RuneLite, or Star Miners.

## Version 1.8

- **Activity Profiles** save complete reusable setups for Shooting Stars,
  skilling, questing, Slayer, bossing, or a custom activity.
- Every profile keeps its own start screen, navigation tabs, pinned helpers,
  selected OSRS character, Shooting Star safety/location filters, and floating
  overlay configuration.
- Five useful profiles are created automatically without replacing the setup
  from an earlier Rune Companion installation.
- Profiles can be switched, copied, renamed, and deleted under
  **More → Customize & settings → Customize experience**.
- Long-press the minimised floating bubble—or tap **◆** in the compact
  panel—to switch the complete setup without leaving OSRS.
- Overlay sections can now be reordered, and each profile remembers its panel
  size, opacity, selected section, and separate portrait/landscape panel and
  bubble positions.
- Activity Profiles are included in encrypted export/import, Android cloud
  backup, phone-to-phone transfer, and in-place app upgrades.
- Version 1.8 uses Android version code 21 and updates earlier versions in place
  without deleting saved accounts, settings, filters, routes, or journals.

## Version 1.7

- The compact overlay now has an **Edit** action for every configured section.
- Editing expands into a focusable 92% × 88% floating workspace while OSRS
  remains visible and running behind it.
- Each of the 27 overlay sections opens its matching existing Rune Companion
  editor, including Stars, timers, Slayer, journals, accounts, goals,
  teleports, economy tools, supplies, and session logs.
- Text fields use the Android keyboard normally; catalogue selectors, Add, Save,
  and other existing form controls work without leaving the overlay.
- **Done** returns to the compact panel at its previous position, and saved data
  is immediately reflected in that panel.
- Version 1.7 uses Android version code 20 and updates earlier versions in place
  without deleting saved profiles, settings, filters, routes, or journals.

## Version 1.6

- Personalize the app around Shooting Stars, skilling and Mining, questing,
  Slayer, bossing, or a fully custom setup.
- Choose which screen—or which pinned gameplay helper—opens when Rune Companion
  starts.
- Show, hide, and reorder the bottom navigation while keeping a safe route back
  to **More → App settings**.
- Pin up to six of the 30 gameplay helpers to a Quick access section with useful
  saved-data summaries.
- Focus presets configure a sensible start screen, navigation order, and pinned
  tools in one tap; every individual choice remains editable.
- Existing installations default to Stars and retain all saved accounts,
  filters, timers, journals, routes, and overlay settings.
- Personalization is stored locally, included in encrypted backups, and restored
  immediately after import.
- Version 1.6 uses Android version code 19 and updates earlier versions in place.

## Version 1.5.1

- Network refreshes now run only while their screens are useful, share cached
  item data, ignore duplicate requests, and retain the last successful star feed
  during temporary connection failures.
- Price search waits briefly for typing to finish and discards outdated results.
- Tabs preserve their scroll position and in-progress form text when switching.
- Shooting Star controls are condensed into a quick status card so filters and
  live results appear much sooner; timer and journal builders stay collapsed
  until requested.
- Backup imports reload saved accounts, timers, filters, and overlay settings
  immediately without restarting the app.
- The floating bubble opens on the right side by default instead of covering app
  headings, and the overlay skips Star Miners requests when neither Stars nor
  star alerts are enabled.
- Version 1.5.1 uses Android version code 18 and updates earlier versions in
  place without deleting saved profiles, settings, or journals.

## Version 1.5

- The floating window is now a configurable Rune Companion dashboard instead
  of a Shooting Stars-only panel.
- Enable any combination of 27 live, progress, planning, reference, economy,
  and history sections, then use **‹** and **›** in the panel to switch between
  them without returning to the app.
- Available overlay sections include stars, timers, Slayer, trip timer,
  checklist, player stats, goals, quests and diaries, farming, routines, itineraries, loadouts,
  teleports, GE tools, loot, supplies, Wilderness risk, and activity sessions.
- The compact bubble displays the current section's symbol and count. Its
  enabled sections and current selection persist across app restarts, in-place
  updates, encrypted backups, and phone transfers.
- Shooting Stars retain their expandable fastest routes, exact directions,
  shortcuts, requirements, Wilderness warnings, and landing-site maps.
- Version 1.5 uses Android version code 17 and updates earlier versions in place
  without deleting saved profiles or journals.

## Version 1.3

- A first-class **Wiki** tab with full in-app OSRS Wiki search and article reading;
  existing Wiki buttons now open inside Rune Companion instead of switching apps
- Boss readiness plans combining public skill checks with manual quest, gear,
  supply, mechanics, route, and loadout checks
- Smart gameplay itineraries built from saved farming patches, routines, or
  selectable travel presets, with safe regional ordering
- GE-priced gear upgrade plans, itemised loot ledger, supply/charge locker, and
  Wilderness risk estimates
- Automatic public boss, raid, clue, and activity counter goals
- Quest and Achievement Diary navigation with public skill readiness and manual
  completion state
- OSRS Wiki real-time GE history charts with price range, spread, and volume
- Searchable monster/drop quick reference with full current Wiki articles in app
- All ten expansion tools persist across in-place updates and encrypted backup
- Version 1.3 uses Android version code 14 and updates version 1.2 in place

See [the complete 1.3 release notes](docs/RELEASE_NOTES_1.3.md).

- Live Shooting Star worlds, tiers, locations, callers, arrival windows, and estimated depletion windows
- Expandable fastest-route guidance on every one of the 82 star cards, ranked
  against the selected player's configured teleports and public Agility/Magic levels
- Alternative teleports, walking directions, shortcut requirements, route maps,
  and prominent Wilderness warnings on each star card
- Tier, members/F2P, official server-region, world, and location filters
- Tick/untick checklist covering all 82 Star Miners landing sites in 15 areas
- Safe-world mode that removes PvP, Bounty Hunter, High Risk, Wilderness PK, Deadman, and similarly labelled worlds
- Saved world, tier, and favourite-location alerts with quiet hours
- Configurable alert sounds through Android notification settings
- Reliable background checks through Android WorkManager
- A draggable Android overlay with compact, bubble, and near-full editing modes
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
- Multiple public-hiscore profiles with timestamped daily and weekly XP history
- Automatic named public-hiscore counters for Sailing, clues, minigames, raids,
  and current bosses, with daily and weekly gains
- Skill-goal and banked-XP planners
- GE target notifications, portfolio cost basis, 1% tax, and net-profit tracking
- Farming patch dashboard, Slayer knowledge cards, and boss/raid session logs
- Searchable category tabs and auto-filled presets for 151 Slayer assignments,
  25+ detailed Slayer cards, farming patches/crops, banked XP, collection
  targets, activities, routines, Combat Achievement tiers, and loadouts
- Collection dry-streak probabilities and Combat Achievement planning
- Searchable clue helper and points-based minigame calculator
- Player-specific teleport route planner covering spellbooks, tablets, jewellery,
  special items, quest unlocks, house location, POH facilities, and Portal Nexus destinations
- Manual DPS, consumable, daily/weekly routine, and shareable loadout tools
- Android home-screen widget for the selected public-hiscore profile
- Passphrase-protected AES-256-GCM backup and restore
- Update-safe saved data with schema migration, last-known-good recovery, encrypted
  Android cloud backup, and device-to-device transfer
- No account login, ad SDK, analytics, or collection of personal information

The persistent area and safe-world filters apply consistently to the main Stars list, floating overlay, and matching-star notifications.

## In-app OSRS Wiki

The **Wiki** bottom tab searches and displays the complete current
[Old School RuneScape Wiki](https://oldschool.runescape.wiki/) inside Rune
Companion. Wiki links from Shooting Star routes, clues, bosses, monsters,
quests, diaries, death mechanics, and quick references use the same reader.

Articles are loaded directly from the Wiki rather than copied into the APK.
This keeps information current, avoids an enormous stale offline snapshot, and
preserves the Wiki's authorship, attribution, links, history, and
CC BY-NC-SA 3.0 notices. External non-Wiki links still ask Android to open the
appropriate app.

## Shooting Star routes

Every live star card includes a route summary. Tap **All routes & shortcuts** to see:

- the fastest route enabled by **More → Teleport route planner → My teleports**;
- every other known teleport route for that landing site;
- the walk from the teleport destination to the crashed star;
- Agility shortcut levels taken from the selected account's public hiscores;
- quest, diary, equipment, coin, and transport requirements that still need a
  manual check; and
- a prominent warning on every Wilderness route.

The 82-site route keys match the Star Miners feed. Route ordering is based on the
current [OSRS Wiki Shooting Stars landing-site tables](https://oldschool.runescape.wiki/w/Shooting_Stars#Landing_sites).
The app does not read the player's live position, inventory, run energy, or game
state, so “fastest available” means the highest-ranked route whose teleport and
public skill requirements match the profile configured in Rune Companion.

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

All timers, journal entries, loadouts, goals, teleport selections, and watchlists are stored locally on the phone.

## Teleport planner

Open **More → Teleport route planner → My teleports** and select what that player
can actually use:

- available spellbooks and quest/diary unlocks;
- teleport tablets currently carried or banked;
- teleport jewellery and special teleport items;
- player-owned house access and house portal location;
- mounted jewellery, fairy ring, spirit tree, obelisk, and other POH facilities; and
- each configured portal chamber or Portal Nexus destination.
- custom or newly released teleport items and their destinations.

The selected account's public hiscores provide its Magic level. Jagex's hiscores
do not expose inventory, bank, active spellbook, quests, diaries, item charges, or
POH configuration, so those capabilities cannot be detected automatically without
inspecting the game client. Rune Companion intentionally requires the player to
configure them and never uses screen capture, OCR, Accessibility Services, or client hooks.

## Automatic player stats

Enter an OSRS display name under **More → App settings → Tracked player**. Rune Companion immediately loads public skill levels, ranks, and XP, then:

- refreshes at most every 10 minutes while the app is open;
- asks Android WorkManager to refresh every 15 minutes when the app is closed;
- stores the latest and baseline snapshots locally; and
- shows total and per-skill XP gained since the baseline; and
- records named public boss, raid, clue, minigame, collection-log, and Sailing
  counters for automatic 24-hour and 7-day progress.

Android may delay background work because of battery optimisation or network conditions, and the official hiscores can lag behind recent in-game XP. No Jagex login, password, or game-client access is used.

For several characters, use **More → Multi-account profiles**. These profiles
refresh every 10 minutes while Rune Companion is open (and through Android's
15-minute background scheduler), retaining recent snapshots plus compact hourly
history for the daily and weekly XP view.

## Automatic data and selectable presets

Rune Companion automates data that is available without inspecting the game:

- public skills, XP, boss KCs, raid completions, clues, minigames, Sailing, and
  collection-log count from Jagex hiscores;
- item names and live prices from the OSRS Wiki price API;
- worlds from Jagex and Shooting Stars from Star Miners; and
- growth times, Slayer references, drop-rate defaults, routines, and loadout
  starting points from the app's selectable catalogues.

Choose **Preset** in a supported tool, select a category tab, and search or tap
an entry. The fields are filled automatically and remain editable before saving.
Choose **Custom** for anything not in the catalogue.

Inventory, bank contents, equipped items, current Slayer assignment, quest and
diary state, POH unlocks, current position, loot, and supply use are not exposed
by public hiscores. Those remain explicit selections or local counters so Rune
Companion can stay passive and avoid screen capture, OCR, Accessibility Services,
or game-client hooks.

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

For later updates, use the fail-safe update script:

```powershell
.\scripts\install-update.ps1 -Device "PHONE_IP:WIRELESS_DEBUGGING_PORT" `
  -ApkPath ".\app\build\outputs\apk\debug\app-debug.apk"
```

The script only performs an in-place Android update. It never uninstalls Rune
Companion or clears storage. If Android reports an incompatible signature or a
lower version code, build with the same signing key and a higher version instead
of uninstalling—the uninstall would erase private app data.

Since Rune Companion 1.1.1, the app also keeps a last-known-good copy of its profile/toolkit
documents, migrates the original tracked-player profile into the multi-account
list, and opts its preferences into encrypted Android backup and phone-to-phone
transfer. Android cloud restore depends on backup being enabled for the phone's
Google account. The manual encrypted backup under **More → Encrypted backup**
remains the most portable backup.

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
