# Competitor study: minimalist launchers and screen-time apps (Android)

Researched 7 November 2026 for Yoga Phone. Play Store data (installs, ratings, in-app purchase ranges, update dates) was read directly from the Google Play listing pages for India (`hl=en_IN&gl=IN`) and the US (`hl=en_US&gl=US`) on that date. Exact plan prices are not shown on Play pages; the "IAP range" is Play's own "₹X – ₹Y per item" line, which covers every product the app sells (plans, offers, tips). Exact plan prices come from developer pages, the App Store, or reviews, and are marked with their source and date. Anything not confirmed is marked **(uncertain)**.

Ratings differ by country on Play; the rating count is global. Where two ratings are shown, they are India / US.

See also `docs/research/regain.md` (full Regain study) and `docs/launch/pricing.md`.

## Comparison table

| App | Model | Free tier | Monthly | Yearly | Lifetime / one-time | Trial | Play IAP range (IN / US) |
|---|---|---|---|---|---|---|---|
| minimalist phone (Focus Experts) | Subscription or one-time, paywall after trial | Effectively none: features shrink after a few days unless a plan or trial starts (user reviews, German review) | US$4.99 (TechRadar) | US$21.43 promo (TechRadar); iOS US$19.99 | ~US$29.99 (TechRadar); iOS US$59.99 | 7 days, on all three plans | ₹24.90–₹10,100 / US$0.29–89.99 |
| Olauncher (Digital Minimalism) | Free, GPLv3, donations | Everything | – | – | – | – | none |
| Pro Launcher (same developer as Olauncher) | Paid sibling app, subscription **(model uncertain)** | – (paid features only) | **(uncertain)** | **(uncertain)** | **(uncertain)** | **(uncertain)** | ₹499–₹1,999 / US$9.99–24.99 |
| Niagara Launcher (Mellowdrop Studio) | Freemium; Pro yearly subscription or lifetime | Full launcher, ad-free | Help centre mentions monthly/yearly; monthly price not found | ₹140 (gogi.in 2026); ~US$13.99 (2026 guides, Play review says "$14/yr") | ₹420 (gogi.in); ~US$42.99 (Play review "$42") | Not published | ₹15–₹420 / US$0.99–74.99 |
| Before Launcher (Before Labs) | Freemium; one-time Pro, "no subscriptions, ever" | Launcher, notification filter, themes | – | – | ₹180–₹490 / US$1.99–6.99 range; Gizmodo cites ~US$6 **(tier split uncertain)** | None found | ₹180–₹490 / US$1.99–6.99 |
| Blank Spaces (Ecstasis LLC) | Subscription + lifetime, **iOS only** | Little; trial then subscribe | US$3.99 (also US$1.99/week) | US$17.99 | US$23.99 | 7 days | Not on Google Play (no Android listing found) |
| UnLauncher (100 GB Labs) | Freemium; one-time Pro | Text launcher, hide/rename | – | – | ₹330–₹999 / US$3.99–12.99 **(which is the base price uncertain)** | 7 days of Pro | ₹330–₹999 / US$3.99–12.99 |
| Indistract Minimalist Launcher | Freemium; one-time Pro | Most features | – | – | ₹70–₹280 / US$0.99–9.99; Gizmodo cites US$10 | None found | ₹70–₹280 / US$0.99–9.99 |
| KISS Launcher | Free, GPLv3 | Everything | – | – | – | – | none |
| Opal (Opal: The Focus Company) | Freemium subscription + lifetime | 1 rule, basic blocking, today's score | US$19.99 (iOS/web, Sep 2026) | US$99.99 (iOS/web) | US$399 (iOS/web) | 3 days monthly, 1 week yearly | ₹199–₹8,600 / US$2.99–79.99 (Android prices are lower than iOS; US$399 lifetime is not in the Android range) |
| ScreenZen | Free, optional tips | Everything | – | – | Tips only | – | ₹490–₹2,050 / US$4.99–19.99 (tips); iOS tips US$7/14/25 |
| one sec (riedel.wtf) | Freemium subscription + lifetime | 1 app only | US$3.99 family plan (iOS) | US$19.99 (iOS); €14.99 DE; family US$29.99 | US$99.99 (iOS); family US$149 | 1 week | ₹85–₹9,999 / US$3.99–99.99 |
| Regain (EpowerX Labs) | Freemium subscription ("Regain Pro"), no lifetime | Focus timer, blocking, insights | ₹199 (often discounted to ₹84) | ₹999 (often discounted to ₹299) | none | Not published | ₹49–₹1,999 / US$2.09–150.14 |

