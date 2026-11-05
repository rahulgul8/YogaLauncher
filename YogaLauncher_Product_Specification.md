# Yoga Launcher

## Product and feature specification

**Version:** 1.0 — consolidated discussion draft  
**Updated:** 5 November 2026  
**Status:** Product direction documented; detailed rules and visual designs remain to be finalized

## 1. Executive summary

Yoga Launcher (also discussed as **Yoga Phone: Minimalist Launcher**) is an Android launcher and digital-wellbeing application intended primarily for adults who want to reduce habitual, unintentional phone use. Its promise is a calmer phone that helps users make conscious choices, not a punitive lockout system.

The launcher replaces a stimulation-heavy home screen with a minimalist interface and introduces progressive interventions based on actual app usage: visibility of screen time, usage allowances, mindful delays, grayscale, focus profiles, and contextual reminders. A distinctive concept is to let users assign a monetary value to their time and visualize the **opportunity cost of excess usage** as money being burned.

This document distinguishes **discussed product decisions** from **proposed implementation details**. The latter are recommendations for planning, not decisions already made.

## 2. Product identity and positioning

### Working name

- **Yoga Launcher** — working project/product name.
- **Yoga Phone: Minimalist Launcher** — previously considered consumer-facing Play Store positioning.
- The naming discussion compared “Yoga Phone” and “Yoga Launcher”; a final package name, store title, icon, tagline, and brand guidelines have not been locked.

### Audience

- Adults concerned about unproductive screen time, endless scrolling, distraction, and bedtime phone use.
- Users who want a minimalist launcher but still need a functional everyday Android phone.
- Users motivated by measurable improvements and understandable tradeoffs, including the value of their time.

### Core principles

1. **Awareness before restriction.** Show users what is happening, then support informed changes.
2. **Gradual intervention.** Reminders and friction should precede more intrusive modes.
3. **User control.** Clear allowances, schedules, exceptions, and easy-to-understand settings.
4. **Calm visual identity.** Minimalist layouts, low visual noise, restrained animations.
5. **Respectful feedback.** Avoid shame, sensational addiction diagnoses, and misleading cost claims.
6. **Reliability first.** Critical phone functions, accessibility, notifications, and emergency flows remain usable.

## 3. Core user journeys

### Journey A — First installation and activation

1. User installs and opens Yoga Launcher as an ordinary Android app.
2. A setup flow explains what the launcher changes, what data it reads, and which optional capabilities require permission.
3. User chooses whether to make Yoga Launcher the default home app.
4. User selects preferred home-screen style, essential apps, and categories.
5. User grants Usage Access to enable app-usage awareness (with a clear explanation of the Android settings handoff).
6. User sets a daily screen-time allowance and optionally allowances for specific distracting apps or categories.
7. User can enter an **hourly value of time** for the money-cost visualization; opt-out is available.
8. User chooses initial interventions, such as reminders, grayscale, deliberate delays, and focus hours.
9. A brief preview demonstrates what an allowance warning and excess-usage indication will look like.
10. User lands on the minimalist home screen with a visible way to change settings or leave the launcher.

**Decision status:** A guided onboarding walkthrough was requested but placed later in the implementation priority, closer to launch. The flow above is a recommended breakdown.

### Journey B — Normal daily use

1. User opens the phone to a clean, minimal home screen.
2. Essential apps, search, and optional widgets remain accessible.
3. Usage is tracked against the user's allowance, with a compact progress indicator.
4. When the user approaches a configured threshold, Yoga provides a neutral reminder.
5. If an allowance is exceeded, the selected intervention activates or becomes available.
6. The user can intentionally continue, take a timed break, start focus mode, or adjust the allowance.
7. The launcher later summarizes progress and reinforces improvements.

### Journey C — Excess-use monetary indicator

1. During setup, the user specifies the monetary value of one hour (for example, ₹500 per hour).
2. The user sets a daily usage allowance (for example, 90 minutes of selected distracting apps).
3. Once excess tracked usage begins, an unobtrusive floating or overlay indicator can show the accumulating **equivalent value of extra time**, such as “₹42 of time used beyond your limit.”
4. The indicator can be dismissed, snoozed, or disabled, according to user preferences.
5. A details view explains the calculation, the contributing apps, and the period.

**Illustrative formula:** `excess minutes × hourly value / 60`. This is an illustrative valuation of time, **not actual money deducted, income lost, or a verified financial loss**. The product should say so explicitly. The “burning money” animation or metaphor is an idea to validate for motivation versus anxiety.

