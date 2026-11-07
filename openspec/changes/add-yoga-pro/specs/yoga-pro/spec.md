## ADDED Requirements

### Requirement: Reverse trial
Yoga SHALL give every new user all Yoga Pro features for 14 days from first launch without asking for payment, and SHALL NOT show a paywall during onboarding.

#### Scenario: New install
- **WHEN** Yoga is opened for the first time
- **THEN** start the 14-day trial and enable every Pro feature

#### Scenario: Trial ends without purchase
- **WHEN** 14 days have passed and the user has no Pro purchase
- **THEN** pause Pro features, keep their settings and data, and keep the launcher fully working

### Requirement: The launcher is never locked
Yoga SHALL keep the home screen, clock, app drawer, search and dock working in every entitlement state.

#### Scenario: Free tier
- **WHEN** the user is on the free tier
- **THEN** every installed app can still be found and opened from Yoga

### Requirement: Honest upgrade prompt
Yoga SHALL show one dismissible trial-ending card from day 10, built from the user's own results, and SHALL NOT use countdown timers or invented discounts.

#### Scenario: Card dismissed
- **WHEN** the user dismisses the trial-ending card
- **THEN** do not show it again; Yoga Pro stays reachable from Settings

### Requirement: Purchase and restore
Yoga SHALL sell Yoga Pro through Google Play Billing as a yearly subscription and a lifetime purchase, show prices exactly as Play returns them, and restore Pro from existing purchases.

#### Scenario: Purchase after lapse
- **WHEN** a user on the free tier buys Yoga Pro
- **THEN** re-enable Pro features with the settings they had before
