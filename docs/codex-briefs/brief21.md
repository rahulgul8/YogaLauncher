# Brief 21: Block WhatsApp Status, keep WhatsApp (piece K)

Repo: Yoga Phone launcher (Kotlin, Jetpack Compose). Work in this checkout (`YogaLauncher`, branch `main`); nobody else edits it while you work. Don't commit; Claude reviews, builds, tests on the phone and commits. You can't run Gradle, so write code that compiles first time: check every import, signature and every exhaustive `when` over `GuardSurface` and `IntroId`.

## Goal

The founder wants WhatsApp Status treated like Shorts and Reels: WhatsApp (chats, calls, groups) keeps working, and only **watching Status updates** is blocked, with the same way through (Watch anyway → resist screen → pass) and strict mode. The person turns it on in Settings, in onboarding's blocks step, and through a feature introduction, exactly like Shorts and Reels.

Read first, and follow the existing Shorts/Reels patterns everywhere rather than inventing new ones:
- `guard/GuardModels.kt` (`GuardSurface`, `family()`, `shortName()`, `isLooser`), `guard/ShortsDetector.kt`, `guard/YogaGuardService.kt`, `guard/GuardPrefs.kt`, `guard/GuardBlockHistory.kt`, `guard/BlockCardIntroduction.kt`;
- `ui/GuardSettingsScreen.kt`, `ui/ResistScreen.kt`, `ResistActivity.kt`, `ui/GuardDisclosureDialog.kt`;
- `intro/IntroModels.kt`, `intro/IntroEngine.kt`, `intro/IntroSignalsReader.kt`, `intro/IntroActions.kt`, `ui/FeatureIntroductions.kt`, `ui/SuggestionsScreen.kt`;
- `ui/OnboardingScreen.kt` (the Shorts & Reels "BlocksStep") and `ui/OnboardingSteps.kt`;
- `review/ReviewController.kt` (R1 fires after the Shorts/Reels introduction; include Status);
- tests under `app/src/test/java/in/tuwi/yogaphone/guard/` and `.../intro/`.

## 1. Surface and detection

- Add `GuardSurface.WHATSAPP_STATUS` **at the end** of the enum (the 7-day history encodes by ordinal; appending keeps old data valid). Its family is itself only (fix `family()`: today every non-YouTube surface falls into the Instagram family). `shortName()` → "Status".
- Packages: `com.whatsapp` and `com.whatsapp.w4b` (WhatsApp Business).
- Detection, in `ShortsDetector` (keep one detector):
  1. **Activity class (primary):** the Status viewer is its own activity. Add `fun detectByClass(packageName: String, className: String?, enabled: Set<GuardSurface>): GuardSurface?` returning WHATSAPP_STATUS when the package is WhatsApp and the class name contains `StatusPlayback` (e.g. `com.whatsapp.status.playback.StatusPlaybackActivity`). In `YogaGuardService.onAccessibilityEvent`, remember the className of the latest `TYPE_WINDOW_STATE_CHANGED` for the foreground package (only activity classes: ignore `android.widget.*`/`android.view.*` dialog and toast classes, as the service already does for its own overlays) and use it in `update()` before the node walk.
  2. **View ids (fallback):** a set `whatsappStatusIds` checked like `youtubeIds`. Start with `status_playback_container`, `status_playback_page`, `playback_container` scoped to the WhatsApp packages; Claude will confirm the real ids on the phone with a UI dump and adjust the set, so keep it in one place.
  - **Never** match the Updates tab list, chats, calls, or the camera; only the full-screen Status viewer.
- `supportedPackages` includes the WhatsApp packages when WHATSAPP_STATUS is enabled.
- The block action stays Back (Home after repeated tries), which returns to the Updates list. The block card says "Status is off · Chats and calls still work · Blocked N times today" and offers "Watch anyway" unless strict, like the others. The resist screen eyebrow is "Status", title "Watch anyway?". The pass pill reads "Status · N min left".
- Tests: class-name detection (positive for both packages, negative for `HomeActivity`, `Conversation`, camera), id fallback, family/shortName, and that adding the enum entry keeps `guardBlockHistory` decoding old 4-surface rows.

## 2. Settings and disclosure

- `GuardSettingsScreen`: a row "WhatsApp Status" with the same switch, Block / Allow daily chips and minute choices as the others, under the Shorts and Reels list. Rename the section to "Shorts, Reels and Status" and the screen title to "Shorts, Status and live time value"; update the Settings row summary ("Shorts, Reels, Status, live pill").
- Turning it off or loosening goes through the existing `isLooser` / `LoosenSheet` / strict rules unchanged (they're generic; just check they cover the new surface).
- `GuardDisclosureDialog`: the text must stay accurate, because Google reviews it. Rewrite it to name WhatsApp too, and to cover learning videos, which was merged yesterday (it reads the YouTube channel name and video title in memory for channels the person adds). Keep the same structure and calm tone; keep "Screen content is never stored, logged or sent anywhere"; list what is saved. Update the comment above it to say the declaration and video must be updated before the next open-testing review.
- `docs/launch/privacy-policy.md` and `docs/launch/play-listing.md` (Accessibility declaration text): add WhatsApp Status in the same places Shorts/Reels are described.

## 3. Introduction (like I1/I2)

- `IntroId.STATUS_BLOCK`. Signals: `whatsappInstalled` and `whatsappMinutes7d` from the same source as YouTube/Instagram minutes. Rule: WhatsApp installed, at least 3 h a week in WhatsApp, Status not already blocked, guard available. Copy, following the existing I1/I2 style:
  - title: "${duration} on WhatsApp this week" (use the existing duration formatter);
  - body: "Chats and calls still work.";
  - action: **Turn off Status**.
- The action path is the same as Shorts/Reels in `IntroActions` (disclosure if the guard is off, Android settings, confirmation "Status is off. Chats and calls still work." with Undo), and it emits the R1 review moment like the others.
- Surfaces: the Usage screen card, home line and Suggestions list, exactly as I1/I2. The block card's "Also turn off …?" sibling line: add Status as a sibling only between WhatsApp and nothing else (don't offer Status on a Shorts card); if that's awkward in `BlockCardIntroduction`, skip the sibling line for Status and say so.
- Debug preview: extend `IntroSignalsReader.setPreview` with `whatsapp=<minutes>` like `instagram=300`.
- Tests in `IntroEngineTest`: shows at ≥ 3 h, not below, not when already blocked, not when WhatsApp isn't installed.

## 4. Onboarding

- In the Shorts & Reels step, when WhatsApp is installed, add a third switch "WhatsApp Status" ("Chats and calls still work"), with the real weekly WhatsApp minutes if the step shows them for the others. The step title may stay; if it names Shorts and Reels explicitly, make it "Shorts, Reels and Status" only when WhatsApp is installed. Don't add a new image.

## 5. Docs

- `docs/adr/0015-bypass-with-resistance.md`: one paragraph noting Status as a third family.
- OpenSpec: add a requirement + scenarios for Status to `openspec/changes/add-bypass-with-resistance/specs/` (or the guard spec delta there), and tasks.

## Constraints

- No new dependencies. Don't touch `TODO.md`, `review/*` beyond the R1 hook, or the learning-video logic.
- Calm copy, no exclamation marks, never scolding. Kit components only.

## Done when

- It compiles. Claude runs `./gradlew :app:testDebugUnitTest :app:assembleDebug`, then checks it on the moto g31.
- Your summary lists files changed, the detection approach, and anything you couldn't do.
