# Yoga Launcher - Development Roadmap

## Overview
This roadmap organizes the Yoga Launcher features into prioritized development phases, from MVP to advanced features. Each phase builds on the previous one, ensuring a solid foundation while delivering value incrementally.

---

## Development Strategy: Solo Developer Approach

This roadmap is structured for solo development in three major stages:
1. **STAGE 1: Working Prototype (Weeks 1-8)** - Build a functional launcher with core features
2. **STAGE 2: Essential Features (Weeks 9-16)** - Add features that make it distinctly "Yoga Launcher"
3. **STAGE 3: Polish & Ship (Weeks 17-24)** - Testing, optimization, and Play Store release

Focus on getting things working quickly, then iterate and improve.

---

# STAGE 1: WORKING PROTOTYPE

## Phase 0: Minimal Setup
**Goal:** Just enough setup to start building
**Timeline:** Day 1-2
**Status:** ✅ Complete

### Tasks

#### 0.1 Basic Project Structure
- [x] Create package structure: `ui/`, `data/`, `utils/`
- [x] Add Room database dependency
- [x] Add ViewModel/Lifecycle dependencies
- [x] Add Coroutines dependencies
- [x] Add DataStore for simple preferences

#### 0.2 Minimal Design System
- [x] Define 3 color schemes in Theme.kt (Light/Dark/Greyscale)
- [x] Set up basic typography (use Material 3 defaults, adjust sizes)
- [x] That's it - build as you go

**Note:** No CI/CD, no linting setup, no complex DI frameworks. Keep it simple. Use basic ViewModelProvider factory if needed.

---

## Phase 1: Basic Launcher
**Goal:** Functional launcher that can replace default launcher
**Timeline:** Week 1-3
**Status:** ✅ Complete

### Tasks
- [x] Add launcher intent filters in AndroidManifest (HOME, MAIN categories)
- [x] Request launcher as default (no complex permission handling yet)
- [x] Query installed apps using PackageManager
- [x] Create AppModel data class (name, package, activity and profile metadata)
- [x] Create Room database with Apps table (just favorite flag for now)
- [x] Build basic ViewModel with app list
- [x] Create text-only home screen UI:
  - [x] LazyColumn with favorite app names and adaptive spacing
  - [x] Large, readable typography
  - [x] Click to launch app
- [x] Implement app launch (create Intent, startActivity)
- [x] Long-press to favorite/unfavorite (simple toggle)
- [x] Implement search/app drawer interaction:
  - [x] Add home button at bottom of screen
  - [x] Detect swipe-up gesture from bottom
  - [x] Show bottom sheet with all apps (animated slide from bottom)
  - [x] Add search TextField at top of bottom sheet
  - [x] Filter apps as user types
  - [x] Tap outside or swipe down to close
- [x] Simple settings screen:
  - [x] Font size (3 options)
  - [x] Theme toggle (Light/Dark)
  - [x] Store in DataStore

**Acceptance Criteria:**
- Works as default launcher
- Shows favorite apps on home screen
- Search/app drawer opens with home button tap OR swipe-up gesture
- Bottom sheet animates smoothly from bottom
- Can search and launch any app from drawer
- Basic settings work

**Keep it Simple:** Don't worry about edge cases yet. Just get it working.

**UX Note:** Home screen shows only favorites. To access all apps, user taps the home button at bottom or swipes up from bottom, which pulls up the app drawer as a bottom sheet with search capability.

---

## Phase 2: Focus Mode (Minimal)
**Goal:** Basic app blocking during focus time
**Timeline:** Week 4-5
**Status:** ⏳ Pending

### Tasks
- [x] Request PACKAGE_USAGE_STATS permission
- [ ] Request AccessibilityService permission (not used: Yoga pauses launches it starts itself, no Accessibility service)
- [ ] Create AccessibilityService to detect app launches (deferred, see above)
- [x] Build simple FocusMode entity in database (enabled, blockedApps list)
- [x] Add Focus Mode toggle in settings
- [x] Select apps to block (multi-select list)
- [x] When app is launched, show blocking overlay:
  - [x] Simple fullscreen composable
  - [x] "This app is blocked" message
  - [x] Countdown (5 seconds fixed for now)
  - [x] "Go Back" button
- [x] After countdown, allow app to open
- [ ] Add persistent notification when Focus Mode is on (focus status shows on home instead)

**Acceptance Criteria:**
- Focus Mode can be toggled on/off
- Selected apps show delay overlay
- Overlay dismisses after countdown
- Notification shows Focus Mode status

**Skip for now:** Scheduling, DND integration, greyscale, complex UI

---

