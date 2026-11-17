## Context

The change crosses launcher UI, Usage Access, the accessibility guard and secure-settings greyscale.

## Decisions

- Keep quota records and transient windows in DataStore; only quota records travel in backups.
- Put rule selection, quota, window-expiry and intervention decisions in Android-free logic.
- Reuse the guard's existing ticker/events and refresh usage at most once a minute per foreground app.
- Route urgent greyscale through QuietRepository, preserving other active greyscale reasons.

## Risks / Trade-offs

- Android can delay usage events; the guard recheck is best effort within about one minute.
- Hosted widget pending intents cannot be intercepted by the launcher; Yoga guard handles their destination app.

