## Yoga Launcher — Detailed Functional Documentation

### 1. Home Screen
**Purpose:** The main minimalist interface promoting calm, intentional use and focus-driven interactions.

**Screens:**
- **Primary Launcher View:** Displays only essential apps as text entries with large readable typography.
- **Search Panel:** Type-to-search or swipe-up search for Intent-Based App Launch.
- **Quick Actions Bar:** Minimal set of contextual shortcuts like Focus Mode toggle, Timebox Timer, and Profile Switch.

**Options:**
- Add/remove favorite apps through long-press.
- Toggle greyscale or low-stimulation visuals.
- Choose between list or single-column layout.
- Adjust font size and theme tones for readability.
- Display Timebox Timer progress subtly on home screen.

**Tasks & To-Dos:**
- [ ] Develop text-only launcher UI with adaptive spacing.
- [ ] Add predictive AI for app suggestions based on frequency and context.
- [ ] Enable gesture controls (swipe up for search, swipe down for timer, long-press for focus toggle).
- [ ] Integrate smooth animations to maintain a calm user experience.

---

### 2. Focus & Detox Controls
**Purpose:** Tools to reduce distractions, limit impulsive use, and introduce mindfulness during phone use.

**Screens:**
- **Focus Mode Screen:** Configure daily focus sessions, auto-triggers, or quick manual activation.
- **Detox Configuration Page:** Define time caps for apps or app groups.
- **Notification Center Filter:** Manage which notifications are allowed, delayed, or batched.

**Options:**
- Enable/disable Focus Mode manually or schedule it.
- Configure deliberate delay before launching distracting apps (2–10 seconds).
- Create recurring detox periods (e.g., 8–10 PM daily).
- Select apps to allow during Focus Mode.
- Enable greyscale auto-switch when focus begins.

**Tasks & To-Dos:**
- [x] Implement deep link integration with Android DND.
- [ ] Build background service to monitor screen unlocks and enforce limits.
- [x] Add real-time delay and lock overlay. (Launcher-level pause; apps opened from notifications or other apps skip it.)
- [ ] Integrate AI-based trigger predictions (detect recurring distraction windows).

---

### 3. Profiles & App Grouping
**Purpose:** Enable users to separate digital lives (Work, Personal, Study, etc.) with profile-based app access and contextual rules.

**Screens:**
- **Profile Manager:** Create, edit, rename, or delete profiles.
- **Profile Switcher Overlay:** Slide-up interface for one-tap switching.
- **Policy Dashboard:** Configure group-level rules for all apps in a profile.

**Options:**
- Assign apps to profiles manually or let AI classify them using app metadata.
- Apply rules like time limits, focus restrictions, or greyscale filters per profile.
- Auto-switch profiles based on time, Wi-Fi, or GPS location.
- Temporarily lock non-active profiles.

**Tasks & To-Dos:**
- [ ] Implement SQLite schema for profile-based app storage.
- [ ] Build ML-based categorization model (TF Lite) to tag new apps.
- [ ] Develop contextual trigger service (Wi-Fi SSID, GPS geofencing).
- [ ] Create visual indicator (color bar or label) showing active profile.

---

### 4. AI Insights & Coaching
**Purpose:** Turn behavioral data into meaningful stories that motivate users.

**Screens:**
- **Insights Feed:** Scrollable daily and weekly summary cards.
- **Coaching Overlay:** Chat-like interaction with micro feedback and encouragement.
- **Insight Notification Panel:** Surface contextual nudges throughout the day.

**Options:**
- View NLP-based summaries (e.g., “This week, you reduced late-night usage by 22%”).
- Enable reflection prompts after focus sessions (“How do you feel after 2 hours screen-free?”).
- Enable/disable AI coaching notifications.
- Customize coaching tone (gentle / data-driven / motivational).

**Tasks & To-Dos:**
- [ ] Create local analytics pipeline to record app usage and unlocks.
- [ ] Integrate lightweight on-device NLP summarizer.
- [ ] Build templating system for generating daily insight cards.
- [ ] Add micro-interactions for reflection prompts.

