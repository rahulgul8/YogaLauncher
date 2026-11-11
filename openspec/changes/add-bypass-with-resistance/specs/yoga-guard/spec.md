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
