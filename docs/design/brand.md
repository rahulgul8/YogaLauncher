# Yoga brand and design system

Status: all three directions ship as user-selectable **looks** (founder's decision, 6 Nov 2026). People pick one in onboarding or Settings → Home and appearance, from live miniature home screens. Obsidian and Brass is the default. Light/dark/follow-phone/greyscale is a separate choice that applies to every look. Two app icons ship for testing, "Ten past ten" (default) and "The colon"; people switch in Settings → Home and appearance (activity-aliases `.IconTenPastTen` / `.IconColon`, see `ui/AppIcons.kt`). The founder will decide whether to keep both or one.

| Look | Display face | Text face | Dark | Light |
| --- | --- | --- | --- | --- |
| Obsidian and Brass (default) | Cormorant Garamond 300 | Manrope | Obsidian, ivory, brass | Porcelain, ink, deep brass |
| Stone and Saffron | Urbanist 200 | Urbanist | Graphite, ivory, saffron | Stone, ink, deep saffron |
| Midnight Sage | Jost 200 | Jost | Forest black, mist, sage | Mist, ink, deep sage |

Code: tokens per look and brightness live in `ui/theme/Color.kt` (`Palettes`, `Tokens`); type in `ui/theme/Type.kt` (`typographyFor`); `YogaPhoneTheme(themeMode, palette)` builds the scheme. **Never hard-code a colour or font in a screen**: read `MaterialTheme`, so every screen follows the chosen look. Code drawn outside Compose (the guard overlay) reads `Prefs.userPreferences.palette` and uses `specOf(palette).dark`.

The token table below describes the default look.

## Idea

A premium, minimalist launcher built around time. The home clock is the hero, and its colon is the brand mark: a brass dot over a plain one. The app icon is a watch dial at 10:10 whose hands, with a stem at six, form a Y.

## Colour (direction A)

| Token | Dark | Light |
| --- | --- | --- |
| Background / surface | Obsidian `#0F0F0E` | Porcelain `#F3F2EF` |
| Raised surface | `#181715` | `#FBFAF8` |
| High surface (pills, highlights) | `#211F1C` | `#E9E7E2` |
| Hairline | `#2E2C27` | `#D9D5CD` |
| Text | Ivory `#ECE6DA` | Ink `#15140F` |
| Muted text, icons | Stone `#8E877A` | `#6A655C` |
| Accent (one per screen, sparingly) | Brass `#C9A45C` | Deep brass `#8A6726` |
| Time-burning only | Ember `#D9824A` | `#B4592A` |

Rules: one brass element per view at most (the clock's dot, an active icon, a primary action). No gradients, no shadows; separate with space first, hairlines second, surfaces last.

## Type

- **Display:** Cormorant Garamond, light (300). Clock, large numbers, screen titles (`displayLarge` … `headlineSmall`). Always lining figures for numbers.
- **Text:** Manrope (400/500/600) for everything else.
- Labels and section headings: `labelSmall`, uppercase, 0.12em tracking, muted or brass.
- All four fonts (Cormorant Garamond, Manrope, Urbanist, Jost) are SIL Open Font License, bundled in `res/font` (see `docs/design/fonts-license.md`).

## Shape and spacing

- Corners: 8 / 12 / 18 / 26 / 34 dp (`YogaShapes`). Pills are fully round.
- Screen side margins 28 dp on home, 24 dp elsewhere. Touch targets at least 48 dp.

## Icons

`ui/theme/YogaIcons.kt`: Yoga's own line icons on a 24-unit grid, 1.6 stroke, round caps. Use these everywhere instead of Material icons, so nothing looks borrowed. Apps the user puts in the dock show their own icon in monochrome.

## Motion

- The drawer follows the finger and settles with a critically damped spring (no bounce).
- The search field takes focus as the drawer starts settling open, so the keyboard rises together with it.
- Avoid full-screen alpha fades during gestures; they cost a frame on mid-range phones.

## Screens done

- [x] Home, dock and app drawer
- [x] Onboarding, settings and appearance
- [x] Focus, timebox and mindful pause
- [x] Guard and disclosure
- [x] Hidden apps, app actions and rename
- [x] Insights
- [x] Usage dashboard, usage access and time value details
- [x] Profiles and categories
- [x] Privacy and help
- [x] Visual interventions, quiet notifications and digest
- [x] Usage settings and value-of-time settings
- [x] Widgets

## Home screen anatomy

Top bar (profile chip · timebox · focus · settings icons) → brand clock → date → status lines → widgets → favourite apps (bottom-aligned) → quick-launch dock (two slots · Search pill · two slots).
