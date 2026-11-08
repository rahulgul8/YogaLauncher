# Brief 13: Onboarding and introduction images (piece B)

You are working in a git worktree of the Yoga Phone Android app on branch `onboarding-images`. Claude reviews the images, converts them, tests them on the phone in all three looks, and commits.

**Use your built-in image generation (the `imagegen` skill / `image_gen` tool).** No API key is needed.

## Context

- Yoga Phone is a premium, minimalist Android home screen that helps people use their phone on purpose. Its selling point is a sophisticated, detail-led look.
- It has three looks, each with its own accent colour:
  - **Obsidian** (brass `#C9A45C` on near-black `#0F0F0E`);
  - **Stone** (saffron `#B85E17` on warm grey `#ECEBE7`);
  - **Midnight Sage** (sage `#93B8A0` on deep green-black `#0C1310`).
- Each look also has a light or dark variant. Read `docs/design/brand.md` for the tone.
- The images will be **tinted at runtime with the current look's accent**, so they must be **single-colour line art**. One image set then works in every look, light and dark.

## Style (decided by the founder)

Apply this to every image:

- **Fine etched engraving line art**, like a banknote engraving or a luxury watch catalogue illustration.
- **Lines:**
  - hairline strokes, with light cross-hatching only for shading;
  - **pure black (#000000) lines only, on a fully transparent background**: no fills, no grey wash, no paper texture, no vignette.
- **Composition:**
  - **generous negative space**; the subject sits in the middle 60% of the canvas;
  - calm, still, symmetrical or gently balanced;
  - no people's faces; hands are fine if needed.
- **No text, letters, numbers, logos, UI or app icons.** Nothing that resembles YouTube, Instagram or any brand mark.
- Keep the style consistent across the set. Generate the first image (welcome), and once it looks right, use it as the style reference for every other image.

## Images

| File | Size | Subject |
|---|---|---|
| `welcome.png` | 1536×1024 | A low sun resting on perfectly still water, inside the thin circle of a watch dial. Quiet morning. |
| `awareness.png` | 1536×1024 | A slender hourglass; a fine stream of sand catches the light. |
| `shorts.png` | 1536×1024 | An endless vertical film strip, cut cleanly in two by a single straight line, and the loose end curling away. |
| `reels.png` | 1536×1024 | A loop of film gently coming untied, a ribbon falling free. |
| `allowance.png` | 1536×1024 | A simple ceramic bowl being filled with water up to a fine marked line. |
| `nudges.png` | 1536×1024 | A single feather landing on still water, one ripple ring. |
| `done.png` | 1536×1024 | An open doorway framing a calm horizon line. |
| `spot_shield.png` | 1024×1024 | A small, simple shield. Engraved, not heraldic. |
| `spot_hourglass.png` | 1024×1024 | A small hourglass (simpler than awareness.png). |
| `spot_moon.png` | 1024×1024 | A crescent moon with a few fine stars. |

## Steps

1. Generate each image with a transparent background. If an output has an opaque or off-white background, regenerate it rather than accept it.
2. Look at every output critically and regenerate any that fail. Common problems:
   - text or stray glyphs;
   - fills or grey shading blocks;
   - busy composition;
   - a style that drifts from the welcome image;
   - anything that looks like a logo.
3. Copy the chosen final PNG for each file from `~/.codex/generated_images/` to `design/onboarding/src/<file>` in this worktree.
4. Write `design/onboarding/README.md` with:
   - a table of the files;
   - the exact prompt used for each;
   - a note that the source PNGs are black line art on transparency, converted later to WebP in `res/drawable-nodpi/`.
5. Don't touch any Kotlin, Gradle or `res/` files.
6. When finished, list the files, any image you weren't happy with, and why.
