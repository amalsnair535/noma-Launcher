package com.freelauncher.app

import android.app.Application
import timber.log.Timber

class LauncherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
