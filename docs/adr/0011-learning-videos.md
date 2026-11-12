# ADR 0011: User-chosen YouTube learning channels

Date: 8 November 2026

## Decision

Yoga Pro lets a person maintain an on-device list of YouTube learning channels. The guard reads the current YouTube accessibility tree with the same bounded traversal limits used for Shorts detection. It finds watch-page channel and title fields by resource id first and content-description patterns second. Channel keys are case-folded, whitespace-normalised, stripped of emoji and a trailing `- Topic`. An unreadable or unknown channel is ordinary YouTube use.

Only an explicit user action adds a channel: typing its name, tapping a starter or recent chip, tapping the expanded live pill, or using the focus block card. Title keywords can make that action more prominent but never classify or add a video automatically. Titles exist only for the current guard tick and are never persisted or logged.

Learning foreground seconds are stored per package and local calendar day in DataStore. Effective YouTube use is Android's measured foreground time minus those seconds, never below zero. Channel configuration is always included in manual backup; learning totals are included only when history is requested and are cleared by Delete history.

During an active focus session, Shorts remain blocked and YouTube watch pages are allowed only for listed channels. Home and search are blocked by default. A setting can allow those discovery surfaces. Blocking reuses a debounced Back, Back, Home escalation, while the watch/focus decision remains a pure, unit-tested function.

## Consequences

YouTube can change its accessibility layout, so recognition is deliberately conservative and must be checked against current app versions before release. Mini-player and picture-in-picture work only when YouTube exposes the same metadata. Learning measurements are guard-observed seconds subtracted from Android's independently rounded usage totals, so displayed whole minutes can differ slightly at boundaries.

The feature requires Yoga guard accessibility access and a Yoga Pro entitlement. Free mode retains saved channels but ignores them. No network service, YouTube API, video identifier, title history, dependency, or Room migration is introduced.
