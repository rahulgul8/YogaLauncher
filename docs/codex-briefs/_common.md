You are working in a git worktree of YogaLauncher ("Yoga Phone"): an Android launcher in Kotlin, Jetpack Compose, Material 3, minSdk 26, source package `in.tuwi.yogaphone` (applicationId `com.tuwi.yogaphone`). Sources are under `app/src/main/java/in/tuwi/yogaphone/`, unit tests under `app/src/test/`.

The founder's requirement list is `docs/plans/bugs-and-features-2026-11.md` (section L of `TODO.md`). Read your item there first.

Rules for every brief:
- Use the Yoga kit in `ui/theme/YogaComponents.kt` and `YogaIcons` for any UI; copy the look of neighbouring screens. Sentence-case copy, short words, no exclamation marks.
- No new dependencies. A Room schema change only if the brief allows it, with a migration and the exported schema updated.
- Do not commit, push, switch branches or install anything. Leave your changes in the working tree.
- Prove it: `./gradlew :app:testDebugUnitTest` and `./gradlew :app:compileDebugKotlin` (add `--offline` if the network is blocked). If you can't run Gradle, say so plainly; never claim tests pass that you didn't run.
- Finish with a short report: what you changed and why, files touched, tests added, anything you could not verify, and anything that needs a real phone.
- UTF-8 without a BOM.
