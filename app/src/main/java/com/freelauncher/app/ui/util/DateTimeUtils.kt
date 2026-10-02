package com.freelauncher.app.ui.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Central utility for date and time formatting to avoid redundant SimpleDateFormat creation.
 */
object DateTimeUtils {

    fun format(date: Date, pattern: String, locale: Locale = Locale.getDefault()): String {
        return getFormatter(pattern, locale).format(date)
    }

    /**
     * Patterns used across the app.
     */
    object Patterns {
        const val DATE_ISO = "yyyy-MM-dd"
        const val TIME_HM = "h:mm"
        const val TIME_HMS = "h:mm:ss"
        const val TIME_HH_MM = "hh:mm"
        const val TIME_HOUR = "h"
        const val TIME_HOUR_24 = "hh"
        const val TIME_MINUTE = "mm"
        const val TIME_HM_A = "h:mm a"
        const val DATE_EEE_MMM_D = "EEE, MMM d"
        const val DATE_EEEE_MMMM_D = "EEEE, MMMM d"
        const val TIME_HH_SPACE_MM = "hh : mm"
        const val TIME_AM_PM = "a"
        const val DATE_MMM_D_HM_A = "MMM d, h:mm a"
        const val DAY_LETTER = "EEEEE"
        const val DAY_NAME = "EEEE"
    }

    private val formatters = mutableMapOf<String, SimpleDateFormat>()

    private fun getFormatter(pattern: String, locale: Locale): SimpleDateFormat {
        val key = "$pattern-$locale"
        return formatters.getOrPut(key) {
            SimpleDateFormat(pattern, locale)
        }
    }
}
