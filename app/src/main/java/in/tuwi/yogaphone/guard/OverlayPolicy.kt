package `in`.tuwi.yogaphone.guard

/** Safety exclusions apply regardless of classification or counting preference. */
object OverlayPolicy {
    fun shouldShow(eligible: Boolean, essential: Boolean = false, dialer: Boolean = false,
        camera: Boolean = false, finance: Boolean = false, inCall: Boolean = false,
        locked: Boolean = false, screenOn: Boolean = true, ownApp: Boolean = false,
        systemUi: Boolean = false, keyboard: Boolean = false, launcher: Boolean = false): Boolean =
        eligible && !essential && !dialer && !camera && !finance && !inCall && !locked && screenOn &&
            !ownApp && !systemUi && !keyboard && !launcher
}
