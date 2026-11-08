package com.kodomo.app.kiosk

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.UserManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zero-Escape Kiosk Engine.
 *
 * When Kodomo is provisioned as Device Owner
 * (`adb shell dpm set-device-owner com.kodomo.app/.kiosk.KodomoDeviceAdminReceiver`) it can:
 *  - whitelist itself for Lock Task (true kiosk; Home/Recents/notifications are unavailable),
 *  - disable the status bar & quick settings,
 *  - become the persistent HOME activity,
 *  - apply user restrictions that block safe boot, factory reset, app install/uninstall, USB storage & overlays.
 *
 * Without Device Owner rights it degrades gracefully to Android screen pinning (`startLockTask` with user consent).
 */
@Singleton
class DeviceOwnerManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    val adminComponent: ComponentName = KodomoDeviceAdminReceiver.component(context)

    val isDeviceOwner: Boolean get() = dpm.isDeviceOwnerApp(context.packageName)

    val isAdminActive: Boolean get() = dpm.isAdminActive(adminComponent)

    val isLockTaskPermitted: Boolean get() = dpm.isLockTaskPermitted(context.packageName)

    val isInLockTask: Boolean get() = activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

    val kioskRestrictions: List<String> = buildList {
        add(UserManager.DISALLOW_SAFE_BOOT)
        add(UserManager.DISALLOW_FACTORY_RESET)
        add(UserManager.DISALLOW_INSTALL_APPS)
        add(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
        add(UserManager.DISALLOW_UNINSTALL_APPS)
        add(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
        add(UserManager.DISALLOW_USB_FILE_TRANSFER)
        add(UserManager.DISALLOW_ADD_USER)
        add(UserManager.DISALLOW_CREATE_WINDOWS)
        add(UserManager.DISALLOW_CONFIG_DATE_TIME)
        add(UserManager.DISALLOW_MODIFY_ACCOUNTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS)
    }

    /** Intent that asks the parent to activate Kodomo as a (non-owner) device admin. */
    fun adminActivationIntent(): Intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
        putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "کدومو برای قفل امن محیط کودک و جلوگیری از خروج ناخواسته به این مجوز نیاز دارد.",
        )
    }

    /**
     * Applies the full Device Owner policy set. [allowedPackages] are the parent-approved apps that may be opened from
     * inside the kiosk (they must be whitelisted for lock task too). Returns false when Kodomo is not the device owner.
     */
    fun applyKioskPolicies(allowedPackages: Collection<String> = emptyList()): Boolean {
        if (!isDeviceOwner) return false
        return runCatching {
            dpm.setLockTaskPackages(adminComponent, (listOf(context.packageName) + allowedPackages).distinct().toTypedArray())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.setLockTaskFeatures(adminComponent, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            }
            dpm.setStatusBarDisabled(adminComponent, true)
            kioskRestrictions.forEach { dpm.addUserRestriction(adminComponent, it) }

            val homeFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            dpm.addPersistentPreferredActivity(adminComponent, homeFilter, ComponentName(context, KidsLauncherActivity::class.java))

            listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).forEach { permission ->
                dpm.setPermissionGrantState(adminComponent, context.packageName, permission, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED)
            }
            true
        }.onFailure { Log.e(TAG, "Failed to apply kiosk policies", it) }.getOrDefault(false)
    }

    /** Lifts every restriction (used when the parent turns the kiosk off from the parent hub). */
    fun clearKioskPolicies(): Boolean {
        if (!isDeviceOwner) return false
        return runCatching {
            dpm.setStatusBarDisabled(adminComponent, false)
            kioskRestrictions.forEach { dpm.clearUserRestriction(adminComponent, it) }
            dpm.clearPackagePersistentPreferredActivities(adminComponent, context.packageName)
            dpm.setLockTaskPackages(adminComponent, emptyArray())
            true
        }.onFailure { Log.e(TAG, "Failed to clear kiosk policies", it) }.getOrDefault(false)
    }

    /** Pins Kodomo on screen. With Device Owner this is silent and inescapable; otherwise Android shows its pinning prompt. */
    fun enterLockTask(activity: Activity, allowedPackages: Collection<String> = emptyList()) {
        // Without Device Owner, Android's screen pinning asks for the phone's PIN every time an approved app opens;
        // the app guard keeps the child in bounds instead, so pinning is only used in full (Device Owner) mode.
        if (!isDeviceOwner) return
        if (isInLockTask) return
        if (isDeviceOwner && !isLockTaskPermitted) applyKioskPolicies(allowedPackages)
        runCatching { activity.startLockTask() }.onFailure { Log.w(TAG, "startLockTask failed", it) }
    }

    fun exitLockTask(activity: Activity) {
        if (!isInLockTask) return
        runCatching { activity.stopLockTask() }.onFailure { Log.w(TAG, "stopLockTask failed", it) }
    }

    /** True when Kodomo is the launcher Android starts after boot / when Home is pressed. */
    val isDefaultHome: Boolean
        get() {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            @Suppress("DEPRECATION")
            val resolved = context.packageManager.resolveActivity(home, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            return resolved?.activityInfo?.packageName == context.packageName
        }

    /**
     * Intent that lets the parent make Kodomo the default Home app, so the phone boots straight into Kodomo instead of
     * the manufacturer launcher (MIUI/HyperOS, One UI…). Uses the Home role dialog on Android 10+, settings otherwise.
     */
    fun defaultHomeIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(android.app.role.RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME) && !roles.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                return roles.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
            }
        }
        return Intent(android.provider.Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Fallback for ROMs whose role dialog is hidden: the generic default-apps screen. */
    fun defaultAppsSettingsIntent(): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Permanently releases Device Owner (parent-only, behind the gate). */
    fun releaseDeviceOwnership() {
        if (!isDeviceOwner) return
        clearKioskPolicies()
        @Suppress("DEPRECATION")
        dpm.clearDeviceOwnerApp(context.packageName)
    }

    fun lockScreenNow() {
        if (isAdminActive) dpm.lockNow()
    }

    private companion object { const val TAG = "DeviceOwnerManager" }
}
