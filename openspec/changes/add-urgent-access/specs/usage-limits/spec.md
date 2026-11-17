## ADDED Requirements

### Requirement: App and category allowance enforcement
Yoga SHALL replace an over-limit app launch with a Time's up screen showing the app, its daily minutes, Home and any available urgent access. Yoga guard SHALL recheck the foreground app within about one minute without adding a separate polling loop.

#### Scenario: Opening an over-limit app
- **WHEN** an enabled app rule or the app's category rule is OVER
- **THEN** a launch from Yoga shows Time's up instead of opening the app
- **AND** an app detected by Yoga guard returns to Yoga and shows the same screen

#### Scenario: Guard unavailable
- **WHEN** an app or category becomes over limit without Yoga guard
- **THEN** Yoga keeps the usage notification and says the affected app is closed for today

### Requirement: Scarce urgent access
Yoga SHALL offer at most three urgent opens per local day across apps, record them separately per app and day, and start each from a three-second hold with visible progress and a TalkBack action. Each open SHALL bypass the pause and allowance block for five minutes.

#### Scenario: Urgent open available
- **WHEN** a person completes the Urgent hold on a pause or Time's up screen
- **THEN** Yoga opens the app at once for five minutes and records one urgent open for that app and local day

#### Scenario: Daily quota used
- **WHEN** three urgent opens have already been recorded for the local day
- **THEN** Yoga says none are left and omits the Urgent control

### Requirement: Gentle urgent-window expiry
Yoga SHALL NOT close an app when its urgent window expires. While the person remains in it, Yoga SHALL apply greyscale when secure-settings control is available, otherwise post a gentle reminder every five minutes. Leaving the app SHALL stop reminders and remove only the urgent greyscale reason.

#### Scenario: Window expires in the app
- **WHEN** five urgent minutes expire while the app remains foreground
- **THEN** Yoga turns the screen grey when supported or posts a reminder every five minutes
- **AND** calls, notifications and Do Not Disturb remain unchanged

#### Scenario: Colour is safely restored
- **WHEN** the person leaves the app, the service restarts, the phone reboots, or an expired window is found elsewhere
- **THEN** Yoga clears urgent greyscale and preserves any focus, bedtime or total-limit greyscale reason

### Requirement: Urgent access visibility and backup
Usage SHALL show today's urgent opens out of three. Daily per-app urgent counts SHALL be included in preference backups, while active windows SHALL not be backed up. Greyscale settings SHALL state whether control is available and how to enable it.

#### Scenario: Reviewing urgent use
- **WHEN** the person opens Usage after one urgent open
- **THEN** Yoga shows Urgent opens today: 1 of 3