---

### 5. Motivation & Reinforcement
**Purpose:** Sustain user engagement through positive reinforcement and measurable progress.

**Screens:**
- **Streak Tracker:** Shows number of consistent focus days.
- **Milestone Page:** Celebrates achievements visually.
- **Focus Points Scoreboard:** Shows accumulated points and ranks.
- **Timebox Timer Interface:** Countdown timer for focused sessions with gentle alarms.

**Options:**
- Configure focus durations (Pomodoro-style presets: 25/45/60 minutes).
- Enable optional background sound during sessions.
- Turn off/on celebration animations.
- View streak history and motivational quotes.

**Tasks & To-Dos:**
- [ ] Implement time-tracking database.
- [ ] Integrate gamified scoring system for completed focus sessions.
- [ ] Design celebration overlay animations.
- [ ] Sync focus points with dashboard.

---

### 6. Dashboard & Analytics
**Purpose:** Give a holistic view of user’s focus, screen-time reduction, and performance trends.

**Screens:**
- **Overview Dashboard:** Displays unlocks, screen time, top apps, and goals.
- **Trend Charts:** Weekly and monthly heatmaps.
- **Goal Summary:** Highlights progress and AI-adjusted goals.

**Options:**
- Set and edit daily/weekly focus goals.
- Compare usage with past performance.
- Enable voice summary (AI reads highlights).
- Export anonymized data (premium feature).

**Tasks & To-Dos:**
- [ ] Design responsive dashboard cards.
- [ ] Build AI-based adaptive goal engine.
- [ ] Implement charting via lightweight library (MPAndroidChart or Compose).
- [ ] Connect backend analytics module to AI summarizer.

---

### 7. Onboarding Flow
**Purpose:** Smooth initial setup through intelligent, guided walkthrough.

**Screens:**
- **Welcome Page:** Introduces Yoga Launcher philosophy.
- **Permissions Page:** Requests required Android permissions (notifications, usage stats).
- **AI Personalization Preview:** Shows preselected essential apps and allows user edits.
- **Tutorial Slide:** Demonstrates focus mode, timer, and gestures.

**Options:**
- Accept or adjust AI’s home app suggestions.
- Choose between Light/Dark/Greyscale themes.
- Enable first Focus Challenge (introductory streak goal).

**Tasks & To-Dos:**
- [ ] Build Compose-based onboarding screens.
- [ ] Connect onboarding preferences to launcher database.
- [ ] Add optional skip mode for returning users.
- [ ] Integrate AI onboarding insights for personalization.

---

### 8. Settings & Privacy
**Purpose:** Provide transparency and control while ensuring data remains local.

**Screens:**
- **General Settings:** Theme, layout, notifications.
- **Privacy Dashboard:** View stored analytics data.
- **Backup/Restore Panel:** Manage preferences backup.

**Options:**
- Toggle AI features (analytics, suggestions).
- Manage local data retention period.
- Enable Family Mode for shared devices.
- Export preferences securely.

**Tasks & To-Dos:**
- [ ] Implement encrypted local data store.
- [ ] Build simple UI for AI toggles.
- [ ] Add restore-from-cloud option using Google Drive API.

---

### 9. Monetization & Expansion
**Purpose:** Build sustainable value for power users.

**Screens:**
- **Premium Upgrade Page:** Highlights unlocked features.
- **Add-on Marketplace (Future):** Curated expansion packs like wallpapers, AI voices.

**Options:**
- Purchase one-time or recurring subscription.
- Unlock AI insights, dashboard export, and customization features.
- Enable beta features early access.

**Tasks & To-Dos:**
- [ ] Add billing integration (Google Play Billing Library v6).
- [ ] Implement feature flag system.
- [ ] Create promotional banners within settings.

---

**Design Vision:**  
> Yoga Launcher merges AI-driven awareness with minimalist philosophy, blending design calmness and smart behavior insights. Every element—from onboarding to focus streaks—is designed to make mindfulness effortless while preserving user privacy.

