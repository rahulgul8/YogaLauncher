# yogaphone.tuwi.in

The Yoga Phone website: a homepage and the privacy policy. Plain HTML, CSS and JS, no dependencies, no cookies, analytics or third-party requests. Hosted on Firebase Hosting (project `yogaphone-c3901`).

- Pages: `index.html`, `404.html`. Shared head, header, footer and icon sprite live in `_partials/` and are expanded at build time (`<!-- include:name -->`).
- Privacy policy: rendered from `docs/launch/privacy-policy.md` (the single source) into `/privacy`. Edit the Markdown, not HTML.
- Design follows the app: tokens from `ui/theme/Color.kt`, faces from `Type.kt` (fonts subset to Latin from `app/src/main/res/font`), home-screen proportions from `HomeScreen.kt`, `HomeDock.kt` and `BrandClock.kt`. The look picker switches the whole page between the three looks.
- `_cards/` holds the sources for `og.png` (1200x630) and `icon-512.png`. To regenerate, copy a card into `build/site`, serve, and screenshot it with headless Edge or Chrome at that size.

```bash
node scripts/build-site.mjs        # build to build/site
firebase serve --only hosting      # preview on localhost:5000
firebase deploy --only hosting     # build (predeploy) and publish
```
