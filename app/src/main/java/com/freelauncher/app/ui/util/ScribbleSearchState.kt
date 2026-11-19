package com.freelauncher.app.ui.util

import com.freelauncher.app.data.models.AppItem

data class AppMatchResult(
    val app: AppItem,
    val highlightStart: Int = -1,
    val highlightEnd: Int = -1,
    val score: Int = 0
)

data class ScribbleSearchState(
    val strokes: List<ScribbleStroke> = emptyList(),
    val currentStroke: ScribbleStroke? = null,
    val recognizedQuery: String = "",
    val matchingApps: List<AppMatchResult> = emptyList(),
    val isRecognizing: Boolean = false,
    val requestId: Long = 0L
) {
    val isActive: Boolean
        get() = recognizedQuery.isNotBlank()

    val isDrawing: Boolean
        get() = currentStroke != null || strokes.isNotEmpty()
}