### Journey D — Timed intentional use

1. User opens a timebox control from the launcher.
2. User selects a duration, optionally an app or purpose, and starts a countdown.
3. The countdown remains easy to check without demanding constant attention.
4. At the end, Yoga prompts the user to stop, extend intentionally, or switch to a break/focus state.
5. Completion contributes to the day's usage and mindfulness insights.

## 4. Feature inventory and behavior

The priorities below capture the discussion where explicit: **app grouping and profile modes moved to Priority 2**, while the **onboarding walkthrough was deferred toward launch**. Exact priority placement of the remaining features is a recommended working plan, not a previously locked ranking.

### 4.1 Minimalist launcher foundation — Priority 1 recommended

**Purpose:** Provide a useful daily home screen that users will keep as their default launcher.

**Capabilities**

- Simple home screen, restrained typography, focused app shortcuts, and minimal visual clutter.
- App drawer/list with fast search and app launching.
- Favorite and essential apps, with easy reordering.
- Clear access to system settings and Yoga settings.
- Support for standard Android navigation, home behavior, and app shortcuts where feasible.
- Optional clock/date and concise daily usage summary.
- Widget support, including adding, removing, placing, and resizing widgets where supported.
- Wallpaper, dark/light appearance, typography, accent colors, and icon treatment to be resolved during design.

**Design notes:** OLauncher was discussed as a possible base. Validate its current license, maintainability, architecture, compatibility, and the effort to extend it before committing. Avoid assuming all features of a traditional launcher are included in the base project.

**Acceptance direction:** Home/default-app switching works; search and launch remain responsive; essential apps are not hidden; widgets survive normal launcher recreation.

### 4.2 Usage measurement and allowances — Priority 1 recommended

**Purpose:** Establish trustworthy usage awareness and thresholds.

**Capabilities**

- Read app usage with Android Usage Access after explicit permission.
- Show daily total, category/app totals, and comparison to allowances.
- Allow a daily total allowance and app-level or category-level limits.
- Support configurable warnings before and after thresholds (recommended, not yet locked).
- Exclude or specially handle essential apps such as phone, messages, maps, authentication, and accessibility tools.
- Daily reset based on local time; handle midnight, timezone changes, and missing/partial data robustly.
- Explain differences between app usage, screen-on time, and launcher-measured eligible usage.

**Open choice:** Whether the main allowance applies to total phone use or only chosen distracting apps; recommended default is selected distracting categories, with an optional all-app view.

**Acceptance direction:** Usage totals reconcile reasonably with the Android system's reported usage; exclusions and day boundaries behave predictably; unavailable access produces a helpful fallback state.

### 4.3 Grayscale intervention — Priority 1 recommended

**Purpose:** Reduce the visual reward of prolonged use.

- User can enable or disable a grayscale-related mode.
- Potential triggers: exceeding allowance, entering focus mode, and scheduled bedtime hours.
- Support manual override or temporary snooze.
- Clear indicator that a mode is active and how to exit it.

**Android feasibility:** A third-party launcher cannot generally enforce device-wide grayscale solely by being the default launcher. Explore supported system settings/user-driven flows and platform-specific capabilities; do not promise silent, universal color-filter control. Any implementation relying on Accessibility, special permissions, or device-owner privileges needs separate review of Android behavior and Play policy.

### 4.4 Focus mode and deliberate app opening — Priority 1 recommended

**Purpose:** Make distraction an intentional choice.

- Focus sessions with selectable duration and optional scheduling.
- Hide, deemphasize, or group distracting apps on the launcher during focus.
- Show a deliberate delay or pause screen before launching selected apps.
- Offer an intentional “Continue anyway” choice, potentially with a brief reason/purpose prompt.
- Allow exceptions and emergency access.
- Show a session timer and completion state.

**Design considerations:** Delays should be configurable and modest, not forced repeatedly for urgent access. Launcher-only friction can be bypassed when an app opens from another entry point; stronger intervention mechanisms require feasibility and policy evaluation.

### 4.5 Notification filtering and quieting — Priority 1 feasibility, staged delivery

**Purpose:** Reduce interruptions that trigger habitual checking.

- Let users choose which app notifications are distracting versus essential.
- Offer quiet windows aligned with focus sessions or schedules.
- Potential notification digest or grouped review surface.
- Ensure critical communications remain visible and configurable.