## Phase 3: Timer & Basic Stats
**Goal:** Pomodoro timer and simple usage tracking
**Timeline:** Week 6-8
**Status:** ⏳ Pending

### 3.1 Timebox Timer
- [x] Create FocusSession entity (startTime, duration, completed)
- [x] Build timer screen:
  - [x] Display countdown (25 min default)
  - [x] Start/Stop buttons
  - [x] Simple progress indicator
- [x] Implement foreground service for timer (alarm + countdown notification instead of a service)
- [x] Show notification with timer countdown
- [ ] Play simple beep on completion
- [x] Store completed sessions in database
- [ ] Show count of sessions today on home screen

**Acceptance Criteria:**
- Timer runs in background
- Notification shows time remaining
- Sessions are logged

### 3.2 Basic Usage Stats
- [x] Query UsageStatsManager for today's data
- [x] Store in simple UsageStats entity
- [x] Create basic dashboard screen:
  - [ ] Total screen time today
  - [ ] Number of unlocks
  - [ ] Top 3 apps used
  - [ ] Focus sessions completed
- [ ] Add dashboard icon on home screen

**Acceptance Criteria:**
- Dashboard shows accurate today's stats
- Data updates when screen is opened

**Skip for now:** Charts, goals, streaks, AI insights

---

**🎯 End of Stage 1: You now have a working minimalist launcher with focus mode and timer!**

---

# STAGE 2: ESSENTIAL FEATURES

## Phase 4: Profiles System
**Goal:** Separate app contexts (Work/Personal/Study)
**Timeline:** Week 9-11
**Status:** ⏳ Pending

### Tasks
- [ ] Create Profile entity (id, name, color, apps list)
- [ ] Build Profile management screen:
  - [ ] List profiles
  - [ ] Create/edit/delete
  - [ ] Assign apps to profiles
- [ ] Add profile switcher on home screen (dropdown or bottom sheet)
- [ ] Filter home screen by active profile
- [ ] Store active profile preference
- [ ] Add 3 default profiles on first launch

**Acceptance Criteria:**
- Can create and switch profiles
- Home screen shows only apps in active profile
- Profile persists across app restart

**Skip for now:** Auto-switching, complex rules, AI categorization

---

## Phase 5: Better Dashboard & Insights
**Goal:** Useful analytics and insights
**Timeline:** Week 12-13
**Status:** ⏳ Pending

### Tasks
- [x] Enhance usage tracking (track per-app time, not just totals)
- [ ] Build better dashboard:
  - [x] Week view (last 7 days)
  - [ ] Simple bar chart for daily screen time
  - [x] App usage breakdown (list with durations)
  - [ ] Streak counter (consecutive days with <X screen time)
- [ ] Add simple goal setting:
  - [x] Daily screen time goal
  - [x] Show progress bar
  - [x] Notification when goal exceeded
- [ ] Create 3-5 insight templates:
  - [ ] "Screen time down X% this week"
  - [ ] "You unlocked phone X times today"
  - [ ] "Most productive hour: Xam-Xpm"
- [ ] Show one random insight on home screen

**Acceptance Criteria:**
- Dashboard shows weekly trends
- Goals can be set and tracked
- Insights are accurate and encouraging

**Skip for now:** Advanced charts, AI predictions, reflection prompts

---

## Phase 6: Motivation System
**Goal:** Keep users engaged
**Timeline:** Week 14-15
**Status:** ⏳ Pending

### Tasks
- [ ] Implement streak tracking:
  - [ ] Count consecutive days meeting screen time goal
  - [ ] Store in database
  - [ ] Display prominently on home screen
- [ ] Create 5-10 simple milestones:
  - [ ] First focus session
  - [ ] 7-day streak
  - [ ] 30-day streak
  - [ ] 100 focus sessions
- [ ] Show celebration dialog when milestone unlocked
- [ ] Add milestones gallery (grid showing locked/unlocked)
- [ ] Simple points system:
  - [ ] 1 point per focus session
  - [ ] Display total on dashboard

**Acceptance Criteria:**
- Streak is tracked and displayed
- Milestones unlock at correct times
- Celebration feels rewarding

---

## Phase 7: Onboarding
**Goal:** Smooth first-time experience
**Timeline:** Week 16
**Status:** ⏳ Pending

### Tasks
- [ ] Create 4-screen onboarding flow:
  - [ ] Welcome (explain philosophy)
  - [ ] Permissions (explain why each is needed)
  - [ ] Setup (choose favorite apps, set goal)
  - [ ] Tutorial (show gestures, focus mode)
