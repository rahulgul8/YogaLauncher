# Brief 11: Learning videos (YouTube learning channels)

You are working in a git worktree of the Yoga Phone Android app (Kotlin, Jetpack Compose, code namespace `in.tuwi.yogaphone`, app id `com.tuwi.yogaphone`) on branch `learning-videos`. Claude reviews, builds and commits. Your sandbox can't run Gradle or fetch packages; don't add dependencies.

Read first:
- `openspec/changes/add-learning-videos/`: proposal, design (how a video is recognised, accounting, focus, starter chips) and spec. **Approved scope.**
- `app/src/main/java/in/tuwi/yogaphone/guard/`: `ShortsDetector.kt` (bounded accessibility-tree walk; `NodeView` abstraction for tests), `YogaGuardService.kt` (tick loop, live pill, Back/Home escalation, cards), `GuardPrefs.kt`, `BurnMeter.kt`, `OverlayPolicy.kt`.
- `app/src/main/java/in/tuwi/yogaphone/pro/`: `ProGate` / `ProFeature.LEARNING_VIDEOS`. Learning videos is a **Pro** feature: when FREE, the settings entry shows the Pro tag and opens the Yoga Pro screen; the guard ignores the learning list.
- `docs/design/brand.md`, `ui/theme/YogaComponents.kt`, `ui/theme/YogaIcons.kt`: every screen uses the Yoga kit and YogaIcons only. Never hard-code a colour or font.
- `ui/RecentEntries.kt` / existing recent-text chips: reuse for "recently added channels".
- ADR format: `docs/adr/0010-yoga-pro.md`. Your ADR is **0011**.

## Build

1. **Watch-page reader** (`guard/LearningDetector.kt`, pure, tested with `NodeView` fixtures):
   - On YouTube (`com.google.android.youtube`) find the channel name on the watch page (view id first, then content-description patterns such as "Go to channel …" / channel avatar descriptions) and the video title. Return `WatchInfo(channel: String?, title: String?)`.
   - Normalise for matching: case-fold, trim, collapse spaces, strip emoji and a trailing " - Topic".
   - Bounded traversal like ShortsDetector (node and depth caps). Never store or log titles or channel names except channels the user added.
   - Write fixtures for at least two YouTube layouts, the mini-player and a non-watch screen.
2. **Learning store** (`guard/LearningPrefs.kt`, DataStore):
   - Learning channels (display name + normalised key) and daily learning seconds per package (local day).
   - Recently added channels for chips (last 3, newest first, de-duplicated).
   - Include channels in backup export/import (`privacy/BackupCodec`, `BackupManager`); include learning totals in "delete history".
3. **Guard integration** (`YogaGuardService`):
   - Each tick on YouTube with Pro allowed: if the channel is on the list, the state is LEARNING. While LEARNING:
     - accumulate learning seconds;
     - don't count the time toward YouTube's surface allowance;
     - the live pill shows a calm "Learning · 12 min" state in the palette's accent, not burning.
   - Unknown or unreadable channel means normal counting (the safe default). Autoplay to a non-learning channel flips back within one tick.
   - **Pill action "Count as learning":** a tap on the expanded pill while a non-learning watch page is open adds the channel (one tap, with a brief "Added <channel>" confirmation).
   - **"Is this learning?" hint:** if the title contains study/work words (lecture, tutorial, course, chapter, class, lesson, explained, how to, interview prep; editable list in code), the pill shows the action more prominently. Never auto-add.
   - **Focus:** when a focus session is active (FocusRepository state), a non-learning YouTube watch page uses the existing Back → Home escalation and shows the card "Not on your learning list" with "Add channel". Shorts stay blocked. The YouTube home feed and search results are also blocked during focus unless the setting "Allow YouTube home during focus" is on (default off).
4. **Usage accounting:** today's YouTube use shown on the dashboard and counted against allowances = measured use minus today's learning seconds (never below zero). Show "Learning time today" on the usage dashboard and a learning row in insights.
5. **Settings screen** (`ui/LearningChannelsScreen.kt`, route `settings/learning`, reached from the Settings "Awareness" section row "Learning videos" and from guard settings):
   - Your channels (remove with an undo snackbar; no confirmation dialog).
   - A text field to add a channel by name, with the 3 most recent additions as chips.
   - Starter chips from the design doc, unselected by default.
   - The focus setting switch.
   - A short note on how recognition works and that titles are never stored.
6. **Docs:**
   - ADR 0011.
   - Update `docs/launch/privacy-policy.md`: Yoga guard reads YouTube channel names and titles in memory; only the channels you add are stored.
   - Update `docs/launch/play-listing.md`: the guard declaration text and a "Learning videos" bullet.
   - Tick the done items in the change's `tasks.md`.

## Constraints

- Don't change Shorts detection behaviour, the drawer, `DrawerController` or home performance code.
- Keep every guard decision unit-testable (pure functions plus thin Android glue). Add JUnit tests for the detector, normalisation, accounting (learning seconds subtracted, never negative, day rollover) and the focus decision.
- Plain UTF-8 without BOM. No mojibake in "·", "…" or "→". Check `MainActivity.kt` for any garbled strings you introduce.
- When finished, list the files you changed and anything you couldn't complete.
