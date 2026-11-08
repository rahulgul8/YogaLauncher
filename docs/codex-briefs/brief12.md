# Brief 12: In-app updates and What's new (piece A)

You are working in a git worktree of the Yoga Phone Android app on branch `in-app-updates`. The app is Kotlin and Jetpack Compose; the code namespace is `in.tuwi.yogaphone` and the app id is `com.tuwi.yogaphone`. Claude reviews, builds and commits.

**Constraints on your sandbox:**
- It can't run Gradle or fetch packages.
- `com.google.android.play:app-update-ktx:2.1.0` is **already added** (`libs.play.app.update.ktx`) and resolves. Don't add any other dependency.

## Read first

- `openspec/changes/add-in-app-updates/`: the proposal, tasks and spec. **This is the approved scope.**
- `docs/plans/feature-discovery.md` section 3 (context only; don't edit the plan).
- `MainActivity.kt`:
  - `onResume` / `onStart` / `onStop` (around line 200);
  - the home `statusContent` column (around line 455), where `DefaultHomePrompt` and other home lines live.
- `ui/HomePrompts.kt`: `DefaultHomePrompt` is the visual pattern for the update line (accent dot, muted text, text button).
- `ui/PrivacyScreens.kt` `HelpScreen`: add the "Check for updates" row here.
- `ui/theme/YogaComponents.kt`, `YogaIcons.kt`, `docs/design/brand.md`. Use the Yoga kit only, and never hard-code colours or fonts.
- `scripts/play-upload.mjs` (Node, no dependencies) and `docs/launch/release.md`.
- `docs/play/release-notes.md`.
- ADR format: `docs/adr/0010-yoga-pro.md`. **Your ADR is 0013.**

## Build

1. **`update/UpdatePolicy.kt`**, a pure object with no Android types.
   - Input: `UpdateFacts`, containing:
     - `available: Boolean`, `inProgress: Boolean`;
     - `priority: Int` (0–5), `stalenessDays: Int?`;
     - `status`: an enum NONE, PENDING, DOWNLOADING, DOWNLOADED, INSTALLING, FAILED;
     - `downloadedAtMillis: Long?`, `lastCheckMillis: Long?`, `nowMillis: Long`;
     - `foreground: Boolean`.
   - Output: an `UpdateAction`, one of:
     - `None`;
     - `ShowUpdateLine` (priority 0–3, available, not downloading);
     - `ShowDownloadingLine` (quiet "Downloading update…");
     - `StartFlexibleDialog` (priority 4);
     - `StartImmediate` (priority 5, or an immediate update already in progress);
     - `CompleteNow` (DOWNLOADED and not foreground);
     - `ShowRestartLine` (DOWNLOADED, foreground, downloaded ≥ 24 h ago).
   - `shouldCheck(lastCheck, now)`: true when there was no check or it was ≥ 24 h ago.
   - Unit-test every branch, including the 24 h boundaries.
2. **`update/UpdateStore.kt`**, in DataStore (`data/dataStore`). It stores:
   - the last check time;
   - when the download finished;
   - whether the person dismissed the update line for this version code (it comes back after 3 days);
   - the last-run version code, used for What's new.
3. **`update/UpdateController.kt`**, a thin layer over `AppUpdateManagerFactory.create(context)`:
   - **`onHomeResume(activity)`:**
     - if `shouldCheck` is true, or the store says a download is pending or downloaded, call `appUpdateInfo`;
     - apply `UpdatePolicy`;
     - expose a `StateFlow<UpdateUi>` for the home line.
   - **Flexible flow:**
     - `startUpdateFlowForResult` with `AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE)`, through an `ActivityResultLauncher<IntentSenderRequest>` registered in `MainActivity`;
     - register an `InstallStateUpdatedListener` that records DOWNLOADED and the time.
   - **Immediate flow:** for priority 5, and to resume `DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS`.
   - **`onHomeStop()`:**
     - if the status is DOWNLOADED, call `completeUpdate()`. Android installs silently while Yoga is in the background, and the home screen comes back by itself.
     - Wrap everything in `runCatching`. A device without Play, or a sideloaded build, must never crash and must show nothing.
   - **Expose it** as `YogaApplication.updateController` (lazy, like the other repositories).
4. **Home line** (`ui/UpdatePrompt.kt`):
   - **Line text:**
     - "Update ready" with the button "Update";
     - "Downloading update…" with no button;
     - "Restart to update" with the button "Restart", which calls `completeUpdate()` in the foreground.
   - **Behaviour:**
     - a long press, or a small "Not now", hides it for 3 days;
     - add it to `statusContent` with **one line** in `MainActivity`, right after `DefaultHomePrompt`;
     - keep all other logic in the new file.
5. **Help → "Check for updates"** (in `HelpScreen`):
   - show "Version 1.2 (3)" from `BuildConfig`;
   - a "Check now" text button forces a check and shows one note: "You're up to date", "Update ready", or "Updates come from Google Play" when Play reports nothing usable (for example, a sideloaded build).
6. **What's new:**
   - `res/raw/whats_new.json`: `[{"versionCode":3,"versionName":"1.2","lines":["A calmer Settings screen with grouped sections","Clearer summaries and a Yoga Pro card"]}]`. Up to 3 lines, each 8 words or fewer.
   - `update/WhatsNew.kt`, pure: given the stored last-run version code, the current version code and the entries:
     - on first run (nothing stored), store the current code and show nothing;
     - when the code went up and an entry exists for the current version, show "New in 1.2 ·" with the button "See what's new".
   - The sheet is one `YogaSheet`/`YogaDialog`-style surface using the kit, with an eyebrow, the version as the title, and the lines as quiet rows.
   - Opening or dismissing it marks it seen.
   - Unit-test the version logic.
7. **`scripts/play-upload.mjs`:**
   - add `--priority N` (0–5, default 2) to the commands that create a release (upload and promote), setting `inAppUpdatePriority` on the track release;
   - validate the range;
   - print it in the summary;
   - update the usage text.
8. **Docs:**
   - ADR 0013 (`docs/adr/0013-in-app-updates.md`);
   - in `docs/launch/release.md`, a short "Choosing an update priority" section: 2 normal, 4 important, 5 critical (rare, full-screen);
   - tick the done items in `openspec/changes/add-in-app-updates/tasks.md`;
   - under "A." in `TODO.md`, tick the items you finished. Leave the heading's `[~]` alone.

## Constraints

- Don't change onboarding, `HomeUsageIndicator`, the guard, billing, or anything outside this scope.
- Copy rules: short, calm, sentence case, no exclamation marks.
- Plain UTF-8 without BOM. No mojibake in "·", "…" or "→".
- Keep `MainActivity` edits minimal. Another branch also adds one home line there, so put your logic in new files.
- When finished, list the files you changed, the tests you added, and anything you couldn't complete or verify.
