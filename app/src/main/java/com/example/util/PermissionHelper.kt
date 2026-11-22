package com.example.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class DialerPermissionsStatus(
    val hasCallLog: Boolean = false,
    val hasPhoneState: Boolean = false,
    val hasPhoneNumbers: Boolean = false,
    val hasCallPhone: Boolean = false,
    val hasContacts: Boolean = false,
    val hasWriteContacts: Boolean = false,
    val hasNotifications: Boolean = true
) {
    val isPhoneGroupGranted: Boolean
        get() = hasPhoneState && hasCallPhone

    val isCoreGranted: Boolean
        get() = hasCallLog && hasPhoneState && hasCallPhone && hasContacts

    val missingCoreLabelsEn: List<String>
        get() = buildList {
            if (!hasCallLog) add("Call Logs")
            if (!hasPhoneState || !hasCallPhone) add("Phone & SIM")
            if (!hasContacts) add("Contacts")
        }

    val missingCoreLabelsBn: List<String>
        get() = buildList {
            if (!hasCallLog) add("কল লগ")
            if (!hasPhoneState || !hasCallPhone) add("ফোন ও সিম")
            if (!hasContacts) add("কন্টাক্ট")
        }
}

object PermissionHelper {

    private const val PREFS_NAME = "dialer_permission_prefs"
    private const val KEY_PROMPTED_PREFIX = "prompted_"

    fun corePermissionsArray(): Array<String> = buildList {
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.READ_CALL_LOG)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.WRITE_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Manifest.permission.ANSWER_PHONE_CALLS)
            add(Manifest.permission.READ_PHONE_NUMBERS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    fun callLogAndPhonePermissionsArray(): Array<String> = buildList {
        add(Manifest.permission.READ_CALL_LOG)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Manifest.permission.READ_PHONE_NUMBERS)
        }
    }.toTypedArray()

    fun phoneGroupPermissionsArray(): Array<String> = buildList {
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Manifest.permission.READ_PHONE_NUMBERS)
            add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
    }.toTypedArray()

    fun contactsPermissionsArray(): Array<String> = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS
    )

    fun isGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkStatus(context: Context): DialerPermissionsStatus {
        val phoneNumbersGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            isGranted(context, Manifest.permission.READ_PHONE_NUMBERS)
        } else {
            true
        }
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

        return DialerPermissionsStatus(
            hasCallLog = isGranted(context, Manifest.permission.READ_CALL_LOG),
            hasPhoneState = isGranted(context, Manifest.permission.READ_PHONE_STATE),
            hasPhoneNumbers = phoneNumbersGranted,
            hasCallPhone = isGranted(context, Manifest.permission.CALL_PHONE),
            hasContacts = isGranted(context, Manifest.permission.READ_CONTACTS),
            hasWriteContacts = isGranted(context, Manifest.permission.WRITE_CONTACTS),
            hasNotifications = notificationsGranted
        )
    }

    fun getMissingPermissions(context: Context, permissions: Array<String>): List<String> {
        return permissions.filter { !isGranted(context, it) }
    }

    fun markPermissionsPrompted(context: Context, permissions: Collection<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            permissions.forEach { perm ->
                putBoolean("$KEY_PROMPTED_PREFIX$perm", true)
            }
        }.apply()
    }

    fun wasPromptedBefore(context: Context, permission: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("$KEY_PROMPTED_PREFIX$permission", false)
    }

    fun shouldShowRationale(activity: Activity?, permissions: Collection<String>): Boolean {
        if (activity == null) return false
        return permissions.any { perm ->
            !isGranted(activity, perm) &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
        }
    }

    fun isPermanentlyDenied(context: Context, permissions: Collection<String>): Boolean {
        val activity = context.findActivity() ?: return false
        val missing = permissions.filter { !isGranted(context, it) }
        if (missing.isEmpty()) return false
        return missing.any { perm ->
            wasPromptedBefore(context, perm) &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
        }
    }

    fun createAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun Context.findActivity(): Activity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