Notes:
- Regain ₹199/month and ₹999/year come from Regain's Android refund-policy page as recorded in `docs/research/regain.md`. On 7 Nov 2026 that page returned 404, so today's figures are **not re-confirmed**; the Play India IAP range (₹49–₹1,999) is consistent with them.
- Play IAP ranges include promotional and tip products, so the extremes (for example minimalist phone's ₹10,100, Regain's US$150.14) are probably family, lifetime or win-back offers **(uncertain)**.

## Play Store snapshot (7 Nov 2026)

| App | Package | Developer | Installs | Rating (IN / US) | Ratings | Last update |
|---|---|---|---|---|---|---|
| minimalist phone: Launcher | com.qqlabs.minimalistlauncher | Focus Experts (Estonia; iOS seller is Hachi Media OÜ) | 5M+ (AppGoblin: ~8.6M) | hidden in IN / 4.7 | 51.6K | 13 Oct 2026 |
| Olauncher. Minimal AF Launcher | app.olauncher | Digital Minimalism (Tanuj) | 1M+ | 4.8 / 4.8 | 84.6K | 2 Sep 2026 |
| Pro Launcher. Productive You. | app.prolauncher | Digital Minimalism | 100K+ | not shown | – | 29 Oct 2026 |
| Niagara Launcher | bitpit.launcher | Mellowdrop Studio | 10M+ | 4.7 / 4.8 | 261K | 5 Nov 2026 |
| Before Launcher - Go Minimal | com.beforesoft.launcher | Before Labs | 500K+ | 4.7 / 4.8 | 13.4K | 31 Oct 2026 |
| UnLauncher. Minimalist Home | com.hundredgb.unlauncher | 100 GB Labs | 10K+ | 4.4 / 4.3 | 306 | 26 Oct 2026 |
| Indistract Minimalist Launcher | com.indistractablelauncher.android | Indistractable (Devansh Gulhane, Bengaluru) | 1M+ | not shown | – | 28 Oct 2024 (stale) |
| KISS Launcher | fr.neamar.kiss | Neamar | 500K+ | 4.3 | 4.7K | 24 Jul 2026 |
| Opal Screen Time Limit & Timer | com.withopal.opal | Opal: The Focus Company | 500K+ | 4.5 / 4.6 | 11.8K | 27 Oct 2026 |
| ScreenZen | com.screenzen | screenzen | 1M+ | 4.7 / 4.8 | 35.8K | 19 Sep 2026 |
| one sec | wtf.riedel.onesec | riedel.wtf | 1M+ | 4.5 / 4.8 | 46.5K | 26 Oct 2026 |
| Regain: Study Timer for Focus | ai.regainapp | EpowerX Labs (India) | 5M+ (AppGoblin: ~6.1M) | 4.4 / 4.5 | 228K | 7 Nov 2026 |

None of these shows ads in its Play listing.

## 1. minimalist phone (com.qqlabs.minimalistlauncher)

Formerly published as QQ Labs / QQ42 Labs; the Play developer is now "Focus Experts". Separate iOS app by Hachi Media OÜ.

**Features**
- Icon-free, text-only home screen with favourites; everything else via search.
- App blocker and website blocker.
- Blocks short-video sections (YouTube Shorts, Instagram/Facebook Reels, Snapchat).
- Blocking schedules (work, study, sleep; can act as a Pomodoro timer).
- Session limits and mindful launch delays; in-app reminders with a progress bar.
- Notification filter that hides unimportant notifications.
- Themes, fonts, app renaming, app hiding; grayscale/monochrome mode.
- Work-profile support. Uses Accessibility for reminders and blocking.

