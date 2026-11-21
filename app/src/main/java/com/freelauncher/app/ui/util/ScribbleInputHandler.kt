package com.freelauncher.app.ui.util

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Custom Compose pointer input modifier to handle handwriting strokes and gestures anywhere on the screen.
 * Consumes drag events during stroke drawing to prevent scroll interference, while allowing
 * clean vertical scrolling on the result list when intended.
 */
@Composable
fun Modifier.scribbleInputHandler(
    enabled: Boolean,
    screenWidthPx: Float,
    isResultViewActive: Boolean = false,
    onStrokeStarted: (ScribblePoint) -> Unit,
    onStrokeUpdated: (ScribblePoint) -> Unit,
    onStrokeCompleted: (List<ScribblePoint>, GesturalAction) -> Unit
): Modifier {
    if (!enabled) return this

    val currentIsResultViewActive by rememberUpdatedState(isResultViewActive)
    val currentOnStrokeStarted by rememberUpdatedState(onStrokeStarted)
    val currentOnStrokeUpdated by rememberUpdatedState(onStrokeUpdated)
    val currentOnStrokeCompleted by rememberUpdatedState(onStrokeCompleted)

    return this.pointerInput(enabled, screenWidthPx) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val strokePoints = mutableListOf<ScribblePoint>()
                val startPt = ScribblePoint(down.position.x, down.position.y)
                strokePoints.add(startPt)

                val pointerId = down.id
                var lastX = down.position.x
                var lastY = down.position.y
                var isStrokeActive = false
                var isScrollActive = false

                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val dragChange = event.changes.find { it.id == pointerId } ?: break

                    if (dragChange.pressed) {
                        val currentX = dragChange.position.x
                        val currentY = dragChange.position.y
                        val deltaX = currentX - startPt.x
                        val deltaY = currentY - startPt.y
                        val totalDist = hypot(deltaX, deltaY)

                        if (currentIsResultViewActive && !isStrokeActive && totalDist > 16f) {
                            val isLinearVerticalDrag = abs(deltaY) > abs(deltaX) * 2.2f
                            if (isLinearVerticalDrag && dragChange.isConsumed) {
                                isScrollActive = true
                                break
                            }
                        }

                        val dist = hypot(currentX - lastX, currentY - lastY)
                        if (totalDist > 28f || isStrokeActive) {
                            if (dist > 6f) {
                                if (!isStrokeActive) {
                                    isStrokeActive = true
                                    currentOnStrokeStarted(startPt)
                                }
                                dragChange.consume()
                                val pt = ScribblePoint(currentX, currentY)
                                strokePoints.add(pt)
                                currentOnStrokeUpdated(pt)
                                lastX = currentX
                                lastY = currentY
                            }
                        }
                    } else {
                        break
                    }
                }

                if (!isScrollActive && strokePoints.size > 1) {
                    val completedPoints = strokePoints.toList()
                    val action = ScribbleGestureCoordinator.analyzeStroke(
                        points = completedPoints,
                        screenWidthPx = screenWidthPx
                    )
                    currentOnStrokeCompleted(completedPoints, action)
                } else if (!isScrollActive) {
                    currentOnStrokeCompleted(emptyList(), GesturalAction.NONE)
                }
            }
        }
    }
}
