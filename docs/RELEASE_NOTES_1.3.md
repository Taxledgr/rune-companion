# Rune Companion 1.3.0

Rune Companion 1.3 adds ten connected planning and tracking tools plus a
first-class in-app OSRS Wiki reader.

## New companion tools

1. Boss readiness checker with public skill checks and manual readiness lists.
2. Smart gameplay itinerary built from farming, routines, and travel presets.
3. GE-priced gear upgrade planner.
4. Itemised loot ledger with current public item values.
5. Automatic public boss, raid, clue, and activity counter goals.
6. Quest and Achievement Diary navigator with public skill readiness.
7. Charges and supplies locker with low-stock thresholds.
8. GE market-history charts, spread, range, and volume.
9. Wilderness carried/protected/at-risk value planner.
10. Monster and drop explorer with quick references and full Wiki pages.

## In-app Wiki

The new Wiki bottom tab searches and reads the complete current OSRS Wiki
without switching apps. Existing Wiki links throughout Rune Companion use the
same reader. Articles remain hosted by the OSRS Wiki so attribution, revision
history, licences, images, links, and updates remain intact.

## Data and update safety

- Android version code 14 updates version 1.2 in place.
- Version 1.3 data is an additive schema update; existing version 1.2 records
  are preserved and new collections default to empty.
- All ten feature collections are included in the existing encrypted backup
  because they are stored in the companion feature document.
- Installation must use `adb install -r` or the repository update script.
  Do not uninstall the existing app, as Android uninstall removes private data.

## Verification

- 38 local unit tests.
- Android lint passes with no errors.
- Debug APK assembles for API 28+ and targets API 35.
