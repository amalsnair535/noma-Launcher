package com.freelauncher.app.ui.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

object SafeCrashlytics {
    private const val TAG = "SafeCrashlytics"

    fun logException(context: Context?, throwable: Throwable) {
        try {
            if (context != null && FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseCrashlytics.getInstance().recordException(throwable)
            } else {
                Log.w(TAG, "Firebase not initialized. Suppressed Crashlytics record.", throwable)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not record exception to Crashlytics: ${e.message}", throwable)
        }
    }

    fun logException(throwable: Throwable) {
        try {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Could not record exception to Crashlytics: ${e.message}", throwable)
        }
    }
}