**What's paid**
- Monthly, annual or one-time plan, each with a free trial (7 days per TechRadar and a German review). Reviews say there is no meaningful free version: after a couple of days quick access to favourites and interruptions are removed unless you start a trial.
- Prices: TechRadar quotes US$4.99/month, US$21.43 for 12 months (promotional) and about US$29.99 one-time. iOS US store shows US$19.99/year and US$59.99 lifetime (CA store: lifetime C$99.99). Play India IAP range ₹24.90–₹10,100. Exact INR plan prices **not found**.

**Praise**
- People say it sharply cuts screen time ("instantly halved" usage, per one review) and works because it is boring.
- Blockers and timers work even when the launcher itself isn't the default.

**Complaints**
- Paywall pushed too early and features removed step by step until you pay.
- Price considered high next to one-time launchers; an r/minimalist_phone thread is titled "I have to pay now?".
- TechRadar's reviewer found the phone slower and harder to use, and missed meetings.
- Lifetime buyers worry lifetime access may not survive future changes.

## 2. Olauncher (app.olauncher)

**Features**
- Text-only home screen with up to 8 apps; app drawer by search with auto-launch when one match remains.
- Rename and hide apps, text size, alignment, status bar toggle.
- Gestures: double-tap to lock, swipe left/right to open chosen apps, swipe down for notifications.
- A new wallpaper every day; dark and light themes.
- Dual apps and work profile support. No data collection.

**What's paid**
- Nothing. GPLv3 open source, also on F-Droid. Donations via Buy Me a Coffee, GitHub Sponsors and PayPal.
- The same developer sells **Pro Launcher** (app.prolauncher, 100K+ installs, IAP ₹499–₹1,999 / US$9.99–24.99). Pro features: "Interrupt" (gentle prompt when opening time- or money-wasting apps), a page of widgets plus weather, app categories/folders, Classic and AMOLED daily wallpapers, fonts, notes and tasks, short-video blocking, a members' Telegram group. Appears to be a subscription (the developer mentions re-subscription) **(plan prices uncertain)**.

**Praise**
- Clean, elegant, genuinely free and ad-free; users cite reduced screen time and more reading. Endorsed by DHH and several "best launcher" lists.
- Friction of typing names breaks the pull of app logos.

**Complaints**
- Occasional lag when opening apps; gesture navigation issues on some phones (blamed on manufacturers).
- Missing weather/calendar widgets; duplicate app names are hard to tell apart without icons.

## 3. Niagara Launcher (bitpit.launcher)

Six-person indie team; the largest launcher here (10M+ installs).

**Features**
- One-handed vertical app list with the "wave alphabet" scroller.
- Integrated notifications (read and reply from home), notification summary that bundles less important notifications.
- Pop-up folders, widget stack (up to four), calendar and weather widgets.
- Themes, icon packs, built-in wallpapers, clock styles, custom fonts, Material You, light/dark.
- Gestures (double-tap to lock), hide apps, advanced search (contacts, web, calculator).
- Usage Breaker: gentle reminders for apps you use too much.
- Backup and restore. Ad-free even in the free version.

**What's paid (Niagara Pro)**
- Pro-only: curated and community themes, icon packs (Anycons, Niagara Dots), Icon Assistant, Usage Breaker, calendar and weather widgets, widget stacks, pop-ups, custom fonts and clock, double-tap to lock, hiding the status bar, music-app integration.
- Billing: yearly subscription or one-time "Pro Lifetime". Prices are localized and raised every 2–3 years for new users; existing subscriptions keep their price.
- India: ₹140/year and ₹420 lifetime (gogi.in, 2026). Play India IAP range ₹15–₹420 matches. History: ₹100/₹250 before Dec 2022, then ₹120/₹350.
- US: about US$13.99/year and US$42.99 lifetime in 2026 (a US Play review cites "$14/yr" and "$42"). Play US range tops out at US$74.99 **(purpose of that product uncertain)**.
- No trial published.

**Praise**
- Polished, fast, well optimised; music apps surface automatically in the car; the yearly price feels small.
- Frequently recommended by Android Police, How-To Geek, Tom's Guide.

**Complaints**
- Basic things like weather on the home screen and custom fonts are behind Pro; some call it "egregious" for basic features.
- Finding rarely used apps without icons is hard; not a cure for addiction on its own.
- Community themes shared via Discord; some layout limits (widget placement, right alignment).

## 4. Before Launcher (com.beforesoft.launcher)

