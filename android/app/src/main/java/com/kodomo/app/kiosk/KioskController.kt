package com.kodomo.app.kiosk

import androidx.compose.runtime.staticCompositionLocalOf

/** Window-level kiosk controls implemented by [KidsLauncherActivity] and consumed by composables. */
interface KioskController {
    /** Enter the locked kids space (lock task + immersive). Lock task is applied only if the parent enabled the kiosk. */
    fun enterKidSpace()

    /** Leave the locked space after a successful parent gate. */
    fun exitKidSpace()

    /** Dims the display for bedtime. */
    fun setBedtimeDim(enabled: Boolean)

    /** Immediately applies Device Owner policies and enables the kiosk. Returns false if not Device Owner. */
    fun applyDeviceOwnerPolicies(): Boolean

    fun requestDeviceAdmin()

    /** Asks Android to make Kodomo the default launcher (boot lands in Kodomo). */
    fun requestDefaultHome()
}

val LocalKioskController = staticCompositionLocalOf<KioskController> {
    error("KioskController not provided")
}