**Android feasibility:** Notification reading and listener access require explicit user permission; a launcher cannot silently suppress every notification or freely control every app's delivery. Specify whether the feature filters Yoga's presentation, uses notification-listener capabilities, or guides the user to system controls. Preserve transparency around permissions and data handling.

### 4.6 Timebox timer — Priority 1 recommended

**Purpose:** Support purposeful, bounded phone sessions.

- Start from home screen or quick action.
- Preset durations plus custom duration.
- Optional label, app, or goal.
- Countdown, pause/cancel, end notification, and intentional extension.
- Optional focus-mode coupling.
- Record completed/extended sessions in insights.

**Open choices:** Whether a timebox is a general timer or can be bound to a particular app; whether exceeding a timebox triggers only a prompt or an intervention.

### 4.7 Money value of time — Priority 1 experiment recommended

**Purpose:** Translate excess usage into a personally meaningful opportunity-cost metaphor.

- Setup asks for an hourly monetary value with currency appropriate to locale.
- Allow manual edits, pause, and opt-out.
- Compute excess-use equivalent based on agreed measurement rules.
- Show a compact, optional persistent/floating indicator after threshold.
- Daily and weekly equivalent-value summaries.
- Explain assumptions, round sensibly, and avoid claims of actual financial loss.

**Floating indicator constraints:** Drawing over other apps requires special overlay permission and may interfere with other applications or accessibility. Offer a launcher-only indicator or notification as a fallback. Define behavior for lock screen, payments, calls, camera, and other sensitive/immersive contexts; avoid overlays where inappropriate.

**UX hypothesis to test:** A subtle live number may motivate more effectively than a dramatic burning animation, which may feel shaming or annoying.

### 4.8 Automatic app categories — Priority 1 or 2 depending on foundation

**Purpose:** Reduce setup effort and enable category-level allowances.

- Prepopulate sensible categories from available Android/package metadata where possible.
- Allow users to recategorize apps and create custom categories.
- Distinguish essential/productive apps from commonly distracting categories without moralizing.
- Handle newly installed and uninstalled apps.

**Open choice:** Entirely local heuristics versus remote categorization; local-first behavior is recommended for privacy and speed.

### 4.9 App grouping and profile modes — **Priority 2 confirmed**

**Purpose:** Adjust the home screen for different contexts while keeping it minimal.

- Create app groups such as Work, Personal, Reading, and Essentials.
- Switch between profiles, potentially with distinct visible apps, focus settings, limits, and appearance.
- Consider time-based profile changes (e.g., working hours or bedtime) after manual switching works well.
- Make switching and override controls discoverable.

**Important:** This feature was explicitly moved to Priority 2 in earlier discussion. Do not let advanced grouping block the core launcher release.

### 4.10 Insights and encouragement — Priority 2 recommended

**Purpose:** Show progress in a nonjudgmental way.

Examples discussed:

- “This week, you reduced late-night usage by 22%.”
- “You've been screen-free for 2 hours.”

Additional proposed views:

- Daily/weekly use, over-limit minutes, timeboxes completed, and focus-session summaries.
- Long-term patterns such as late-night use and most distracting categories.
- Estimated opportunity-cost time value, labeled clearly as an illustrative figure.
- Encouraging trend comparisons with sufficient baseline data.

**Measurement note:** Do not show a percentage improvement without a valid and comparable earlier period. Define “screen-free” precisely (for example, absence of unlocked/foreground usage) and avoid implying comprehensive detection when Android data is incomplete.

### 4.11 Onboarding walkthrough — **Later, before launch confirmed**

**Purpose:** Teach the product without making initial activation exhausting.

- Explain launcher choice, Usage Access, optional overlay and notification permissions.
- Configure allowances and optional hourly value.
- Demonstrate the home screen, drawer, timebox, focus, and intervention behavior.
- Offer a short interactive tutorial and contextual tips.
- Provide an always-accessible guide and easy settings recovery.

**Priority note:** Detailed onboarding can be implemented toward launch, but permission architecture and setup entry points need to be designed into the foundation early.

### 4.12 Free digital-wellbeing questionnaire — Marketing and acquisition experiment

**Purpose:** Help prospective users reflect on habits, learn about Yoga, and decide whether to try it.

- Public web questionnaire designed as a free, short self-assessment.
- Questions might cover compulsive checking, sleep disruption, concentration, perceived control, and time spent scrolling.
- Return a plain-language score or habit profile with personalized, practical suggestions.
- Offer an optional install link and explanation of how Yoga's features could help.
- Optional opt-in to receive a result or launch updates; do not presume consent.