**Features**
- Minimal home screen of favourite apps, plus a scrollable, sortable, searchable app list.
- Notification filter: less important notifications arrive silently and stay out of the status bar, one swipe away.
- Folders, pinned apps, hidden apps (Pro), labels/renaming, home text size.
- Photo, gradient and solid themes or your own; icon styles and third-party icon packs.
- Sort apps by size, install date and last use to clean up.
- Work profiles, backup and restore, double-tap to lock (Accessibility, optional), opt-out analytics.
- Inspired by Digital Minimalism, Indistractable and the Light Phone; Fast Company best new apps 2019.

**What's paid**
- One-time Pro, "lifetime purchase for all of your devices"; listing promises no ads and no subscriptions ever.
- Play IAP ₹180–₹490 / US$1.99–6.99; Gizmodo cites about US$6. Probably a base price plus higher optional tiers **(uncertain)**. Hide apps is Pro-only.

**Praise**
- Simple, calm, good for ADHD focus; notification filter is unique and valued; fast developer support.

**Complaints**
- Some Pro buyers say it is "not worth it": theme creator broken, only one gesture, no drawer font size.
- Requests for per-app usage time shown next to app names.
- Stray UI glitches (dots on home screen); wallpaper scrolls between tabs.

## 5. Blank Spaces (Ecstasis LLC)

**iOS only.** No Google Play listing was found (a Play search for "blank spaces launcher" shows other apps; AlternativeTo claims Android **(unverified, likely outdated)**). Included because it sets price expectations.

**Features**
- "Dumbphone mode": only chosen apps shown, text-based home screen with widgets.
- App blocking unlocked by a breathing exercise, going outside, or doing push-ups.
- Screen time reduction focus; also on iPad and Vision Pro.

**What's paid**
- US App Store: US$1.99/week, US$3.99/month, US$17.99/year, US$23.99 lifetime; 7-day free trial, then subscription required.

**Praise / complaints**
- Not researched in depth because it is not on Android. Typical iOS feedback praises the calm design; the main complaint pattern across this category is the paywall after the trial.

## 6. Indistractable, UnLauncher and KISS (brief)

**Indistract Minimalist Launcher** (com.indistractablelauncher.android, 1M+)
- Light Phone-inspired: search bar home, light/dark with auto switch, at-a-glance screen time and weather, clock and calendar shortcuts, battery %, custom fonts, hide and rename apps, built-in daily tasks.
- Promised but not shipped: intention setting, batched notifications, folders.
- Pro one-time: ₹70–₹280 / US$0.99–9.99 (Gizmodo: US$10). Still labelled beta; **last updated Oct 2024**, so likely abandoned.

**UnLauncher** (two apps share the name)
- *UnLauncher. Minimalist Home* by 100 GB Labs (com.hundredgb.unlauncher, 10K+): text-only home, no icons, badges or widgets; subtle usage stats; themes, fonts, layout and colour modes in Pro. 7-day Pro trial, then free version or one-time Pro (₹330–₹999 / US$3.99–12.99). Users praise how much is free; complaint: double-tap lock uses device admin, which forces PIN instead of fingerprint.
- *Unlauncher* by jkuester (open source, F-Droid; fork of Slim Launcher): free. Slim Launcher itself (com.sduduzog.slimlauncher) has 100K+ installs, 4.4★.

**KISS Launcher** (fr.neamar.kiss, 500K+, 4.3★)
- Search-first: type a few letters to launch; learns your habits; 250 KB, never goes online; GPLv3, free.

## 7. LessPhone and Light Phone-style launchers

- **LessPhone** (Aswin Mohan): older Android launcher (MobileSyrup review 2019) with limited home slots, no full app drawer, and paid extras like dark theme (about US$1.39). It **no longer appears on Google Play India** (package not found, not in search) **(status uncertain)**. An unrelated iOS app now uses the name.
- **Blank Launcher: Digital Detox** (com.blanklauncher.app, 1K+, new): pure-black OLED screen, big clock, up to 6 favourites free, hide/rename. One-time Pro ₹330 adds a per-app countdown or question before opening, scheduled focus blocks, and screen time with streaks and week-on-week comparison. Positions itself against subscriptions and Accessibility permissions.
- **Light Phone Launcher** (com.light.phone.launcher, MobIon): 500+ installs, not notable.
- Open-source dumbphone-style launchers worth knowing: YAM Launcher, Minimo, Kolibri (Android 16 only), Escape Launcher. All free.