- [ ] Request all permissions during onboarding
- [ ] Let user select initial favorite apps
- [ ] Set initial screen time goal
- [ ] Store "onboarding complete" flag
- [ ] Add "skip" option

**Acceptance Criteria:**
- First launch shows onboarding
- All required permissions requested
- User understands core features

---

**🎯 End of Stage 2: You now have a feature-complete Yoga Launcher!**

---

# STAGE 3: POLISH & SHIP

## Phase 8: Testing & Bug Fixes
**Goal:** Ensure stability
**Timeline:** Week 17-18
**Status:** ⏳ Pending

### Tasks
- [ ] Manual testing of all flows
- [ ] Test on physical device (not just emulator)
- [ ] Write critical unit tests:
  - [ ] ViewModels
  - [ ] Repository logic
  - [x] Usage stats calculation
- [ ] Write key UI tests:
  - [ ] App launch flow
  - [ ] Focus mode activation
  - [ ] Timer functionality
- [ ] Fix all critical bugs
- [ ] Test edge cases:
  - [ ] No permissions granted
  - [ ] No apps installed
  - [ ] Battery optimization killing service
  - [ ] App uninstall during focus mode

**Acceptance Criteria:**
- No crashes in main flows
- Core features work reliably
- Tests cover critical paths

---

## Phase 9: Optimization & Polish
**Goal:** Make it fast and accessible
**Timeline:** Week 19-20
**Status:** ⏳ Pending

### Tasks
- [ ] Profile with Android Profiler (fix major issues)
- [ ] Optimize database queries (add indexes)
- [ ] Enable R8/ProGuard for release
- [ ] Reduce APK size:
  - [ ] Remove unused resources
  - [ ] Optimize images
- [ ] Add content descriptions for accessibility
- [ ] Test with TalkBack
- [ ] Support dynamic font sizes
- [ ] Extract strings to resources (prepare for translation)
- [ ] Add error handling and fallbacks
- [ ] Polish animations and transitions
- [ ] Improve empty states and loading states

**Acceptance Criteria:**
- App starts in <2 seconds
- No major memory leaks
- APK size <15MB
- Accessible to TalkBack users

---

## Phase 10: Release Preparation
**Goal:** Get ready for Play Store
**Timeline:** Week 21-22
**Status:** ⏳ Pending

### Tasks

#### **NOW** add the external tooling:
- [ ] Set up GitHub Actions for automated builds
- [ ] Add Crashlytics (or similar) for crash reporting
- [ ] Add basic analytics (Firebase or simple on-device only)
- [ ] Set up ProGuard rules properly
- [ ] Configure app signing for release

#### Play Store prep:
- [ ] Create production signing key (store securely!)
- [ ] Update version to 1.0.0
- [ ] Write release notes
- [ ] Create privacy policy document
- [ ] Set up support email/website
- [ ] Create Play Store listing:
  - [ ] App description
  - [ ] Screenshots (5-8 images)
  - [ ] Feature graphic
  - [ ] App icon
- [ ] Internal testing (install on your device, use for 1 week)
- [ ] Create closed alpha track
- [ ] Invite 5-10 friends to test
- [ ] Gather feedback, fix critical issues
- [ ] Create release build

**Acceptance Criteria:**
- Signed release APK builds successfully
- Play Store listing complete
- Alpha testing shows no critical issues
- Privacy policy published

---

## Phase 11: Launch & Iterate
**Goal:** Release and improve
**Timeline:** Week 23-24+
**Status:** ⏳ Pending

### Tasks
- [ ] Submit to Play Store production
- [ ] Monitor crash reports daily (first week)
- [ ] Respond to user reviews
- [ ] Fix critical bugs in patches
- [ ] Plan next feature updates based on feedback
- [ ] Consider open-sourcing (optional)
- [ ] Build community (Reddit, Discord, etc.)

**Post-Launch Feature Ideas:**
- Advanced AI insights (TensorFlow Lite)
- Contextual profile switching (Wi-Fi, location, time)
- Widgets
- Premium features & billing
- WearOS companion
- More themes and customization

---

**🎯 End of Stage 3: Yoga Launcher is live on Play Store!**

---

# Summary: Solo Developer Timeline

## Quick Overview

**Stage 1 (8 weeks):** Working Prototype
- Phase 0: Minimal setup (2 days)
- Phase 1: Basic launcher (Weeks 1-3)
- Phase 2: Focus mode (Weeks 4-5)
- Phase 3: Timer & basic stats (Weeks 6-8)

**Stage 2 (8 weeks):** Essential Features
- Phase 4: Profiles (Weeks 9-11)
- Phase 5: Dashboard & insights (Weeks 12-13)
- Phase 6: Motivation (Weeks 14-15)
- Phase 7: Onboarding (Week 16)

