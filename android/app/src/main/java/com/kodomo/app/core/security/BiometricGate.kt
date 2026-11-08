package com.kodomo.app.core.security

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

/**
 * Fingerprint / face unlock for the parent gate, using the phone's own biometric dialog (whatever the phone offers:
 * fingerprint, face or iris). Off by default: a parent switches it on, because a child's finger may also be enrolled.
 */
object BiometricGate {
    private const val PREFS = "kodomo_security"
    private const val KEY = "biometric_gate"

    fun isEnabled(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun setEnabled(context: Context, on: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()

    /** The phone has a fingerprint/face sensor with at least one enrolled finger or face. */
    @Suppress("DEPRECATION")
    fun isAvailable(context: Context): Boolean = runCatching {
        when {
            Build.VERSION.SDK_INT >= 30 -> context.getSystemService(BiometricManager::class.java)
                ?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
            Build.VERSION.SDK_INT == 29 -> context.getSystemService(BiometricManager::class.java)?.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
            Build.VERSION.SDK_INT == 28 -> (context.getSystemService(Context.FINGERPRINT_SERVICE) as? android.hardware.fingerprint.FingerprintManager)
                ?.let { it.isHardwareDetected && it.hasEnrolledFingerprints() } ?: false
            else -> false
        }
    }.getOrDefault(false)

    /** Shows the system dialog; [onSuccess] runs only after a recognised finger or face. */
    fun authenticate(context: Context, title: String, onSuccess: () -> Unit, onFallback: () -> Unit = {}) {
        if (Build.VERSION.SDK_INT < 28) { onFallback(); return }
        val executor = context.mainExecutor
        runCatching {
            val builder = BiometricPrompt.Builder(context)
                .setTitle(title)
                .setSubtitle("اثر انگشت یا چهره‌ی خود را تأیید کنید")
                .setNegativeButton("ورود با رمز", executor) { _, _ -> onFallback() }
            if (Build.VERSION.SDK_INT >= 30) builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            builder.build().authenticate(CancellationSignal(), executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) = onFallback()
            })
        }.onFailure { onFallback() }
    }
}
