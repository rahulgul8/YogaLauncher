## ADDED Requirements

### Requirement: Optional live time value
Yoga SHALL offer a non-focus-stealing accessibility overlay for eligible app time, with Burning/Gentle styles, after-TOTAL-limit or first-minute counting, one-second updates, minute-throttled usage snapshots and low-rate minor-unit precision.

#### Scenario: Safe eligible use
- **WHEN** live time value, usage measuring and the hourly value feature are enabled, today is not snoozed, and the foreground app is eligible
- **THEN** show the selected counting mode's illustrative amount, only after a TOTAL rule is OVER in after-limit mode
- **AND** allow collapse and vertical repositioning without taking outside touches

#### Scenario: Sensitive context
- **WHEN** the screen is off/locked, a call is active, or an essential, dialer, camera, Finance/payment, system UI, keyboard or launcher surface is foreground
- **THEN** do not show the indicator

### Requirement: Selected short-form surfaces
Yoga SHALL inspect only enabled YouTube/Instagram and supported browser surfaces with bounded transient node traversal and SHALL never store, log or send screen content.

#### Scenario: Blocked or exhausted surface
- **WHEN** a selected Shorts/Reels surface is detected and configured Block or has exhausted its daily allowance
- **THEN** issue debounced Back, show a brief explanatory card, and escalate to Home after two failed Backs within three seconds
- **AND** store per-surface daily aggregate block counts locally and show today's sum on Usage

#### Scenario: Allowed surface
- **WHEN** an enabled surface has remaining daily allowance
- **THEN** account foreground elapsed time against that surface's allowance and reset accounting on local-day rollover

### Requirement: Disclosure and control
Yoga SHALL default guard features off, expose per-surface allowances and service status, and require affirmative in-app disclosure consent before opening Android accessibility settings.

#### Scenario: Activation handoff
- **WHEN** the user requests Yoga guard activation or management
- **THEN** explain app/window access, transient screen-structure inspection, non-retention/non-transmission and revocation
- **AND** open system settings only after Agree and open settings, with Not now cancelling