**Stage 3 (8 weeks):** Polish & Ship
- Phase 8: Testing (Weeks 17-18)
- Phase 9: Optimization (Weeks 19-20)
- Phase 10: Release prep (Weeks 21-22)
- Phase 11: Launch (Weeks 23-24+)

**Total time to Play Store:** ~24 weeks (6 months)

---

## Key Principles for Solo Dev

1. **Build first, perfect later** - Get features working before polishing
2. **No premature optimization** - Focus on functionality in Stage 1 & 2
3. **External tools in Stage 3 only** - CI/CD, analytics, monitoring come last
4. **Test on real device** - Use your own phone as primary test device
5. **Ship incrementally** - Each stage is usable, gather feedback early
6. **Keep it simple** - Avoid over-engineering, use simple solutions

---

## What Makes This Different from the Original Features Doc?

The features document lists ALL possible features. This roadmap:
- **Simplifies** features into achievable chunks
- **Prioritizes** ruthlessly (MVP → Essential → Nice-to-have)
- **Sequences** dependencies properly
- **Removes** external tooling from early phases
- **Focuses** on solo developer workflow

---

## Next Steps

1. ✅ Project structure documented (`openspec/project.md`)
2. ✅ Roadmap created with clear stages
3. **Start Phase 0:** Set up basic packages and dependencies
4. **Start Phase 1:** Build the basic launcher

Good luck building Yoga Launcher! 🧘‍♂️

## Piece 2 — Phase 1 usage awareness (6 November 2026)

- [x] Usage Access explanation, permission status and unavailable-data fallback.
- [x] Daily/app foreground normalization, essential exclusions and editable classifications.
- [x] Total/per-app allowances, eligibility choices and configurable near/over thresholds.
- [x] Today/last-seven-days dashboard and quiet home progress indicator.
- [x] Local aggregate history, daily/periodic work and once-per-day-state reminders.
- [x] Optional hourly value, currency, calculation details and snoozable silent notification.
- [x] Pure JVM coverage for usage windows, thresholds, rounding and reminder deduplication.

Foreground totals deliberately differ from screen-on time; unlock/focus counts and trend
claims remain future work. Weekly comparisons use the current allowance and recorded
eligibility, with missing days disclosed. Android foreground events are approximate,
queried with a 24-hour lookback; WorkManager checks can be delayed by the OS.
No overlay or network access is introduced. Usage DB and preferences are excluded from backup.

Verification: pure JVM tests run independently; full Android packaging/build requires an
accessible Android 36 SDK and Gradle cache. Physical-device checks (permission revocation,
OEM event feeds, notification permission, DST/timezone and midnight rollover) remain pending.

## Piece 3 — Timebox, focus and mindful pause (6 November 2026)

- [x] Timebox with presets, custom length, purpose chips, optional bound app, pause/resume/extend, countdown notification and completion prompt; survives reboot.
- [x] Focus sessions (manual or "until I stop") and recurring schedules, including windows past midnight; distracting apps step aside on home and in the drawer.
- [x] Optional greyscale launcher and Do Not Disturb during focus, restored afterwards only if Yoga changed them.
- [x] Mindful pause before distracting apps (2–10 s, +5 s during focus), purpose chips, "don't ask again" window, per-app opt-out; outcomes recorded.
- [x] Streak count (days with a completed focus session or timebox) shown in completion messages.

## Piece 4a — Categories and profiles (6 November 2026)

- [x] Built-in and custom categories with local auto-categorisation; recategorise from the drawer or the Categories screen; optional drawer grouping.
- [x] Category-level daily limits in Usage rules and per-category totals on the usage dashboard.
- [x] Profiles (Priority 2): own ordered home apps, optional "tuck other apps away" with an All apps escape, start focus, theme override.
- [x] Profile switcher on home (active profile always labelled) and a profile manager; schedule-linked automatic switching where a manual switch wins until the window ends.

## Piece 4b — Greyscale workflow and quiet notifications (6 November 2026)

- [x] Launcher greyscale on triggers (over today's limit, during focus, during a chosen bedtime schedule) with a home indicator and 30-minute snooze.
- [x] Device-wide greyscale guidance (Accessibility, Digital Wellbeing) and an advanced adb-granted path that switches Android's colour correction and restores the user's previous setting.
- [x] Quiet notifications via an opt-in notification listener: per-app Allow / Hold in focus / Always hold; calls, alarms, ongoing, media and Yoga's own never held; only title and text kept, for at most a day.
- [x] Digest delivered when focus ends and at chosen times, with a held-notifications screen grouped by app.
