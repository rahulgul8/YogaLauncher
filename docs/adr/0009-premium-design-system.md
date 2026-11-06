# ADR 0009: Premium design system

Date: 6 November 2026

## Decision

Yoga presents every screen as one restrained, time-centred object. Compose screens use the shared `YogaComponents` kit, 24 dp screen margins, display type for titles and hero numbers, text type elsewhere, spatial grouping instead of dividers, and Yoga's own 24-unit line icons. Sheets use a 28 dp top radius and hairline handle; dialogs use a 26 dp surface. Controls, cards and progress indicators obtain all colour and typography from `MaterialTheme`.

The kit includes screens, headers, sections, rows and choice rows, chips, cards, notes, fields, stats, actions, `YogaSlider`, `YogaSheet`, `YogaDialog` and `YogaProgress`. Accent marks the single primary or selected item. Ember is reserved for time value and over-limit state. There are no gradients or shadows.

## Looks and tokens

People choose one of three looks independently of light, dark, follow-phone or greyscale mode:

- Obsidian and Brass: Cormorant Garamond display, Manrope text.
- Stone and Saffron: Urbanist display and text.
- Midnight Sage: Jost display and text.

Each look owns light and dark `Tokens`: background, raised/high/highest surfaces, line, outline, text, muted text, accent and its content/container colours, and ember. `specOf(palette)` is the source of truth. Compose maps tokens through `YogaPhoneTheme`; plain Views such as the guard overlay read `Prefs.userPreferences.palette` and use the selected look's dark tokens.

## Consequences

Screens do not hard-code colour or font families, borrow Material icon sets, or instantiate raw Material controls where the kit has an equivalent. The guard remains a plain View overlay but refreshes when the selected palette changes. All three looks must remain legible in light and dark.
