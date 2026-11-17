# ADR 0016: Enforced limits with urgent access

Date: 2026-11-17. Status: accepted.

## Context

App and category allowances produced reminders but did not stop an app. A hard stop also needs a deliberate escape for genuinely urgent use without weakening the normal growing pause.

## Decision

Every launch from Yoga checks the app and category rule. Yoga guard repeats that check at most once a minute for the foreground app using its existing event/ticker loop. An over-limit app returns to Yoga and shows Time's up. Without the guard, the over-limit notification says the app is closed for today.

Time's up and the mindful pause offer a three-second Urgent hold, with a TalkBack action. It grants five minutes and consumes one of three local-day opens shared across apps. Counts remain per app and day. During a live urgent window the allowance block is bypassed. After expiry Yoga does not close the app: the guard applies device greyscale when secure-settings control is available, otherwise it reminds every five minutes until the person leaves.

Urgent greyscale is reconciled through QuietRepository so focus, bedtime and total-limit greyscale remain authoritative. Service restart, reboot and leaving the app clear the urgent reason before restoring colour. Urgent counts are backed up with preferences; momentary windows are not. No DND or notification-holding path is used.

## Consequences

Enforcement depends on Usage Access for current totals and on Yoga guard for apps opened outside Yoga or limits crossed while already inside an app. Android usage events can lag, so the one-minute guard cadence is a target rather than an exact timer. Foreground detection, greyscale restoration and reminder cadence need real-phone checks.