## 8. Opal (com.withopal.opal)

**Features**
- Focus sessions that block chosen apps; recurring schedules; daily app limits.
- Pomodoro timer, soundscapes, weekly reports, Opal Score, focus streaks.
- Hard Mode (can't end a session early), Allow Only list, breaks during sessions.
- Parental control; on-device data. Also on iPhone and Mac. Uses Accessibility on Android 14+.

**What's paid**
- Free: blocking, **one rule**, basic timers, today's score only. Pro: unlimited rules, Hard Mode, Allow Only, advanced timers, score history and analytics.
- iOS/web (Sep 2026): US$19.99/month (3-day trial), US$99.99/year (1-week trial), US$399 lifetime; students up to 50% off; weekly offers at US$4.99–9.99. iOS India shows plans from ₹59/week to ₹1,149/year.
- Android Play range ₹199–₹8,600 / US$2.99–79.99, so Android plans are cheaper than iOS and the US$399 lifetime isn't sold there **(exact Android plans uncertain)**.
- Android free tier used to show ads to take a break; removed June 2026 (Autonomous review).

**Praise**
- Most polished app in the category; strong analytics; press coverage (New Yorker, NYT).

**Complaints**
- Price is the top complaint (competitors market "Opal without the $100/year").
- Updates moved existing multiple rules behind the paywall (free limited to 3 rules at the time, now 1).
- Easy to bypass on Android (force-stop); forced re-login bug; recategorises apps; a persistent on-screen bubble.

## 9. ScreenZen (com.screenzen)

**Features**
- Pause before opening apps or sites, with a wait that grows with each open.
- Interrupt scrolling: allow only short bursts; limited opens per day with cooldowns.
- Daily usage goals with optional blocking; per-app, per-day and per-hour settings.
- Custom prompts such as "Is this important?"; unlock via breathing.
- Website blocking (Accessibility); lock mode against uninstalling and workarounds.
- Screen time for distracting apps only; streaks. iOS, macOS, Windows, Android; optional "halo" hardware.

**What's paid**
- Nothing required: "completely free", donation-supported, no subscription. Tips on Play ₹490–₹2,050 / US$4.99–19.99 (iOS US$7/14/25). Tippers get a cosmetic reward and a promise of permanent access if pricing ever changes (per a user review).

**Praise**
- Free yet granular; users repeatedly contrast it with Opal's price. Strong stories of breaking Shorts addiction.

**Complaints**
- Lock settings can trap users who enable them out of curiosity; requests for a password option instead of a passcode; no keyword blocking for browsing.

## 10. one sec (wtf.riedel.onesec)

**Features**
- Breathing pause before a distracting app opens; then shows how many times you tried to open it, with a choice to close or continue.
- App blocker (instant or scheduled), app time limits, focus times.
- Removes Shorts and Reels inside the apps.
- Porn blocker with lock; screen time stats; desktop browser extension.
- Backed by a PNAS study (57% fewer app opens) with the Max Planck Institute.

**What's paid**
- Free: one app only. Pro: unlimited apps, intentional app switching, time tracking, strict block sessions, strict porn blocker. Android Pro lacks intervention types, "don't get lost" notifications and website blocking that iOS has.
- iOS US: US$19.99/year, US$99.99 lifetime, family US$3.99/month, US$29.99/year, US$149 lifetime. Help centre says US$19/year; students 50% off. Germany €14.99/year. Play range ₹85–₹9,999 / US$3.99–99.99. 1-week trial.

**Praise**
- Simple, science-backed, effective; doctors and therapists recommend it.

**Complaints**
- Recent changes: interventions firing mid-use (while typing), features moved behind the paywall, can no longer choose session length.
- Stops working after a day on some phones; privacy unease about Accessibility seeing every app.

## 11. Regain (ai.regainapp): pricing confirmation

- Full study in `docs/research/regain.md`.
- ₹199/month and ₹999/year, frequently discounted (₹84/month, ₹299/year), no lifetime plan, trial length not published. Source: Regain's refund-policy page (Android), as captured earlier. On 7 Nov 2026 that URL returned 404, so the figures are **not re-confirmed today**. Play India IAP range ₹49–₹1,999 is consistent.
- A US Play review complains that strict mode, once free, now needs about US$10 a month **(US price unconfirmed)**.

## Feature matrix

Columns: MP = minimalist phone, OL = Olauncher, PL = Pro Launcher, NI = Niagara, BE = Before, BS = Blank Spaces (iOS only), UN = UnLauncher (100 GB Labs), IN = Indistract, KI = KISS, OP = Opal, SZ = ScreenZen, OS = one sec, RG = Regain. "P" means the feature exists but is paid only; "~" means partial.

| Feature | MP | OL | PL | NI | BE | BS | UN | IN | KI | OP | SZ | OS | RG |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Replaces home screen (launcher) | ✓ | ✓ | ✓ | ✓ | ✓ | ~ | ✓ | ✓ | ✓ | | | | |
| Text-only / icon-free home | ✓ | ✓ | ✓ | ~ | ✓ | ✓ | ✓ | ✓ | ~ | | | | |
| Search-first drawer, auto-launch | ✓ | ✓ | ✓ | ✓ | ✓ | | | ✓ | ✓ | | | | |
| Hide apps | ✓ | ✓ | ✓ | ✓ | P | | ✓ | ✓ | ✓ | | | | |
| Rename apps | ✓ | ✓ | ✓ | | ✓ | | ✓ | ✓ | | | | | |
| Custom fonts | ✓ | | ✓ | P | | | P | ✓ | | | | | |
| Themes / light and dark | ✓ | ✓ | ✓ | P | ✓ | | P | ✓ | ✓ | | | | P |
| Daily or built-in wallpapers | | ✓ | ✓ | ✓ | ✓ | | | | | | | | |
| Gestures (double-tap lock, swipes) | | ✓ | ✓ | P | ✓ | | ✓ | ✓ | ✓ | | | | |
| Folders / categories | | | ✓ | P | ✓ | | | | | | | | |
| Widgets (weather, calendar) | ✓ | | ✓ | P | | ✓ | | ✓ | | | | | |
| Icon packs | | | | P | ✓ | | | | | | | | |
| Notification filter / summary | ✓ | | | ✓ | ✓ | | | | | | | | |
| Grayscale mode | ✓ | | | | | | | | | | | | |
| Notes / tasks on home | | | ✓ | | | | | ✓ | | | | | ✓ |
| Work profile support | ✓ | ✓ | | | ✓ | | | | | | | | |
| Backup and restore | | | | ✓ | ✓ | | | | | | | | |
| Open source | | ✓ | | | | | | | ✓ | | | | |
| App blocker | ✓ | | | | | ✓ | | | | ✓ | ✓ | ✓ | ✓ |
| Website blocker | ✓ | | | | | | | | | ✓ | ✓ | ~ (not Android) | ✓ |
| Blocking schedules | ✓ | | | | | | | | | ✓ | ✓ | ✓ | ✓ |
| Daily per-app time limits | ✓ | | | | | | | | | ✓ | ✓ | P | ✓ |
| Pause / friction before opening | ✓ | | ✓ | | | ✓ | | | | | ✓ | ✓ | |
| Breathing exercise to unlock | | | | | | ✓ | | | | | ✓ | ✓ | |
| In-app usage reminders | ✓ | | | P | | | | | | ~ | ✓ | ✓ | |
| Block Shorts / Reels only | ✓ | | ✓ | | | | | | | | | ✓ | ✓ |
| Strict / hard mode, anti-uninstall | | | | | | | | | | P | ✓ | P | ✓ |
| Focus timer / Pomodoro | ~ | | | | | | | | | ✓ | | | ✓ |
| Screen time stats | ~ | | | | | ✓ | ✓ | ✓ | | ✓ | ✓ | P | ✓ |
| Streaks | | | | | | | | | | ✓ | ✓ | | ✓ |
| Soundscapes / focus music | | | | | | | | | | ✓ | | | ✓ |
| Porn / adult-site blocker | | | | | | | | | | | | ✓ | ✓ |
| Social / group focus, leaderboards | | | | | | | | | | | | | ✓ |
| Parental control / family plan | | | | | | | | | | ✓ | | ✓ | |
| Other platforms (iOS, desktop) | ✓ | | | | | ✓ | | | | ✓ | ✓ | ✓ | ✓ |

## Sources

Google Play listings (read 7 Nov 2026, India and US storefronts):
- https://play.google.com/store/apps/details?id=com.qqlabs.minimalistlauncher
- https://play.google.com/store/apps/details?id=app.olauncher
- https://play.google.com/store/apps/details?id=app.prolauncher
- https://play.google.com/store/apps/details?id=bitpit.launcher
- https://play.google.com/store/apps/details?id=com.beforesoft.launcher
- https://play.google.com/store/apps/details?id=com.hundredgb.unlauncher
- https://play.google.com/store/apps/details?id=com.indistractablelauncher.android
- https://play.google.com/store/apps/details?id=fr.neamar.kiss
- https://play.google.com/store/apps/details?id=com.sduduzog.slimlauncher
- https://play.google.com/store/apps/details?id=com.blanklauncher.app
- https://play.google.com/store/apps/details?id=com.light.phone.launcher
- https://play.google.com/store/apps/details?id=com.withopal.opal
- https://play.google.com/store/apps/details?id=com.screenzen
- https://play.google.com/store/apps/details?id=wtf.riedel.onesec
- https://play.google.com/store/apps/details?id=ai.regainapp

Developer sites and help centres:
- https://www.minimalistphone.com/
- https://help.niagaralauncher.app/article/40-niagara-pro-features
- https://help.niagaralauncher.app/article/104-price-of-niagara-pro
- https://help.niagaralauncher.app/article/85-subscription-model
- https://beforelabs.com
- https://www.blankspaces.app/
- https://screenzen.co
- https://tutorials.one-sec.app/whats-included-in-one-sec-pro
- https://github.com/tanujnotes/olauncher
- https://tanujnotes.substack.com/p/olauncher-minimal-af-launcher
- https://regainapp.ai/ (refund policy, captured earlier in docs/research/regain.md; 404 on 7 Nov 2026)

App Store listings:
- https://apps.apple.com/us/app/-/id6742103871 (minimalist phone, iOS)
- https://apps.apple.com/us/app/blank-spaces-app-launcher/id1570856853
- https://apps.apple.com/us/app/id1532875441 (one sec)
- https://apps.apple.com/in/app/id1497465230 (Opal, India)

Trackers, reviews and discussions:
- https://appgoblin.info/apps/com.qqlabs.minimalistlauncher
- https://appgoblin.info/apps/ai.regainapp
- https://appgoblin.info/apps/com.indistractablelauncher.android
- https://www.techradar.com/phones/i-tried-minimalist-phone-to-improve-my-mindfulness-and-focus-but-just-became-frustrated-and-forgetful
- https://gummysearch.com/r/minimalist_phone/
- https://www.producthunt.com/@richard_walker6/reviews
- https://www.gogi.in/niagara-launcher-review-2026-the-best-minimalist-android-launcher-for-a-distraction-free-phone.html
- https://www.hotukdeals.com/discussions/last-chance-for-cheaper-niagara-launcher-after-what-happened-to-nova-launcher-4033960
- https://medium.com/niagara-launcher/announcing-niagara-pro-bf888f7f830d
- https://www.alphonsolabs.com/best-android-launchers-minimalist-2026/amp/
- https://gizmodo.com/the-best-android-launchers-for-completely-customizing-y-1848355232
- https://www.autonomous.ai/ourblog/opal-app-review
- https://habitdoom.com/blog/is-opal-free
- https://www.whistleout.com/CellPhones/Apps/screenzen-app-review
- https://mobilesyrup.com/2019/02/17/lessphone-launcher-review-smartphone-addiction/
- https://androidinsider.ru/obzory-prilozhenij/lessphone-launcher-samyj-minimalistichnyj-launcher-dlya-android.html
- https://www.notebookcheck.net/Turn-any-Android-device-into-a-Light-Phone-without-sacrificing-the-features-you-can-t-live-without.1004409.0.html
- https://alternativeto.net/software/blank-spaces
- https://alternativeto.net/software/regain-screen-time--focus/about
- https://apt.izzysoft.de/packages/eu.ottop.yamlauncher
