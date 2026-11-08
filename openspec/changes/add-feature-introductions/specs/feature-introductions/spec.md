## ADDED Requirements

### Requirement: Evidence-led introductions
Yoga SHALL introduce a feature only with a number from the person's own phone, except introductions defined as time-based, and SHALL NOT introduce a feature that is already on or that depends on an app that is not installed.

#### Scenario: Heavy YouTube use
- **WHEN** YouTube was used at least 3 hours in the last 7 days and Yoga guard is off for YouTube
- **THEN** offer "Turn off Shorts" with that week's YouTube time as the evidence

#### Scenario: Feature already on
- **WHEN** Shorts blocking is already on
- **THEN** do not show the Shorts introduction

### Requirement: One at a time
Yoga SHALL show at most one introduction per surface, at most one introduction not shown before per local day, and none within 24 hours of finishing onboarding.

#### Scenario: Two introductions eligible
- **WHEN** two introductions qualify for the same surface
- **THEN** show the one with the higher estimated weekly impact

### Requirement: Respectful dismissal
Yoga SHALL snooze an introduction for 7 days when the person taps "Not now", SHALL retire it after a second "Not now", and SHALL list retired introductions in Help → Suggestions.

#### Scenario: Second dismissal
- **WHEN** the person taps "Not now" on an introduction they dismissed before
- **THEN** never show it again outside Help → Suggestions

### Requirement: One-tap enable with undo
Yoga SHALL turn the introduced feature on with values filled in from the person's data, and SHALL confirm with an Undo action instead of a confirmation dialog, except where Android requires its own permission screen.

#### Scenario: Turn off Shorts
- **WHEN** the person taps "Turn off Shorts" and grants Yoga guard in Android settings
- **THEN** on return show "Shorts are off. Regular videos still work." with Undo
