# Prompt for Gemini — Kodomo UI redesign (code + art)

You are a senior Android engineer, product designer and 3D illustrator. Attached is the complete source of
**Kodomo** (کدومو), an Iranian kids' launcher (Kotlin, Jetpack Compose Material3, Hilt, Room), plus five approved
design boards in `docs/design/handoff_v5/reference/` (00_overview, 01_onboarding, 02_kid_home, 03_kid_inner,
04_parent). Your job is to make the app look like those boards on every screen — as real, interactive Compose UI,
not pictures of screens.

## Read first
1. The five boards (style master). Then `docs/design/handoff_v5/README_FA.md`, `asset_manifest.csv`,
   `GEMINI_ART_PROMPT.md`, and `docs/design/handoff_v4/` (screen specs, tokens, QA checklist).
2. Code map:
   - Design system: `android/app/src/main/java/com/kodomo/app/ui/design/` (KodomoDesign.kt tokens & AgeBand/KidStyle,
     KidScene.kt backgrounds, KidCharacter.kt drawn characters, KidComponents.kt Premium3DIcon/ChildTile/ChildDock/
     MascotBubble/RewardBadge/ProgressRing, ArtAssets.kt which loads `assets/art/<folder>/<name>.png`).
   - Theme: `ui/theme/` (Vazirmatn, light/dark/night). Kid screens: `ui/kids/**`. Parent screens: `ui/parent/**`.
     Onboarding/setup: `ui/onboarding`, `ui/setup`, `ui/profiles`. Navigation: `ui/navigation/KodomoNavHost.kt`.

## Part A — art (most important)
The look of the boards comes from rendered 3D art. Produce the images listed in `asset_manifest.csv` exactly as
described in `GEMINI_ART_PROMPT.md` (Iranian/Islamic: every girl and woman in a hijab fully covering hair, modest
clothes, no text inside images, same characters everywhere, transparent PNG or flat #00FF00 / #FF00FF background).
Put them in `android/app/src/main/assets/art/<path>`.

## Part B — code (UI layer only)
- Wire every art folder into the screens through `Art(name, size) { fallback }` (keep the drawn fallback):
  robot → mascot spots, buddies → companion, scenes → `KidScene` layers per KidStyle, heroes → header of each kid
  category screen, onboarding → onboarding steps, states → bedtime / time-up / school / empty / no-internet screens,
  games/quests/play → game hub, level map and game HUD, parent → parent dashboard/report/safety headers.
- Rebuild screen layouts to match the boards: glass cards, 3D tiles, bottom dock, big rounded radii (16–28dp),
  spacing scale 8/12/16/24/32/48, Vazirmatn, RTL, Persian digits, child touch targets ≥ 64dp, parent ≥ 48dp.
- Four age bands (under 5, 5–7, 7–10, 10–15) and girl/boy *appearance* themes; gender never limits content.
- Light, dark and night (20:00–06:00) themes must all stay readable.
- Parent area stays calm and white like board 04.

## Do NOT change (security / behaviour)
`kiosk/**` (launcher, AppGuardService, DeviceOwnerManager, InternetBlockVpn, BootCompletedReceiver),
`core/security/**`, `ui/parent/ParentGateDialog*`, `data/**`, `domain/**`, `AndroidManifest.xml`, Gradle files,
`.github/**`, `ui/kids/video/LockedWebPlayer.kt` lock rules, `tools/smoke_test.sh`. Do not remove features,
screens, routes, ViewModels or texts; do not add dependencies, network calls, analytics or ads.

## Output
- A ZIP with only the files you changed or added, at their original paths (images under `assets/art/`).
- A `CHANGES.md` listing each file and what changed, and which screens still differ from the boards.
- Code must compile with the existing Gradle setup (Compose BOM already present). If unsure about an API, keep the
  simpler version.
