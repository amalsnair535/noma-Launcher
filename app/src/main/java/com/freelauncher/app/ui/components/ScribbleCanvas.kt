package com.freelauncher.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.freelauncher.app.ui.util.ScribblePoint
import com.freelauncher.app.ui.util.ScribbleStroke

@Composable
fun ScribbleCanvas(
    strokes: List<ScribbleStroke>,
    currentStroke: ScribbleStroke?,
    modifier: Modifier = Modifier,
    strokeColor: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 5.dp
) {
    val allStrokes = if (currentStroke != null) strokes + currentStroke else strokes

    Canvas(modifier = modifier.fillMaxSize()) {
        val widthPx = strokeWidth.toPx()

        for (stroke in allStrokes) {
            val points = stroke.points
            if (points.size < 2) continue

            val path = createSmoothPath(points)
            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(
                    width = widthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Creates a smooth quadratic Bezier curve path through points.
 */
private fun createSmoothPath(points: List<ScribblePoint>): Path {
    val path = Path()
    if (points.isEmpty()) return path

    val first = points.first()
    path.moveTo(first.x, first.y)

    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    for (i in 1 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        val midX = (p1.x + p2.x) / 2f
        val midY = (p1.y + p2.y) / 2f
        path.quadraticTo(p1.x, p1.y, midX, midY)
    }

    val last = points.last()
    path.lineTo(last.x, last.y)
    return path
}
