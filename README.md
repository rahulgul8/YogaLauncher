# Yoga Launcher

Yoga Launcher is a mindful, AI-enhanced Android launcher designed to help users reduce distractions, build healthier digital habits, and embrace intentional phone use. It combines minimalist design principles with intelligent automation, offering a seamless balance between simplicity and smart personalization.

---

## 📍 Status (November 2026)

Built and on `main`: minimalist home and drawer, profiles and categories, usage allowances with reminders and the optional value-of-time indicator, timebox, focus sessions and schedules, the mindful launch pause, greyscale and quiet-notification interventions, insights, first-run setup, privacy controls and backup. Everything runs on the device with Room, DataStore and WorkManager; the AI/ML items below are deferred in favour of local rules and templates. See `ROADMAP.md` for the piece-by-piece log and `docs/launch/` for store and privacy drafts. A prototype of the free phone-habits check-in lives in `web/questionnaire/`.

## 🌿 Overview
Yoga Launcher transforms your home screen into a calm, focus-driven space. It uses AI to understand your habits, personalize layouts, and provide meaningful insights — helping you stay focused without giving up functionality.

**Core Philosophy:**  
> A launcher that respects your attention.

---

## ✨ Key Features

### 🧘‍♂️ 1. Minimalist Home Screen
- Clean text-only interface with essential apps only.
- Quick search for Intent-Based App Launch.
- Auto-populated home apps based on AI usage analysis.
- Optional greyscale mode for reduced stimulation.

### 🔒 2. Focus & Detox Tools
- Focus Mode with app blocking and time-based lockouts.
- Notification filtering and batching to reduce interruptions.
- Deliberate delay (2–10 seconds) before opening distracting apps.
- Greyscale auto-toggle during Focus sessions.

### 👥 3. Profiles & App Grouping
- Separate *Work*, *Personal*, *Study* profiles.
- Policy-based app management (time limits, visibility, rules).
- AI-driven automatic app classification.
- Smart context switching based on time, Wi-Fi, or location.

### 🧠 4. AI Insights & Coaching
- Natural-language summaries like “This week, you reduced late-night usage by 22%.”
- Reflection prompts and mindful check-ins.
- Detection of repetitive app-switching patterns.
- Adaptive goal suggestions and personal focus windows.

### 🔔 5. Motivation & Reinforcement
- Daily streak tracker and milestone celebrations.
- Focus Points and scoreboard system.
- Built-in Timebox Timer for Pomodoro-style sessions.
- Positive reinforcement and calm animations.

### 📊 6. Dashboard & Analytics
- Daily and weekly usage graphs.
- Unlock frequency and app time summaries.
- AI-adjusted focus goals.
- Visual and voice-based summaries.

### 🚀 7. Smart Onboarding
- Guided walkthrough to introduce mindful use.
- AI-suggested initial layout with essential apps.
- Customizable themes (Light, Dark, Greyscale).

### ⚙️ 8. Privacy & Settings
- 100% on-device analytics and AI processing.
- Transparent data control and retention management.
- Backup and restore via secure local storage.
- Optional Family Mode for shared devices.

### 💎 9. Premium Experience
- Unlock full analytics, AI insights, and customization.
- One-time or subscription-based access.
- Early access to experimental AI features.

---

## 🧩 Tech Stack
- **Frontend:** Android (Kotlin, Jetpack Compose)
- **AI Engine:** TensorFlow Lite, on-device NLP models
- **Data:** Room/SQLite for local storage
- **Analytics:** Custom local event tracker (privacy-first)
- **UI Frameworks:** Material Design 3, MotionLayout

---

## 🔧 Installation (Developer Build)
1. Clone the repository:
   ```bash
   git clone https://github.com/yogalauncher/android.git
   cd yogalauncher
   ```
2. Open in **Android Studio** (Arctic Fox+).
3. Sync Gradle and build.
4. Install the debug APK:
   ```bash
   ./gradlew installDebug
   ```

---

## 📱 Usage
1. Set Yoga Launcher as your default launcher.
2. Follow the onboarding walkthrough.
3. Personalize your profiles and focus rules.
4. Enjoy a mindful, distraction-free phone experience.

---

## 🔐 Privacy Commitment
Yoga Launcher never sends personal data to any server.  
All AI computations, analytics, and preferences remain **fully on-device**.

---

## 🗺️ Roadmap
- [ ] Widgets and contextual quick actions.
- [ ] AI voice companion.
- [ ] Dynamic wallpapers based on mood/time.
- [ ] Cloud sync for preferences.

---

## 🤝 Contributing
Pull requests and suggestions are welcome! Please open an issue first to discuss changes.

---

## 🧘 License
MIT License © 2025 Yoga Launcher Team