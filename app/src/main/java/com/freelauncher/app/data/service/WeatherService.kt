package com.freelauncher.app.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class WeatherInfo(
    val tempC: Float,
    val tempF: Float,
    val condition: String,
    val isDay: Boolean = true
)

class WeatherService(
    private val context: Context
) {

    companion object {
        private const val TAG = "WeatherService"

        // Cache weather for 30 minutes
        private const val WEATHER_CACHE_DURATION = 30 * 60 * 1000L

        private const val PREFS_NAME = "weather_cache"
        private const val KEY_TEMP_C = "temp_c"
        private const val KEY_TEMP_F = "temp_f"
        private const val KEY_CONDITION = "condition"
        private const val KEY_IS_DAY = "is_day"
        private const val KEY_TIMESTAMP = "timestamp"

        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_LOCATION_TIMESTAMP = "location_timestamp"

        // Cache location for 6 hours
        private const val LOCATION_CACHE_DURATION = 6 * 60 * 60 * 1000L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Main weather function.
     *
     * Priority:
     * 1. Cached weather
     * 2. Android location
     * 3. Cached location
     * 4. IP geolocation
     * 5. If everything fails -> null
     */
    suspend fun fetchCurrentWeather(
        forceRefresh: Boolean = false
    ): WeatherInfo? = withContext(Dispatchers.IO) {

        try {

            // ---------------------------------------------------------
            // STEP 1: Return cached weather if still valid
            // ---------------------------------------------------------

            if (!forceRefresh) {
                val cachedWeather = getCachedWeather()

                if (cachedWeather != null) {
                    Log.d(TAG, "Using cached weather")
                    return@withContext cachedWeather
                }
            }

            // ---------------------------------------------------------
            // STEP 2: Get location
            // ---------------------------------------------------------

            val coordinates = getLocation()

            if (coordinates == null) {
                Log.w(TAG, "Unable to determine location")
                return@withContext null
            }

            val lat = coordinates.first
            val lon = coordinates.second

            Log.d(
                TAG,
                "Using coordinates: latitude=$lat longitude=$lon"
            )

            // ---------------------------------------------------------
            // STEP 3: Fetch weather from Open-Meteo
            // ---------------------------------------------------------

            val weather = fetchWeatherFromOpenMeteo(
                latitude = lat,
                longitude = lon
            )

            // ---------------------------------------------------------
            // STEP 4: Cache result
            // ---------------------------------------------------------

            if (weather != null) {
                cacheWeather(weather)

                Log.d(
                    TAG,
                    "Weather fetched: ${weather.tempC}°C, ${weather.condition}"
                )
            }

            return@withContext weather

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Weather fetch failed",
                e
            )

            FirebaseCrashlytics.getInstance()
                .recordException(e)

            return@withContext null
        }
    }

    /**
     * Gets location using Android location services first.
     * If unavailable, falls back to cached location and finally IP geolocation.
     */
    private suspend fun getLocation(): Pair<Double, Double>? {

        // -------------------------------------------------------------
        // STEP 1: Android location
        // -------------------------------------------------------------

        if (hasLocationPermission()) {

            try {

                val location = getAndroidLocation()

                if (location != null) {

                    val lat = location.latitude
                    val lon = location.longitude

                    Log.d(
                        TAG,
                        "Android location obtained: $lat, $lon"
                    )

                    saveLocation(lat, lon)

                    return Pair(lat, lon)
                }

            } catch (e: Exception) {

                Log.w(
                    TAG,
                    "Android location failed: ${e.message}"
                )
            }
        } else {

            Log.d(
                TAG,
                "Location permission not granted"
            )
        }

        // -------------------------------------------------------------
        // STEP 2: Cached location
        // -------------------------------------------------------------

        val cachedLocation = getCachedLocation()

        if (cachedLocation != null) {

            Log.d(
                TAG,
                "Using cached location"
            )

            return cachedLocation
        }

        // -------------------------------------------------------------
        // STEP 3: IP geolocation fallback
        // -------------------------------------------------------------

        return getLocationFromIp()
    }

    /**
     * Get approximate/current location using Android's Fused Location Provider.
     */
    private suspend fun getAndroidLocation(): Location? {

        if (!hasLocationPermission()) {
            return null
        }

        return try {

            // Try last known location first.
            val lastLocation =
                fusedLocationClient.lastLocation.await()

            if (lastLocation != null) {
                Log.d(TAG, "Using last known Android location")
                return lastLocation
            }

            // If no last location exists, request a fresh location.
            val cancellationTokenSource =
                CancellationTokenSource()

            val location =
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).await()

            location

        } catch (e: SecurityException) {

            Log.w(
                TAG,
                "Location permission/security error",
                e
            )

            null

        } catch (e: Exception) {

            Log.w(
                TAG,
                "Unable to get Android location",
                e
            )

            null
        }
    }

    /**
     * Check whether the app has either coarse or fine location permission.
     */
    private fun hasLocationPermission(): Boolean {

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val fineGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return coarseGranted || fineGranted
    }

    /**
     * IP-based location fallback.
     *
     * This does NOT require Android location permission.
     */
    private fun getLocationFromIp(): Pair<Double, Double>? {

        return try {

            val request = Request.Builder()
                .url("https://ipapi.co/json/")
                .header(
                    "User-Agent",
                    "FREE-Launcher/1.0"
                )
                .build()

            client.newCall(request).execute().use { response ->

                if (!response.isSuccessful) {
                    Log.w(
                        TAG,
                        "IP geolocation failed: HTTP ${response.code}"
                    )

                    return null
                }

                val body =
                    response.body?.string()
                        ?: return null

                val json = JSONObject(body)

                val latitude =
                    json.optDouble("latitude", Double.NaN)

                val longitude =
                    json.optDouble("longitude", Double.NaN)

                if (
                    latitude.isNaN() ||
                    longitude.isNaN()
                ) {
                    Log.w(
                        TAG,
                        "IP geolocation returned invalid coordinates"
                    )

                    return null
                }

                Log.d(
                    TAG,
                    "IP location: $latitude, $longitude"
                )

                saveLocation(
                    latitude,
                    longitude
                )

                Pair(
                    latitude,
                    longitude
                )
            }

        } catch (e: Exception) {

            Log.w(
                TAG,
                "IP geolocation error: ${e.message}"
            )

            null
        }
    }

    /**
     * Fetch current weather from Open-Meteo.
     */
    private fun fetchWeatherFromOpenMeteo(
        latitude: Double,
        longitude: Double
    ): WeatherInfo? {

        return try {

            val weatherUrl =
                "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$latitude" +
                        "&longitude=$longitude" +
                        "&current=temperature_2m,weather_code,is_day" +
                        "&temperature_unit=celsius" +
                        "&timezone=auto"

            val request = Request.Builder()
                .url(weatherUrl)
                .header(
                    "User-Agent",
                    "FREE-Launcher/1.0"
                )
                .build()

            client.newCall(request).execute().use { response ->

                if (!response.isSuccessful) {

                    Log.w(
                        TAG,
                        "Open-Meteo failed: HTTP ${response.code}"
                    )

                    return null
                }

                val body =
                    response.body?.string()
                        ?: return null

                val json =
                    JSONObject(body)

                val current =
                    json.optJSONObject("current")
                        ?: return null

                val tempC =
                    current
                        .optDouble(
                            "temperature_2m",
                            Double.NaN
                        )
                        .toFloat()

                if (tempC.isNaN()) {
                    return null
                }

                val weatherCode =
                    current.optInt(
                        "weather_code",
                        0
                    )

                val isDay =
                    current.optInt(
                        "is_day",
                        1
                    ) == 1

                val tempF =
                    (tempC * 9f / 5f) + 32f

                val condition =
                    getWeatherConditionText(
                        weatherCode
                    )

                WeatherInfo(
                    tempC = tempC,
                    tempF = tempF,
                    condition = condition,
                    isDay = isDay
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Open-Meteo request failed",
                e
            )

            FirebaseCrashlytics.getInstance()
                .recordException(e)

            null
        }
    }

    /**
     * Save weather to SharedPreferences.
     */
    private fun cacheWeather(
        weather: WeatherInfo
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putFloat(
                KEY_TEMP_C,
                weather.tempC
            )
            .putFloat(
                KEY_TEMP_F,
                weather.tempF
            )
            .putString(
                KEY_CONDITION,
                weather.condition
            )
            .putBoolean(
                KEY_IS_DAY,
                weather.isDay
            )
            .putLong(
                KEY_TIMESTAMP,
                System.currentTimeMillis()
            )
            .apply()
    }

    /**
     * Return cached weather if it is still valid.
     */
    private fun getCachedWeather(): WeatherInfo? {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val timestamp =
            prefs.getLong(
                KEY_TIMESTAMP,
                0L
            )

        if (
            timestamp == 0L ||
            System.currentTimeMillis() - timestamp >
            WEATHER_CACHE_DURATION
        ) {
            return null
        }

        val condition =
            prefs.getString(
                KEY_CONDITION,
                null
            ) ?: return null

        return WeatherInfo(
            tempC = prefs.getFloat(
                KEY_TEMP_C,
                0f
            ),
            tempF = prefs.getFloat(
                KEY_TEMP_F,
                32f
            ),
            condition = condition,
            isDay = prefs.getBoolean(
                KEY_IS_DAY,
                true
            )
        )
    }

    /**
     * Save coordinates for reuse.
     */
    private fun saveLocation(
        latitude: Double,
        longitude: Double
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_LATITUDE,
                latitude.toString()
            )
            .putString(
                KEY_LONGITUDE,
                longitude.toString()
            )
            .putLong(
                KEY_LOCATION_TIMESTAMP,
                System.currentTimeMillis()
            )
            .apply()
    }

    /**
     * Return cached coordinates if they are still valid.
     */
    private fun getCachedLocation(): Pair<Double, Double>? {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val timestamp =
            prefs.getLong(
                KEY_LOCATION_TIMESTAMP,
                0L
            )

        if (
            timestamp == 0L ||
            System.currentTimeMillis() - timestamp >
            LOCATION_CACHE_DURATION
        ) {
            return null
        }

        val latitude =
            prefs.getString(
                KEY_LATITUDE,
                null
            )?.toDoubleOrNull()
                ?: return null

        val longitude =
            prefs.getString(
                KEY_LONGITUDE,
                null
            )?.toDoubleOrNull()
                ?: return null

        return Pair(
            latitude,
            longitude
        )
    }

    /**
     * Convert Open-Meteo WMO weather codes
     * into launcher-friendly text.
     */
    private fun getWeatherConditionText(
        code: Int
    ): String {

        return when (code) {

            0 ->
                "Clear"

            1 ->
                "Mostly Clear"

            2 ->
                "Partly Cloudy"

            3 ->
                "Overcast"

            45, 48 ->
                "Foggy"

            51, 53, 55 ->
                "Drizzle"

            56, 57 ->
                "Freezing Drizzle"

            61, 63, 65 ->
                "Rain"

            66, 67 ->
                "Freezing Rain"

            71, 73, 75, 77 ->
                "Snow"

            80, 81, 82 ->
                "Rain Showers"

            85, 86 ->
                "Snow Showers"

            95, 96, 99 ->
                "Thunderstorm"

            else ->
                "Clear"
        }
    }
}
