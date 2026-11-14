package com.freelauncher.app.ui.util

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

data class ScribblePoint(
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class ScribbleStroke(
    val points: List<ScribblePoint>
)

enum class GesturalAction {
    NONE,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    HANDWRITING_STROKE
}

object ScribbleGestureCoordinator {

    /**
     * Evaluates a completed touch movement path to distinguish between
     * a fast directional system swipe vs. an intentional handwriting stroke.
     */
    fun analyzeStroke(
        points: List<ScribblePoint>,
        screenWidthPx: Float,
        edgeMarginPx: Float = 64f,
        swipeThresholdPx: Float = 100f
    ): GesturalAction {
        if (points.size < 2) return GesturalAction.NONE

        val start = points.first()
        val end = points.last()

        val deltaX = end.x - start.x
        val deltaY = end.y - start.y
        val totalDistance = hypot(deltaX, deltaY)

        val durationMs = (end.timestamp - start.timestamp).coerceAtLeast(1L)
        val velocity = totalDistance / durationMs

        // Calculate path straightness / linearity:
        var pathLength = 0f
        var directionChanges = 0
        var prevAngle: Float? = null

        for (i in 1 until points.size) {
            val p1 = points[i - 1]
            val p2 = points[i]
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            pathLength += hypot(dx, dy)

            if (hypot(dx, dy) > 10f) {
                val currentAngle = atan2(dy, dx)
                if (prevAngle != null) {
                    val angleDiff = abs(currentAngle - prevAngle)
                    if (angleDiff > Math.PI / 3) { // > 60 degree turn
                        directionChanges++
                    }
                }
                prevAngle = currentAngle
            }
        }

        val straightnessRatio = if (pathLength > 0) totalDistance / pathLength else 0f

        // System Swipe Determination:
        val isLinearMovement = straightnessRatio > 0.80f && directionChanges <= 1
        val isHorizontalSwipeZone = start.x < (screenWidthPx * 0.38f) || start.x > (screenWidthPx * 0.62f)

        if (isLinearMovement) {
            if (abs(deltaX) > abs(deltaY) * 1.2f) {
                // Horizontal Swipes (RSS Feed / Time Away)
                if (isHorizontalSwipeZone && totalDistance > swipeThresholdPx && velocity > 0.5f) {
                    if (deltaX < -swipeThresholdPx) return GesturalAction.SWIPE_LEFT
                    if (deltaX > swipeThresholdPx) return GesturalAction.SWIPE_RIGHT
                }
            } else if (abs(deltaY) > abs(deltaX) * 1.2f) {
                // Vertical Swipes (Six Apps / Search)
                if (deltaY < -swipeThresholdPx && velocity > 0.75f && start.y > 400f) {
                    return GesturalAction.SWIPE_UP
                }
                // Swipe Down: Only if started near top of screen (start.y < 300px) and fast
                if (deltaY > swipeThresholdPx && start.y < 300f && velocity > 0.75f) {
                    return GesturalAction.SWIPE_DOWN
                }
            }
        }

        // Handwriting Detection:
        // Any stroke that has significant length (> 15px) or vertical/horizontal handwriting shapes
        if (pathLength > 15f) {
            return GesturalAction.HANDWRITING_STROKE
        }

        return GesturalAction.NONE
    }
}