**Safety and privacy:** Avoid presenting the questionnaire as a clinical diagnosis of “mobile addiction” without a validated instrument and suitable evidence. State what the score measures; minimize data collection; publish privacy terms; do not covertly collect sensitive psychological profiles for advertising. If using a validated scale, assess permissions, scoring, population suitability, and citation requirements.

**Open choices:** Questionnaire framework, number of questions, scoring method, landing page, analytics, and opt-in strategy.

## 5. Proposed screen and navigation map

| Screen or surface | Main contents | Key actions |
| --- | --- | --- |
| Welcome and activation | Product value, launcher preview, permission rationale | Continue, set default launcher, skip optional setup |
| Allowance setup | Daily limits, app/category selection, quiet hours | Save limits, edit exclusions |
| Value of time setup | Hourly amount, currency, example calculation | Save, skip, disable |
| Home | Clock, essential shortcuts, search, calm usage indicator | Launch, search, open drawer, start timebox |
| App drawer | Searchable app list, optional categories | Open app, favorite, categorize |
| Usage dashboard | Day/week totals, category split, allowance progress | Drill into app, change allowance |
| Focus session | Duration, blocked/deemphasized apps, exceptions | Start, pause, end, override |
| Timebox | Countdown and purpose | Start, pause, extend, finish |
| Mindful delay | App name, brief pause, optional purpose | Continue intentionally, go back |
| Excess-use indicator | Over-limit time and optional monetary equivalent | View details, snooze, dismiss |
| Insights | Trends, late-night changes, encouragement | Change date/range, view details |
| Categories and profiles | Groups, app assignments, mode presets | Create/edit/switch profile |
| Settings | Permissions, appearance, widgets, interventions, privacy | Configure, export/reset as appropriate |
| Help and onboarding replay | Tutorials, permission troubleshooting | Replay, fix permission |
| Public questionnaire | Habit questions and interpretation | Take test, view recommendations, optional install |

This is a proposed screen decomposition. Some items can be sheets, dialogs, or inline home-screen surfaces rather than full screens.

## 6. Interaction design and visual language

### Design direction already implied by the project

- Minimalist and adult-oriented; visually calm rather than playful or parental.
- Home screen focuses on essential actions and intentional access.
- Grayscale is a behavioral intervention, not necessarily the permanent brand palette.
- Floating feedback should remain small, contextual, and dismissible.
- Progress language should focus on improvements, not punishment.

### Recommended design system to validate

- **Color:** neutral monochrome base with a single subdued accent; a distinct but restrained state color for warnings.
- **Typography:** large readable clock and clear, high-contrast app labels; consistent hierarchy; no visual clutter.
- **Motion:** brief, purposeful animation for focus activation and timebox completion; reduced-motion support.
- **Components:** simple text-first app rows, compact progress rings/bars, bottom sheets for quick settings, and clear permission explanations.
- **Accessibility:** support font scaling, TalkBack, touch targets, color contrast, and non-color cues for grayscale and warning states.
- **Copy:** neutral wording such as “You're 12 minutes past today's limit” with optional “Equivalent time value: ₹100.”

### Interventions by intensity

| Level | Example behavior | User control |
| --- | --- | --- |
| 0 Awareness | Usage totals and quiet progress indicator | View or hide widget |
| 1 Gentle reminder | Approaching allowance notification | Dismiss or tune threshold |
| 2 Intentional friction | Pause before distracting app | Continue or return |
| 3 Visual de-emphasis | Grayscale or simplified launcher view | Snooze or disable where supported |
| 4 Focus context | Restricted home layout and quiet period | Exceptions and explicit override |

Avoid designing a hard lock as a default requirement. Any stronger blocking must be separately evaluated for Android feasibility, policy compliance, safety, and user trust.

## 7. Settings and configuration model

Suggested settings groups:

1. **Home and appearance:** favorites, app list, widgets, wallpaper, typography, colors.
2. **Usage rules:** tracked apps, daily allowances, reset time, excluded apps, alerts.
3. **Focus and timebox:** duration presets, schedules, delay length, override behavior.
4. **Visual interventions:** grayscale option, triggers, fallback instructions.
5. **Notifications:** quiet categories, digest behavior, essential exceptions.
6. **Value of time:** currency, hourly amount, calculation explanation, floating display, privacy.
7. **Categories and profiles:** app assignments and context-specific views (Priority 2).
8. **Privacy and permissions:** Usage Access, notification listener, overlays, permissions health and revocation.
9. **Help:** tutorial replay, battery optimization guidance if necessary, reset defaults, launcher exit.

