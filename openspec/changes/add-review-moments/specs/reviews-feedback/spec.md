## ADDED Requirements

### Requirement: Policy-compliant review eligibility
Yoga SHALL request a Play review only after an eligible product moment when it has been installed for at least three
days, has resumed home at least five times, is the default home, is outside onboarding, has had no interruption in ten
minutes, is not over today's allowance, has not requested a review in 90 days, and has made fewer than three requests.
Yoga SHALL use each review moment at most once and SHALL record an attempt before launching Play's review flow.

#### Scenario: Every guard passes
- **WHEN** an eligible moment occurs and every review guard passes
- **THEN** Yoga records the attempt and moment and launches Play's review flow

#### Scenario: A guard fails
- **WHEN** an eligible moment occurs but any review guard fails
- **THEN** Yoga does not request a Play review

### Requirement: Positive review moments
Yoga SHALL evaluate review moments after Shorts/Reels blocking is enabled from an introduction, after leaving a good
seven-day usage view, on home after the third completed focus session or timebox, and during 18:00–22:00 on a day
after the first strong block day. A good week requires at least five known days and either five days within the
allowance or at least 20 percent less distracting time than the previous week. A strong block day requires at least
five Shorts/Reels blocks without going over the allowance.

#### Scenario: Strong day occurs
- **WHEN** Yoga records five eligible blocks on a day while the person remains within the allowance
- **THEN** Yoga waits until an evening home resume on a later local date before evaluating the review request

#### Scenario: Third session completes
- **WHEN** the third focus session or timebox completes normally
- **THEN** Yoga evaluates its review moment on the next home resume and not on an early stop

### Requirement: Review interruptions
Yoga SHALL record the latest block card, mindful pause, or limit-notification interruption and SHALL suppress review
requests for ten minutes afterward.

#### Scenario: Pause just appeared
- **WHEN** a mindful pause appears less than ten minutes before an otherwise eligible moment
- **THEN** Yoga does not request a review

### Requirement: Visible feedback email
Yoga SHALL provide Help → Send feedback, opening an email addressed to `tuwi.developer@gmail.com` with app version,
Android version, device model, and up to three recent local opt-out answers. Yoga SHALL never send the email itself.

#### Scenario: Email app exists
- **WHEN** the person taps Send feedback and an email app is available
- **THEN** Yoga opens the prefilled email for review before sending

#### Scenario: No email app exists
- **WHEN** the person taps Send feedback and no email app is available
- **THEN** Yoga shows "No email app found"

### Requirement: Optional opt-out feedback
Yoga SHALL offer an inline, dismissible "What didn't work?" question when guard, daily allowance, or mindful pause is
turned off within seven days of being turned on. Yoga SHALL keep only the last three answers on-device, SHALL offer
recent Other text for reuse, and SHALL transmit nothing automatically.

#### Scenario: Recently enabled feature is disabled
- **WHEN** a person turns off one of the three features within seven days of turning it on
- **THEN** Yoga shows that feature's three reasons plus Other as an optional inline question

#### Scenario: Feature was enabled more than seven days ago
- **WHEN** a person turns off one of the three features more than seven days after turning it on
- **THEN** Yoga does not show the opt-out question
