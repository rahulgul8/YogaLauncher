# Brief 18: Review moments and feedback (piece F)

Repo: Yoga Phone launcher (Kotlin, Jetpack Compose). Work only in this worktree (`YogaLauncher-reviews`, branch `reviews`). Don't commit; Claude reviews, builds and commits. You can't run Gradle, so write code that compiles first time: check every import and signature against the existing code.

The dependency is already added: `libs.play.review.ktx` (`com.google.android.play:review-ktx:2.0.2`) in `gradle/libs.versions.toml` and `app/build.gradle.kts`. Don't add any other dependency.

Read first:
- `docs/plans/feature-discovery.md`, section 4 "Reviews and feedback at the right moment" (the spec for this piece);
- `docs/adr/0013-in-app-updates.md` and `update/*` (the pattern to follow: a pure policy, a small DataStore store, a controller that talks to Play);
- `intro/IntroActions.kt` and `ui/IntroActionHost.kt` (where the Shorts/Reels confirmation is shown);
- `focus/FocusRepository.kt` (timebox and focus completion), `guard/GuardPrefs.kt` and `guard/GuardBlockHistory.kt` (daily block counts);
- `usage/UsageNotifications.kt` (limit notification), `ui/MindfulDelayScreen.kt` (pause screen);
- `ui/PrivacyScreens.kt` (`HelpScreen`), `ui/OnboardingScreen.kt` (only to find the onboarded flag; don't edit it unless a hook is unavoidable);
- `MainActivity.kt` (home resume, `isDefaultHome`, the onboarded flag).

## Goal

Ask for a Play review only at moments when people have just had a good experience, and give everyone else an easy way to send feedback. Play's policy forbids asking "Do you like Yoga?" before the card, so the moment itself is how we pick happy people.

## 1. Pure policy: `review/ReviewPolicy.kt`

```kotlin
enum class ReviewMoment { R1_BLOCK_FROM_DATA, R2_GOOD_WEEK, R3_THIRD_SESSION, R4_STRONG_BLOCK_DAY }
data class ReviewSignals(
    val now: Long,                 // epoch ms
    val installedAt: Long,         // PackageInfo.firstInstallTime
    val homeSessions: Int,         // home resumes counted by ReviewStore
    val isDefaultHome: Boolean,
    val onboarding: Boolean,       // true while onboarding is showing or not finished
    val lastInterruptionAt: Long?, // latest block card, pause screen or limit notification
    val overLimitToday: Boolean,
    val asks: List<Long>,          // times we already launched the review flow
)
object ReviewPolicy { fun mayAsk(signals: ReviewSignals): Boolean }
```

`mayAsk` is true only when all of these pass:
- installed at least 3 days ago, and at least 5 home sessions;
- Yoga is the default home;
- not in onboarding;
- no interruption in the last 10 minutes;
- not over the limit today;
- no ask in the last 90 days, and fewer than 3 asks ever.

Also add pure helpers for the moments:
- `isGoodWeek(daysWithin: Int, knownDays: Int, thisWeekMinutes: Long, lastWeekMinutes: Long?)`: at least 5 of 7 days within the allowance, or distracting time down 20% or more against last week. Needs at least 5 known days.
- `isStrongBlockDay(blocks: Int, overLimit: Boolean)`: 5 or more Shorts/Reels blocks and not over the limit.
- R4 is asked at the **next evening's** home resume (18:00–22:00 local), never on the block card. Put that check in a pure function too (`isEvening(hour)` plus "the strong day was yesterday or earlier and not yet used").

## 2. Store: `review/ReviewStore.kt` (DataStore, follow `update/UpdateStore.kt`)

Keep:
- `homeSessions` (incremented on each home resume; reuse an existing counter if one already exists, grep first);
- `asks` (list of epoch ms);
- `lastInterruptionAt`;
- `completedSessions` (focus sessions plus timeboxes that finished, not stopped early);
- `pendingMoment` (R4: the date of a strong block day waiting for an evening ask);
- `usedMoments` (each moment fires at most once ever; don't fire R1 twice, for example);
- feature-on times for "What didn't work?" (see 5);
- the last 3 "What didn't work?" answers.

## 3. Controller: `review/ReviewController.kt`

- `suspend fun onMoment(activity: Activity, moment: ReviewMoment)`: builds `ReviewSignals`, checks `ReviewPolicy.mayAsk` and `usedMoments`, then runs `ReviewManagerFactory.create(context).requestReview()` → `launchReview(activity, info)`. Record the ask and the moment **before** launching (Play never tells us whether the card showed). Catch every exception; a failure is silent.
- `recordInterruption()`: called when a block card shows, the pause screen shows, or a limit notification is posted.
- Hold it on `YogaApplication`, like the other controllers.
- **Debug only:** use `FakeReviewManager` when an intent extra `--ez review_fake true` is set (follow how `IntroSignalsReader.setPreview` gates its debug extra; never in release).

## 4. Wiring the moments

- **R1:** in `IntroActions`, after the Shorts or Reels introduction's confirmation (the person came back from Android settings and blocking is on). Also from onboarding's Shorts & Reels step if it turns blocking on, only if a non-invasive hook exists; otherwise skip and say so. Ask a few seconds after the confirmation shows, not on top of the disclosure dialog.
- **R2:** the weekly review screen doesn't exist yet (piece G). For now, evaluate R2 when the person opens "Last 7 days" on the Usage screen (`ui/UsageDashboardScreen.kt`), on leaving that screen. Keep the trigger in one function so piece G can move it.
- **R3:** when the 3rd focus session or timebox completes (not stopped early), ask at the next home resume.
- **R4:** the guard service records a strong block day into `pendingMoment`; the ask happens at the next evening's home resume.
- **Interruptions:** call `recordInterruption()` from the block card, `MindfulDelayScreen` and the limit notification.
- **Home sessions:** count in the existing home resume path in `MainActivity` (once per resume, not per recomposition).

## 5. Feedback

- **Help → Send feedback:** a new row in `HelpScreen`. Opens an `ACTION_SENDTO` `mailto:tuwi.developer@gmail.com` email with the subject "Yoga Phone feedback" and a body prefilled with the app version (name and code), Android version and device model. If the person answered "What didn't work?" recently, add those answers to the body too. The person sees the email before sending. If no email app exists, show a notice "No email app found".
- **"What didn't work?":** when someone turns **off** the guard, the daily allowance, or the mindful pause within 7 days of turning it on, show one optional line under the switch (not a dialog): "What didn't work?" with 3 tap reasons plus Other.
  - Guard: "Blocked too much", "Didn't block enough", "Something else broke".
  - Allowance: "Too strict", "Numbers looked wrong", "Didn't need it".
  - Pause: "Too slow", "Annoying", "Didn't help".
  - Other opens a short text field. Offer the last 3 Other texts as tap-to-fill chips; reuse an existing recent-entries helper if one exists (`ui/RecentEntries.kt`), otherwise keep them in `ReviewStore`.
  - Answers stay on the phone (in `ReviewStore`) and go into the next feedback email. Nothing is sent automatically. Dismissing the line is one tap.
- Record the "turned on" time whenever one of those three features is turned on.

## 6. Docs

- Add a "Reviews and feedback" section to `docs/adr/0013-in-app-updates.md` (the plan assigns ADR 0013 to updates and reviews).
- Add an OpenSpec change `openspec/changes/add-review-moments/` (proposal, tasks, spec delta), following `openspec/changes/add-in-app-updates/`. Read `openspec/AGENTS.md` for the format.

## Tests (`app/src/test/java/in/tuwi/yogaphone/review/`)

- `ReviewPolicyTest`: each guard alone blocking the ask, the 90-day and 3-ever caps, and all guards passing.
- Good week, strong block day, and the evening R4 rule.
- The 7-day window for "What didn't work?".
- The feedback email body builder (pure function).

## Constraints

- No new dependencies beyond review-ktx. Don't touch `TODO.md`, `update/*` logic, or the guard's blocking rules (another plan is changing how blocking and bypass work; touch `guard/*` only to record interruptions and block-day counts).
- Keep the kit components and calm look; no new colours.
- All user-facing text short and plain.

## Done when

- It compiles. Claude runs `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- Your summary lists the files changed, how each moment is wired, and any assumptions or skipped parts.
