# Rune Companion 1.10

## 1.10.6 persistent close-to-bubble behavior

- The panel's **×** control now closes the panel back to the bubble.
- Explicit Stop controls remain unchanged, but an ordinary close can no longer
  terminate the overlay service unexpectedly.
- Version `1.10.6` uses Android version code `32`.

## 1.10.5 bubble-first overlay

- Every new overlay session starts as the compact floating bubble.
- Minimising the panel immediately resizes and moves the overlay window to the
  bubble bounds, avoiding an invisible full-panel touch area.
- Version `1.10.5` uses Android version code `31`.

## 1.10.4 in-overlay quest Wiki

- Wiki links opened from a quest guide in the enlarged floating editor stay in
  the overlay instead of handing the player to a separate browser.
- **Guide** returns to the saved quest walkthrough and **Done** closes the
  overlay.
- The reader keeps the existing strict OSRS Wiki host allowlist and private
  WebView configuration.
- Version `1.10.4` uses Android version code `30`.

## 1.10.3 portrait navigation clearance

- The compact 68dp app navigation now sits above the runtime-reported Android
  navigation-button inset.
- The same inset handling also protects the right edge when the phone uses
  three-button navigation in landscape.
- Version `1.10.3` uses Android version code `29`.

## 1.10.2 system-bar spacing

- The profile/search header respects the Android status-bar inset.
- The app navigation bar is reduced to 68dp.
- Nested Stars content no longer reserves Android system-bar space a second
  time, removing the unused strip above the bottom navigation.
- Version `1.10.2` uses Android version code `28`.

## 1.10.1 overlay quest navigation

- Tapping a quest in the compact Quests & Diaries overlay now expands the
  floating editor directly into that quest's step-by-step guide.
- The compact row clearly says that it opens the guide, and remains accessible
  as a minimum 48dp touch target.
- Version `1.10.1` uses Android version code `27`.

Rune Companion 1.10 is a workflow and usability refinement release.

## Quest companion

- Selecting a tracked quest opens a dedicated preparation and walkthrough
  screen instead of a requirements-only card.
- Inventory, equipment, teleports, and ordered route milestones are selectable
  and saved locally.
- Guide progress can be reset per quest.
- The live OSRS Wiki quick guide opens in Rune Companion's allowlisted private
  reader for current exact details.

## Shooting Stars

- The currently expanded Star card survives navigation and app restarts while
  that report remains available.
- A player can save a preferred available route per landing site.
- Available and unavailable alternatives are labelled clearly using the active
  account and configured teleport capability profile.
- The existing exact landing-site preview, walking directions, requirements,
  shortcuts, world safety, tier, scout, and report age remain together.

## Overlay and customization

- Separate portrait and landscape panel widths.
- Adjustable overlay text scale and opacity.
- Optional edge snapping.
- Optional landscape centre-safe bounds for the usual OSRS side controls.
- One-tap position reset with independent orientation placement retained.
- Fresh installs can choose a primary activity before entering the app.

## Quality and privacy

- Ranked, typo-tolerant global search with private recent-search chips.
- Safe local public-Hiscores history compaction in Diagnostics.
- New preferences are included in encrypted exports and encrypted/device-
  transfer Android backup rules.
- No OCR, screen capture, Accessibility Service, Jagex login, gameplay input,
  analytics, or advertising was added.
- Unit tests, Android lint, APK identity checking, in-place data backup, and
  post-install preference verification pass for version `1.10.0` (code `26`).
