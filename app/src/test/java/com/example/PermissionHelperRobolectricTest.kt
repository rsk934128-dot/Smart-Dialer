package com.example

import android.Manifest
import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.PermissionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PermissionHelperRobolectricTest {

    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(app).denyPermissions(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    @Test
    fun checkStatusReflectsMissingAndGrantedCorePermissions() {
        val initial = PermissionHelper.checkStatus(app)
        assertFalse(initial.hasCallLog)
        assertFalse(initial.hasPhoneState)
        assertFalse(initial.hasCallPhone)
        assertFalse(initial.hasContacts)
        assertFalse(initial.isCoreGranted)
        assertEquals(listOf("Call Logs", "Phone & SIM", "Contacts"), initial.missingCoreLabelsEn)

        Shadows.shadowOf(app).grantPermissions(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        )

        val granted = PermissionHelper.checkStatus(app)
        assertTrue(granted.hasCallLog)
        assertTrue(granted.hasPhoneState)
        assertTrue(granted.hasCallPhone)
        assertTrue(granted.hasContacts)
        assertTrue(granted.isCoreGranted)
        assertTrue(granted.missingCoreLabelsEn.isEmpty())
    }

    @Test
    fun missingPermissionsFiltersOutAlreadyGrantedEntries() {
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS)

        val missingContacts = PermissionHelper.getMissingPermissions(
            app,
            PermissionHelper.contactsPermissionsArray()
        )
        assertFalse(missingContacts.contains(Manifest.permission.READ_CONTACTS))
        assertTrue(missingContacts.contains(Manifest.permission.WRITE_CONTACTS))
    }

    @Test
    fun promptedTrackingPersistsAndDetectsPermanentDenialWhenActivityDoesNotShowRationale() {
        val activity = Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        val callLogPerms = listOf(Manifest.permission.READ_CALL_LOG)

        assertFalse(PermissionHelper.wasPromptedBefore(activity, Manifest.permission.READ_CALL_LOG))
        assertFalse(PermissionHelper.isPermanentlyDenied(activity, callLogPerms))

        PermissionHelper.markPermissionsPrompted(activity, callLogPerms)
        assertTrue(PermissionHelper.wasPromptedBefore(activity, Manifest.permission.READ_CALL_LOG))
        assertTrue(PermissionHelper.isPermanentlyDenied(activity, callLogPerms))
    }

    @Test
    fun appSettingsIntentTargetsPackageDetailsScreen() {
        val intent = PermissionHelper.createAppSettingsIntent(app)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:${app.packageName}", intent.dataString)
    }

    @Test
    fun dialerViewModelUpdatesAllPermissionFlowsOnRefresh() {
        val vm = DialerViewModel(app)
        assertFalse(vm.permissionStatus.value.hasCallLog)
        assertFalse(vm.permissionStatus.value.hasPhoneState)
        assertFalse(vm.permissionStatus.value.hasContacts)

        Shadows.shadowOf(app).grantPermissions(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        )
        vm.refreshSimsAndSyncHistory(showFeedback = false)

        assertTrue(vm.hasCallLogPermission.value)
        assertTrue(vm.hasPhonePermission.value)
        assertTrue(vm.hasContactsPermission.value)
        assertTrue(vm.permissionStatus.value.isCoreGranted)
    }
}
