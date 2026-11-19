package com.freelauncher.app.data.service

import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.ui.util.AppMatchResult
import java.util.Locale

object InstalledAppMatcher {

    /**
     * Filters and ranks installed apps based on a recognized search query string,
     * returning [AppMatchResult] objects containing highlighting indices.
     */
    fun matchAppsWithHighlight(
        installedApps: List<AppItem>,
        query: String,
        limit: Int = 100,
        isScribbleSearch: Boolean = false
    ): List<AppMatchResult> {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        if (cleanQuery.isBlank()) return emptyList()

        val queriesToTest = if (isScribbleSearch) {
            when (cleanQuery) {
                "i" -> listOf("i")
                "l" -> listOf("l")
                "1", "|" -> listOf("i", "l")
                else -> listOf(cleanQuery)
            }
        } else {
            if (cleanQuery == "l" || cleanQuery == "1" || cleanQuery == "i") {
                listOf("i", "l")
            } else {
                listOf(cleanQuery)
            }
        }

        val scoredList = mutableListOf<AppMatchResult>()

        for (app in installedApps) {
            val labelLower = app.label.lowercase(Locale.ROOT).trim()
            val packageName = app.packageName.lowercase(Locale.ROOT).trim()
            var bestScore = -1
            var bestHighlightStart = -1
            var bestHighlightEnd = -1

            for (q in queriesToTest) {
                var score = -1
                var hStart = -1
                var hEnd = -1

                if (isScribbleSearch) {
                    // Strictly match apps whose label starts with the scribble letter (or a word boundary starting with the letter)
                    if (labelLower.startsWith(q)) {
                        score = 800 - labelLower.length
                        hStart = 0
                        hEnd = q.length
                    } else if (labelLower.split(" ", "-", "_", ".").any { word -> word.startsWith(q) }) {
                        val idx = labelLower.indexOf(q)
                        if (idx >= 0) {
                            score = 600 - labelLower.length
                            hStart = idx
                            hEnd = idx + q.length
                        }
                    }
                } else {
                    if (labelLower == q) {
                        score = 1000
                        hStart = 0
                        hEnd = app.label.length
                    } else if (labelLower.startsWith(q)) {
                        score = 800 - labelLower.length
                        hStart = 0
                        hEnd = q.length
                    } else if (labelLower.split(" ", "-", "_", ".").any { word -> word.startsWith(q) }) {
                        val idx = labelLower.indexOf(q)
                        if (idx >= 0) {
                            score = 600 - labelLower.length
                            hStart = idx
                            hEnd = idx + q.length
                        }
                    } else if (getInitials(labelLower) == q) {
                        score = 500
                        hStart = 0
                        hEnd = q.length
                    } else if (labelLower.contains(q)) {
                        val idx = labelLower.indexOf(q)
                        score = 400 - idx
                        hStart = idx
                        hEnd = idx + q.length
                    } else if (packageName.contains(q)) {
                        score = 100
                    }
                }

                if (score > bestScore) {
                    bestScore = score
                    bestHighlightStart = hStart
                    bestHighlightEnd = hEnd
                }
            }

            if (bestScore > 0) {
                scoredList.add(
                    AppMatchResult(
                        app = app,
                        highlightStart = bestHighlightStart,
                        highlightEnd = bestHighlightEnd,
                        score = bestScore
                    )
                )
            }
        }

        return scoredList
            .sortedByDescending { it.score }
            .distinctBy { it.app.id.ifBlank { it.app.packageName } }
            .take(limit)
    }

    /**
     * Legacy wrapper returning list of AppItems directly.
     */
    fun matchApps(
        installedApps: List<AppItem>,
        query: String,
        limit: Int = 100,
        isScribbleSearch: Boolean = false
    ): List<AppItem> {
        return matchAppsWithHighlight(installedApps, query, limit, isScribbleSearch)
            .map { it.app }
    }

    private fun getInitials(label: String): String {
        val parts = label.split(" ", "-", "_").filter { it.isNotBlank() }
        if (parts.size <= 1) return ""
        return parts.mapNotNull { it.firstOrNull()?.lowercaseChar() }.joinToString("")
    }
}
