## ADDED Requirements

### Requirement: Deliberate timed way through
Yoga SHALL offer a free way through blocked Shorts/Reels by default: a wait from ResistanceLadder, duration choices, a factual family cost line and a 1500 ms hold. Not now and Back SHALL record one resisted outcome and close the screen without a pass. A screen reader SHALL have a labelled accessible activation after the wait, and reduced motion SHALL use plain progress.

#### Scenario: Choosing a pass
- **WHEN** a person selects Watch anyway on a non-strict block card
- **THEN** open the resistance activity and record an interruption
- **AND** wait 15 seconds, doubling per earlier family pass today up to 120 seconds, before permitting continuation
- **AND** offer 5, 10 and 15 minutes for the first three passes, then only 5 minutes
- **AND** preselect the previous duration when available, otherwise the middle choice

#### Scenario: Card introduction priority
- **WHEN** an introduction is claimed on the first block today
- **THEN** show the introduction instead of Watch anyway
- **AND** otherwise show the quiet action with a 48 dp target for six seconds when not strict

#### Scenario: Choosing not to continue
- **WHEN** Not now or Back is activated
- **THEN** increment today's resisted count once and return to the regular feed reached by the guard's Back action

### Requirement: Family passes and local accounting
A YouTube pass SHALL cover native and browser YouTube only, and an Instagram pass SHALL cover native and browser Instagram only. The gate SHALL continue existing allowance accounting while a pass is live. Choice counters SHALL be updated transactionally, preserved against stale gate saves and reset at local-day rollover. Blocks, passes and resisted aggregates SHALL be retained locally for seven days; no screen content SHALL leave the phone.

#### Scenario: Pass active and expiry
- **WHEN** enabled Shorts/Reels is detected during its family pass
- **THEN** permit it and show a small family/minutes-left pill regardless of Pro or time-value settings, replacing any value pill
- **AND** when it expires hide the pass pill and resume normal gate decisions
- **AND** the next block card SHALL say Your N minutes are up once, using that family's pass duration

### Requirement: Strict mode and deferred loosening
Strict mode SHALL be opt-in and SHALL hide the way through without changing gate evaluation of an existing pass. A scheduled strict-off date SHALL take effect on that local date. Settings (J4) SHALL apply strict loosening tomorrow and tightening immediately, and other loosening SHALL require resistance.

#### Scenario: Strict-off pending
- **WHEN** strict is enabled and strictOffFrom is tomorrow
- **THEN** show today's unchanged strict card
- **AND** permit the way through from tomorrow

#### Scenario: Tightening a rule
- **WHEN** the person tightens a guard rule in settings
- **THEN** apply it immediately without a cooling-off period


### Requirement: WhatsApp Status as a separate guard family
Yoga SHALL offer an opt-in WhatsApp Status block for com.whatsapp and com.whatsapp.w4b as a third independent family. It SHALL recognise only the full-screen Status viewer by a StatusPlayback activity class, with package-scoped status_playback_container, status_playback_page and playback_container view ids as fallback. Chats, calls, groups, the Updates list and camera SHALL remain available. The new GuardSurface entry SHALL be appended, preserving old four-surface history ordinals and decoding absent Status counts as zero.

#### Scenario: Viewing Status with the block enabled
- **WHEN** either WhatsApp package opens a StatusPlayback activity or a viewer-specific fallback id is detected
- **THEN** use the existing Back action and repeated-attempt Home escalation
- **AND** show Status is off, Chats and calls still work and today's block count
- **AND** offer Watch anyway unless strict mode is active

#### Scenario: Ordinary WhatsApp use
- **WHEN** the person opens chats, calls, groups, the Updates list or camera
- **THEN** do not detect Status from tab labels or ordinary screen content
- **AND** replace the remembered activity when a new activity arrives, ignoring framework dialog and toast classes and clearing it when the foreground package changes

#### Scenario: A Status pass
- **WHEN** the person completes the existing resistance screen for Status
- **THEN** grant a pass for Status only and show Status with minutes left
- **AND** keep Shorts and Reels passes and counters independent
- **AND** resume blocking when the Status pass expires

#### Scenario: Settings and onboarding
- **WHEN** the person opens guard settings
- **THEN** offer WhatsApp Status with the existing switch, Block or Allow daily choices, minute choices and strict/loosening rules
- **AND** when WhatsApp or WhatsApp Business is installed, offer the Status switch in onboarding's blocks step with real weekly minutes and Chats and calls still work

#### Scenario: Status introduction
- **WHEN** WhatsApp or WhatsApp Business is installed, combined weekly WhatsApp usage is at least 180 minutes, Status is not enabled and guard functionality is available
- **THEN** offer Turn off Status on Usage and home with the weekly duration and Chats and calls still work
- **AND** use the existing disclosure, Android settings, confirmation with Undo and R1 review path
- **AND** make dismissed introductions available through Suggestions
- **AND** never offer a sibling introduction between Status and Shorts or Reels

#### Scenario: Disclosure and local data
- **WHEN** the person considers enabling Yoga guard
- **THEN** disclose WhatsApp Status and transient YouTube channel/title access for Learning videos, retaining the statement that screen content is never stored, logged or sent anywhere
- **AND** list local settings, chosen learning channels, learning totals, allowance time, timed passes and aggregate counters
