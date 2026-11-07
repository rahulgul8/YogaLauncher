## ADDED Requirements

### Requirement: Learning channel list
Yoga SHALL let the user keep a list of YouTube channels whose videos count as learning, built by adding the channel of the video being watched, by picking starter chips, or by typing a name. Yoga SHALL NOT add a channel without the user's action.

#### Scenario: Add from the current video
- **WHEN** a YouTube video from a channel not on the list is playing and the user taps "Count as learning" on the live pill
- **THEN** add that channel to the list and treat the current video as learning from the next guard tick

#### Scenario: Starter chips
- **WHEN** the user opens Learning channels for the first time
- **THEN** show starter education channels as unselected chips, plus the user's recently added channels

### Requirement: Learning time is not counted as distraction
While a video from a learning channel plays, Yoga SHALL NOT count that time against the app's allowance or show a burning time value, and SHALL record it as learning time for that day.

#### Scenario: Learning video
- **WHEN** the YouTube watch page shows a video whose channel is on the list
- **THEN** show the calm "Learning" pill state, exclude the time from the YouTube allowance, and add it to today's learning time

#### Scenario: Autoplay leaves the list
- **WHEN** the next video comes from a channel that is not on the list
- **THEN** resume normal counting within one guard tick

#### Scenario: Channel cannot be read
- **WHEN** the channel name cannot be found on screen
- **THEN** treat the time as ordinary YouTube use

### Requirement: Learning-only YouTube during focus
During a focus session Yoga SHALL allow YouTube videos only from learning channels, SHALL keep Shorts blocked, and SHALL block the home feed unless the user turned that off.

#### Scenario: Non-learning video during focus
- **WHEN** a focus session is active and a video from a channel not on the list opens
- **THEN** go Back, show "Not on your learning list" with an "Add channel" action, and escalate to Home after two failed Backs

### Requirement: Screen content stays private
Yoga SHALL read channel names and titles only in memory during a guard tick, SHALL store only channel names the user added and daily learning totals, and SHALL never send them off the device.

#### Scenario: Backup and logs
- **WHEN** the user exports a backup or logs are inspected
- **THEN** they contain the learning channel list and daily totals only, with no video titles
