# ADR 8: Optional Yoga guard

Status: implementation authorised by the Piece 8 request; device and store validation pending.

## Decision

Use one opt-in AccessibilityService for a live illustrative time-value pill and user-selected Shorts/Reels controls. Keep Android integration separate from pure BurnMeter, OverlayPolicy, ShortsDetector and ShortsGate logic. Use manual application DI and the existing preferences DataStore; do not add dependencies or change Room.

The overlay uses TYPE_ACCESSIBILITY_OVERLAY, a plain Android View and non-focusable/non-touch-modal flags. Guard settings default off. A dedicated disclosure precedes any accessibility settings handoff. The live indicator shares existing usage eligibility and snapshots, with monotonic live elapsed time and a maximum one-refresh-per-minute rate. Sensitive contexts suppress the indicator. Usage measuring off or a snoozed day also hides it.

Only enabled Shorts/Reels packages are inspected, using view-id tables or supported browser address bars. Bound the traversal to 400 nodes and depth 30; release Android nodes on older releases. Never retain or log screen content. A deterministic gate tracks each surface's local-day allowance, debounces Back and escalates to Home after two failed Back attempts within three seconds.

Store only preferences, per-surface daily allowance time and block counts. The existing typed DataStore backup path includes these without a BackupCodec format change. Android service permission itself is never granted by import.

## Tradeoffs and verification

View ids and browser address bars can change across app versions, so detection requires ongoing device checks. Usage snapshots are approximate and minute-rounded; the live amount is illustrative. Accessibility service delivery and process lifetime vary by OEM. The feature is not a disability-support tool (`isAccessibilityTool=false`) and requires Play declaration/review before release.

All 15 guard JUnit tests pass using cached Kotlin 2.0.21 and JUnit directly, without Gradle. Coverage includes counting modes, precision, safety exclusions, positive/negative detection trees, traversal bounds, allowance exhaustion, debounce/escalation and day rollover. A direct Android compile attempt was blocked by sandbox read denial on the Android 36 SDK jar; service/UI compilation and physical-device validation remain pending. See `docs/launch/launch-audit.md` for the device acceptance checklist.

## Device check (Moto g31, Android 12, YouTube 21.43)

Verified by Claude on 2026-11-06: disclosure → accessibility settings → service on; Shorts tab and an external `youtube.com/shorts` link are both blocked, each counted once; the live pill shows and ticks in YouTube, collapses on tap, hides in Yoga and on the device home screen, and stays hidden with "After my limit" and no daily limit. Motorola's launcher sends no window-state event on Home, so the service also confirms the active window's package on every tick. Instagram Reels and browser detection are untested (Instagram not installed).

Default changed on 2026-11-06 at the founder's request: the pill counts from the first minute in an eligible app; "After my limit" stays as an option. The spec suggested after-limit, but the point is to show the cost of wasteful use as it happens.