## 8. Technical architecture outline

### Android application structure

- Kotlin with Jetpack Compose is a fitting implementation option, subject to the actual codebase and team preference.
- Launcher entry with Android HOME intent filters and role/default launcher selection handling.
- Home UI, searchable app inventory, favorites, and launcher settings.
- Usage data collection through Android-supported usage statistics and events, with explicit access.
- Local persistence for allowances, categorized apps, focus state, preferences, and aggregate history.
- Rule evaluator determining threshold state and next eligible intervention.
- Foreground/session lifecycle handling for timers and reliably surfaced reminders.
- Optional notification-listener/overlay modules kept separate from the core launcher.
- Local-first processing is preferable; no account or backend has been established as necessary.

### Conceptual data entities

| Entity | Example fields |
| --- | --- |
| UserPreferences | appearance, currency, hourlyValue, onboardingVersion |
| AppEntry | packageName, displayName, categoryId, favorite, essential |
| UsageRule | scope, targetIds, dailyAllowanceMinutes, thresholdSettings |
| UsageAggregate | date, packageName, eligibleMinutes, sourceQuality |
| FocusSession | startAt, endAt, mode, status, overrides |
| TimeboxSession | purpose, duration, startedAt, completedAt, extendedMinutes |
| InterventionEvent | type, trigger, shownAt, actionTaken |
| Profile | name, appSet, rules, appearance, schedule |

### Android feasibility checkpoints

1. Launcher behavior is available to a normal app after the user explicitly selects it as the default HOME app.
2. Usage statistics require user-granted Usage Access and are not an always-perfect real-time feed.
3. Device-wide grayscale and enforcement beyond the launcher are platform-dependent; prototype before committing to user-facing promises.
4. Notification manipulation and overlays each have distinct special access, limits, and privacy implications.
5. App launch interception is not guaranteed when apps open from notifications, widgets, recents, deep links, or other launchers.
6. Test OEM variations (Samsung, Motorola, Pixel, etc.), battery restrictions, Android versions, accessibility, and Play Store policy compliance.
7. Earlier discussions considered Android 15 as a minimum; **this is not finalized**. A high minimum API can sharply limit reach. Decide after reviewing supported features and target audience.

## 9. Privacy and trust requirements

- Explain precisely what usage data is accessed and why.
- Keep raw app-usage data on device by default unless a future feature genuinely requires upload.
- Never imply the hourly monetary value is the user's actual wage, income loss, or cash expenditure.
- Request optional permissions in context, only when the user enables the relevant feature.
- Make overlay, notification, and usage monitoring easy to disable.
- Allow deletion/reset of local usage history and preferences.
- Do not collect individual questionnaire answers for marketing profiling without clear, specific informed consent and a legitimate purpose.
- Design for users who may find cost-based reminders stressful: allow opt-out and gentler alternatives.

## 10. Recommended delivery roadmap

### Phase 0 — Feasibility spikes

- Validate OLauncher suitability and license.
- Prototype default launcher activation and core app drawer.
- Measure Usage Access accuracy and latency on several Android devices.
- Test possible grayscale, mindful-delay, notification, and overlay mechanisms.
- Confirm Play policy implications before implementing invasive intervention methods.

### Phase 1 — Useful core launcher

- Reliable home screen, app search, favorites, and settings.
- Basic usage dashboard and daily allowances.
- Essential-app exceptions and threshold reminders.
- Timebox timer.
- Initial optional value-of-time calculation, launcher-visible indicator.
- Basic focus session and deliberate delay where feasible.

### Phase 2 — Behavior and personalization

- Automatic categories and improved category-level rules.
- Grayscale workflow and notification quieting based on feasible mechanisms.
- App grouping and profile modes (**explicit Priority 2 decision**).
- Weekly insights and encouraging progress messages.
- Optional overlay-based cost indicator after usability testing.
- Widget support may need earlier groundwork depending on chosen launcher foundation.

### Phase 3 — Launch readiness

- Complete onboarding walkthrough and tutorial replay (**deferred until near launch**).
- Accessibility, battery, performance, OEM, and recovery testing.
- Clear Play listing, permission disclosures, privacy policy, and help documentation.
- Beta tests for retention, perceived pressure, and usefulness of the cost metaphor.
- Public questionnaire/landing-page acquisition experiment, with responsible scoring and consent.

