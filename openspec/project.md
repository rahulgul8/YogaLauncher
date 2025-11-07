# Project Context

## Purpose
Yoga Launcher is a mindful, AI-enhanced Android launcher designed to help users reduce distractions, build healthier digital habits, and embrace intentional phone use. It combines minimalist design principles with intelligent automation, offering a seamless balance between simplicity and smart personalization.

**Core Philosophy:** A launcher that respects your attention.

**Key Goals:**
- Promote calm, intentional phone use through minimalist UI
- Reduce screen time and distracting app usage
- Provide AI-driven insights and coaching for digital wellbeing
- Maintain 100% on-device privacy (no data leaves the device)
- Create a focus-driven, distraction-free mobile experience

## Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (declarative UI)
- **Design System:** Material Design 3
- **Android SDK:** Min SDK 35, Target SDK 36, Compile SDK 36
- **JVM Target:** Java 11
- **AI/ML:** TensorFlow Lite (on-device NLP and behavior analysis)
- **Local Storage:** Room/SQLite for persistent data
- **Analytics:** Custom local event tracking (privacy-first)
- **Testing:** JUnit, Espresso, Compose UI Testing
- **Build System:** Gradle with Kotlin DSL
- **Version Control:** Git

**Planned Integrations:**
- Google Play Billing Library v6 (for premium features)
- TensorFlow Lite models for app categorization and usage predictions
- Android DND (Do Not Disturb) API
- UsageStatsManager API for app usage tracking
- WorkManager for background tasks

## Project Conventions

### Code Style
- **Package Structure:** `in.tuwi.yogaphone`
- **Kotlin Conventions:** Follow official Kotlin coding conventions
- **Compose Guidelines:** Use composable functions with clear, descriptive names
- **Naming:**
  - Activities: `*Activity` (e.g., `MainActivity`)
  - Composables: PascalCase descriptive names (e.g., `Greeting`, `HomeScreen`)
  - ViewModels: `*ViewModel` (e.g., `MainViewModel`)
  - Data models: `*Model` (e.g., `AppModel`)
- **File Organization:** Group by feature/module (ui, data, helper, listener)

### Architecture Patterns
- **UI Architecture:** MVVM (Model-View-ViewModel) with Jetpack Compose
- **State Management:** ViewModel + Compose State
- **Data Layer:** Repository pattern with Room/SQLite
- **Modular Design:** Separate concerns into packages (ui, data, helper, listener)
- **Edge-to-Edge UI:** Modern Android UI with system bar integration
- **Privacy-First:** All processing and storage must remain on-device
- **AI Integration:** On-device ML models only (no cloud-based AI)

### Testing Strategy
- **Unit Tests:** JUnit for business logic and ViewModels
- **Instrumented Tests:** Espresso and Compose UI tests for Android components
- **Test Location:** Tests mirror main source structure
- **Coverage Goals:** Focus on critical paths (focus mode, app blocking, usage tracking)
- **CI/CD:** TBD (consider GitHub Actions for Android builds)

### Git Workflow
- **Branching:** Feature branches from main (e.g., `feature/focus-mode`, `fix/timer-bug`)
- **Commits:** Descriptive commit messages explaining the "why"
- **Main Branch:** `main` (stable, production-ready code)
- **Pull Requests:** Code review before merging to main
- **Version Tags:** Semantic versioning (v1.0.0, v1.1.0, etc.)

## Domain Context

### Digital Wellbeing & Mindfulness
- **Focus Mode:** Temporary app blocking with deliberate delays (2-10 seconds)
- **Profiles:** Work, Personal, Study contexts with separate app groups and policies
- **Detox Tools:** Time caps, notification filtering/batching, greyscale mode
- **Timebox Timer:** Pomodoro-style focus sessions with gentle alarms
- **Streaks & Milestones:** Gamified positive reinforcement without addiction

### AI & Personalization
- **Behavioral Analysis:** Track app usage patterns, unlock frequency, session duration
- **Natural Language Summaries:** "This week, you reduced late-night usage by 22%"
- **Adaptive Goals:** AI adjusts focus goals based on user progress
- **App Classification:** Automatic categorization using TensorFlow Lite
- **Context Detection:** Time, location (Wi-Fi SSID, GPS), and usage-based triggers

### Privacy & Trust
- **100% On-Device:** All AI processing, analytics, and data storage is local
- **No Telemetry:** No usage data sent to servers or third parties
- **Transparency:** Users can view, export, or delete all stored data
- **Optional Backup:** Secure local backup via Google Drive API (user-controlled)

### User Experience Principles
- **Minimalism:** Text-only launcher with essential apps only
- **Calm Design:** Gentle animations, optional greyscale, reduced visual stimulation
- **Intentional Friction:** Deliberate delays before opening distracting apps
- **Positive Reinforcement:** Celebration of milestones, not punishment for failures

## Important Constraints

### Technical Constraints
- **On-Device Only:** All AI models and data processing must run locally (no cloud APIs)
- **Privacy Requirement:** Zero data transmission to external servers
- **Performance:** AI models must be lightweight (TensorFlow Lite, < 10MB)
- **Battery Efficiency:** Background services must be optimized for minimal battery drain
- **Modern Android Only:** Min SDK 35 (Android 15+) to leverage latest privacy/security features

### Business Constraints
- **Freemium Model:** Core features free, premium features via one-time or subscription
- **Premium Features:** Full analytics, AI insights, customization, early access
- **No Ads:** Ad-free experience (monetization via premium only)

### Regulatory/Privacy Constraints
- **GDPR Compliance:** Even though on-device, support data export and deletion
- **Android Permissions:** Request only essential permissions (Usage Stats, Notifications, Accessibility)
- **Transparency:** Clear explanations for each permission request during onboarding

## External Dependencies
- **Android System APIs:**
  - UsageStatsManager (app usage tracking)
  - NotificationListenerService (notification filtering)
  - PackageManager (installed apps list)
  - DeviceAdminReceiver (screen lock functionality)
  - AccessibilityService (app launch detection)
  - Wi-Fi & Location APIs (context detection)

- **Google Play Services:**
  - Google Play Billing Library v6 (in-app purchases)
  - Google Drive API (optional backup/restore)

- **Third-Party Libraries (Planned):**
  - TensorFlow Lite (on-device ML)
  - MPAndroidChart or Compose Charts (analytics visualization)
  - WorkManager (background task scheduling)
  - Accompanist (Compose utilities)

- **Reference Implementation:**
  - OLauncher codebase (in `reference/` directory) used as architectural reference
