## ADDED Requirements

### Requirement: Quiet update check
Yoga SHALL check Google Play for a newer version when home resumes, at most once every 24 hours, and SHALL NOT make network calls of its own for this.

#### Scenario: Update available
- **WHEN** home resumes, 24 hours have passed since the last check and Play reports a newer version with priority 0–3
- **THEN** show the home line "Update ready · Update" and no dialog

#### Scenario: Checked recently
- **WHEN** home resumes less than 24 hours after the last check
- **THEN** do not check again

### Requirement: Seamless install
Yoga SHALL download normal updates in the background after one tap, and SHALL install a downloaded update when Yoga is no longer in front, so the person does not see a restart.

#### Scenario: Download finished while using another app
- **WHEN** a flexible update has finished downloading and Yoga is in the background
- **THEN** complete the update without showing any screen

#### Scenario: Yoga stays in front
- **WHEN** a downloaded update has waited 24 hours and Yoga is in front
- **THEN** show the home line "Restart to update"

### Requirement: Priority-based prompts
Yoga SHALL use the release's in-app update priority: 0–3 shows a quiet home line, 4 shows Play's flexible update dialog, and 5 starts an immediate update.

#### Scenario: Critical fix
- **WHEN** Play reports an update with priority 5
- **THEN** start the immediate update flow on the next home resume

### Requirement: What's new
Yoga SHALL show one dismissible "New in <version>" home line after the installed version changes, opening a one-screen summary, and SHALL NOT show it after a fresh install.

#### Scenario: After an update
- **WHEN** Yoga starts with a higher version code than it last ran
- **THEN** show the "New in <version>" line until it is opened or dismissed
