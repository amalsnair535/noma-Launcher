package com.freelauncher.app.data.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NomaNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Triggered when a notification is posted
    }

    companion object {
        var instance: NomaNotificationListenerService? = null
            private set

        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, NomaNotificationListenerService::class.java)
        }

        fun isPermissionGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
            val myComponent = getComponentName(context).flattenToString()
            return enabledListeners.contains(myComponent)
        }
    }
}
