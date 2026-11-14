package com.freelauncher.app.data.service

import com.freelauncher.app.data.models.AppItem
import java.util.Locale

object InstalledAppMatcher {

    /**
     * Filters and ranks installed apps based on a recognized search query string.
     * Supports exact match, prefix match, substring match, and monogram/initials match.
     */
    fun matchApps(
        installedApps: List<AppItem>,
        query: String,
        limit: Int = 100,
        isScribbleSearch: Boolean = false
    ): List<AppItem> {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        if (cleanQuery.isBlank()) return emptyList()

        // Single vertical stroke candidate expansion for 'i' vs 'l' vs '1'
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

        data class ScoredApp(val app: AppItem, val score: Int)

        val scoredList = mutableListOf<ScoredApp>()

        for (app in installedApps) {
            val label = app.label.lowercase(Locale.ROOT).trim()
            val packageName = app.packageName.lowercase(Locale.ROOT).trim()
            var bestScore = -1

            for (q in queriesToTest) {
                var score = -1
                if (isScribbleSearch) {
                    // Strictly match apps starting with the scribble query letter
                    if (label.startsWith(q)) {
                        score = 800 - label.length
                    }
                } else {
                    if (label == q) {
                        score = 1000
                    } else if (label.startsWith(q)) {
                        score = 800 - label.length
                    } else if (label.split(" ", "-", "_", ".").any { word -> word.startsWith(q) }) {
                        score = 600 - label.length
                    } else if (label.contains(q)) {
                        score = 400 - label.indexOf(q)
                    } else if (getInitials(label) == q) {
                        score = 300
                    } else if (packageName.contains(q)) {
                        score = 100
                    }
                }

                if (score > bestScore) {
                    bestScore = score
                }
            }

            if (bestScore > 0) {
                scoredList.add(ScoredApp(app, bestScore))
            }
        }

        return scoredList
            .sortedByDescending { it.score }
            .map { it.app }
            .distinctBy { it.id.ifBlank { it.packageName } }
            .take(limit)
    }

    private fun getInitials(label: String): String {
        val parts = label.split(" ", "-", "_").filter { it.isNotBlank() }
        if (parts.size <= 1) return ""
        return parts.mapNotNull { it.firstOrNull()?.lowercaseChar() }.joinToString("")
    }
}