## 11. Quality and success metrics

### Product health

- Launcher activation completion and retained default-launcher rate.
- Crash-free sessions, home-screen latency, widget reliability, and permission-error rate.
- Setup completion, permission opt-in, and successful fallback when permissions are denied.

### Behavior outcomes

- Change in eligible distracting-app minutes per day and late-night sessions.
- Percentage of days within selected allowances.
- Timebox starts, completions, and extensions.
- Focus session completion and deliberate-delay cancellation rate.
- Retention and voluntary feature adoption rather than merely number of interventions shown.

### Qualitative validation

- Whether the money indicator feels motivating, stressful, or gimmicky.
- Whether users understand how cost is calculated.
- Whether grayscale and delays change behavior or simply frustrate users.
- Whether the launcher remains practical enough to keep enabled.

## 12. Key open decisions

| Topic | Open decision | Suggested next action |
| --- | --- | --- |
| Brand | Yoga Launcher versus Yoga Phone as final public name | Test store-facing title and identity |
| Allowance | All screen time or selected apps/categories | User interviews and simple prototype |
| Value of time | Required setup versus optional; hourly value source | Keep optional and user-defined |
| Cost indicator | Overlay, notification, or launcher-only | Prototype and test annoyance level |
| Grayscale | Exact supported activation mechanism | Device and Android version feasibility spike |
| Focus enforcement | Soft friction versus stronger restrictions | Favor soft friction initially |
| Notification filtering | Quiet presentation versus listener/digest | Define permission and UX model |
| Categorization | Local heuristic and manual editing versus backend | Start local-first |
| Widgets | Scope and minimum support | Check chosen base launcher |
| Minimum Android | Android 15 versus broader compatibility | Reach and capability assessment |
| Monetization | Free, premium, subscription, or one-time | Not yet discussed as a decision |
| Acquisition test | Questionnaire scoring and consent model | Design and validate independently |

## 13. Implementation-ready backlog starter

Each work item should become a ticket with interaction details, states, permissions, testing criteria, and design assets.

### Foundation

- [ ] Evaluate OLauncher and select base architecture.
- [ ] Implement HOME role/default launcher activation and exit guidance.
- [ ] Build minimal home, favorite apps, app drawer, and app search.
- [ ] Establish app inventory refresh and package-change handling.
- [ ] Build settings architecture and local persistence.
- [ ] Define widget host behavior.

### Usage and interventions

- [ ] Build Usage Access explanation and permission status UI.
- [ ] Normalize daily/app usage and define eligible minute rules.
- [ ] Implement allowance editing, exclusions, and threshold evaluation.
- [ ] Build usage dashboard and near/over-limit feedback.
- [ ] Implement timebox lifecycle and completion prompts.
- [ ] Prototype deliberate launch delay and limitations.
- [ ] Prototype grayscale workflow and supported fallback.
- [ ] Prototype focus sessions and essential-app exceptions.
- [ ] Investigate notification quieting, consent, and digest options.

### Differentiation and refinement

- [ ] Build hourly value setup and cost-equivalent calculation.
- [ ] Design optional overlay/notification cost indicator and safety exclusions.
- [ ] Test metaphor and copy with prospective users.
- [ ] Add automatic categories and manual recategorization.
- [ ] Add grouping and profile modes in Priority 2.
- [ ] Build credible weekly insights and late-night reporting.

### Launch

- [ ] Complete onboarding walkthrough and tutorial replay.
- [ ] Audit accessibility, permissions, privacy, and Play policy.
- [ ] Test device matrix and long-running reliability.
- [ ] Create store listing, screenshots, support, and privacy content.
- [ ] Prototype free online questionnaire and scoring interpretation.
- [ ] Run controlled beta; revise interventions based on feedback.

## 14. Scope notes and provenance

This document consolidates the Yoga Launcher project discussions available through 5 November 2026: minimalist launcher identity, OLauncher basis, grayscale, focus mode, notification filtering, deliberate delays, automatic categories, widget support, timebox timer, app grouping/profile priority, onboarding priority, progress messaging, optional value-of-time setup, allowance-based monetary indicator, and a free online overuse questionnaire. It expands these into actionable product and design detail.

The exact designs, thresholds, data models, roadmap phase allocation, and acceptance criteria are recommendations where prior conversations did not establish a final decision. No implementation, screenshots, codebase, or finished UX designs were provided for verification in this consolidation.
