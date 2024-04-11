package com.surendar4.criticalnotifications

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.surendar4.criticalnotifications.notifications.NotificationNotifier
import com.surendar4.criticalnotifications.ui.theme.CriticalNotificationsTheme

class MainActivity : ComponentActivity() {

    private val postNotificationsRequestCode = 1001
    private lateinit var notificationManager: NotificationManager

    private val showDNDPermissionRequiredDialog: MutableState<Boolean> = mutableStateOf(false)
    private val canShowNotificationOptions: MutableState<Boolean> = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkDNDAndPostNotificationsPermissionRequestIfNeeded()

        setContent {
            CriticalNotificationsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShowAlertOptions(canShowNotificationOptions)
                    ShowDndPermissionRequiredDialog(this, showDNDPermissionRequiredDialog)
                }
            }
        }
    }

    private fun checkDNDAndPostNotificationsPermissionRequestIfNeeded() {
        if (isAndroid13OrAbove()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PERMISSION_GRANTED) {
                setPermissionsStatus()
            } else {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), postNotificationsRequestCode)
            }
        } else {
            setPermissionsStatus()
        }
    }

    private fun isAndroid13OrAbove() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private fun setPermissionsStatus() {
        getNotificationManager().isNotificationPolicyAccessGranted.let {dndPermissionGranted ->
            canShowNotificationOptions.value = dndPermissionGranted
            showDNDPermissionRequiredDialog.value = !dndPermissionGranted
        }
    }

    override fun onResume() {
        super.onResume()

        setPermissionsStatus()
    }

    private fun getNotificationManager(): NotificationManager {
        if (!::notificationManager.isInitialized) {
            notificationManager = getSystemService(NotificationManager::class.java)
        }
        return notificationManager
    }

    fun openDNDPermissionSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        ActivityCompat.startActivity(this, intent, Bundle())
    }
}

@Composable
fun ShowDndPermissionRequiredDialog(mainActivity: MainActivity, showDNDPermissionRequiredDialog: MutableState<Boolean>) {
    if (showDNDPermissionRequiredDialog.value) {
        Dialog (onDismissRequest = { }) {
            Column (modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Text("Critical Notifications requires DND permission to work\n To grant DND permission, tap on 'Open settings' > Allow Do Not Disturb access to 'Critical Alerts' app")
                Button(onClick = {
                    mainActivity.openDNDPermissionSettings()
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Open settings")
                }
            }
        }
    }
}

@Composable
fun ShowAlertOptions(canShowNotificationOptions: MutableState<Boolean>) {
    if (canShowNotificationOptions.value) {
        Column (
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { NotificationNotifier.postCriticalNotification(0.8f) }) {
                Text("Critical alert notification")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { NotificationNotifier.postNormalNotification() }) {
                Text("Normal alert notification")
            }
        }
    }
}

