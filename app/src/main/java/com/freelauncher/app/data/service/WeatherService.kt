package com.freelauncher.app.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

data class WeatherInfo(
    val tempC: Float,
    val tempF: Float,
    val condition: String,
    val isDay: Boolean = true
)

class WeatherService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun fetchCurrentWeather(): WeatherInfo? = withContext(Dispatchers.IO) {
        try {
            // Step 1: Obtain location via IP Geolocation (No location permission needed)
            var lat = 37.7749
            var lon = -122.4194

            try {
                val ipRequest = Request.Builder()
                    .url("https://ipapi.co/json/")
                    .header("User-Agent", "FREE-Launcher/1.0")
                    .build()
                client.newCall(ipRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            if (json.has("latitude") && json.has("longitude")) {
                                lat = json.getDouble("latitude")
                                lon = json.getDouble("longitude")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w("IP Geolocation fallback used: ${e.message}")
            }

            // Step 2: Fetch weather from Open-Meteo API
            val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,is_day"
            val weatherRequest = Request.Builder()
                .url(weatherUrl)
                .header("User-Agent", "FREE-Launcher/1.0")
                .build()

            client.newCall(weatherRequest).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val current = json.optJSONObject("current") ?: return@withContext null

                val tempC = current.optDouble("temperature_2m", 20.0).toFloat()
                val weatherCode = current.optInt("weather_code", 0)
                val isDay = current.optInt("is_day", 1) == 1

                val tempF = (tempC * 9f / 5f) + 32f
                val condition = getWeatherConditionText(weatherCode)

                return@withContext WeatherInfo(
                    tempC = tempC,
                    tempF = tempF,
                    condition = condition,
                    isDay = isDay
                )
            }
        } catch (e: Exception) {
            Timber.e(e)
            return@withContext null
        }
    }

    private fun getWeatherConditionText(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1 -> "Mostly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Clear"
        }
    }
}
