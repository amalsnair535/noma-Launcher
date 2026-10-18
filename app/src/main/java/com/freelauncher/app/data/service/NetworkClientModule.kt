package com.freelauncher.app.data.service

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClientModule {
    val sharedOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
