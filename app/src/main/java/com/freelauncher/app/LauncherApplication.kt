package com.freelauncher.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class LauncherApplication : Application() {

    companion object {
        private const val TAG = "LauncherApplication"
    }

    override fun onCreate() {
        super.onCreate()
        initFirebaseSafely()
        setupGlobalExceptionHandler()
    }

    private fun initFirebaseSafely() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization warning: ${e.message}")
        }
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
