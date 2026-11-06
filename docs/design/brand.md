# Yoga brand and design system

Status: direction A ("Obsidian and Brass") and the "Ten past ten" icon are built in as the working default. The founder will make the final pick from the review board; swapping is a token change in `ui/theme/Color.kt`, the two fonts in `res/font`, and the three icon drawables.

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
- Both fonts are SIL Open Font License, bundled in `res/font` (see `docs/design/fonts-license.md`).

## Shape and spacing

- Corners: 8 / 12 / 18 / 26 / 34 dp (`YogaShapes`). Pills are fully round.
- Screen side margins 28 dp on home, 24 dp elsewhere. Touch targets at least 48 dp.

## Icons

`ui/theme/YogaIcons.kt`: Yoga's own line icons on a 24-unit grid, 1.6 stroke, round caps. Use these everywhere instead of Material icons, so nothing looks borrowed. Apps the user puts in the dock show their own icon in monochrome.

## Motion

- The drawer follows the finger and settles with a critically damped spring (no bounce).
- The keyboard opens only after a slide has settled, and only when the user came from Search.
- Avoid full-screen alpha fades during gestures; they cost a frame on mid-range phones.

## Home screen anatomy

Top bar (profile chip · timebox · focus · settings icons) → brand clock → date → status lines → widgets → favourite apps (bottom-aligned) → quick-launch dock (two slots · Search pill · two slots).
