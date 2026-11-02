package com.freelauncher.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import com.freelauncher.app.ui.util.DateTimeUtils
import java.util.*
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect

enum class ClockStyle(val id: String, val displayName: String) {
    LARGE_DIGITAL("large_digital", "Large Digital"),
    THIN_DIGITAL("thin_digital", "Thin Digital"),
    MONOSPACED("monospaced", "Monospaced [10:16]"),
    MINIMAL_STACKED("minimal_stacked", "Minimal Stacked"),
    WORD_BASED("word_based", "Word-Based Clock"),
    COMPACT("compact", "Compact Digital"),
    ELEGANT_SERIF("serif", "Elegant Serif"),
    DOT_BASED("dot_based", "Minimal Dot-Based"),
    HANDWRITTEN("handwritten", "Handwritten"),
    OUTLINE("outline", "Modern Outline"),
    BOLD_BLOCK("bold_block", "Bold Block"),
    MINIMAL_SECONDS("minimal_seconds", "Minimal w/ Seconds"),
    BUBBLY_3D("bubbly_3d", "Bubbly 3D"),
    FLIP("flip", "Flip Split-Flap"),
    TIMELINE("timeline", "Timeline Progress"),
    ROTATING_WHEELS("rotating_wheels", "Rotating Wheels")
}

enum class TimeCardVerticalAlign(val id: String, val displayName: String) {
    TOP("top", "Top"),
    CENTER("center", "Center"),
    BOTTOM("bottom", "Bottom")
}

enum class TimeCardHorizontalAlign(val id: String, val displayName: String) {
    LEFT("left", "Left"),
    CENTER("center", "Center"),
    RIGHT("right", "Right")
}

@Composable
fun MinimalistClock(
    timeFlow: kotlinx.coroutines.flow.StateFlow<Date>,
    clockStyle: ClockStyle,
    modifier: Modifier = Modifier
) {
    val currentTime by timeFlow.collectAsStateWithLifecycle()
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = clockStyle,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "clock_style_anim"
        ) { targetStyle ->
            when (targetStyle) {
                ClockStyle.LARGE_DIGITAL -> LargeDigitalClock(currentTime)
                ClockStyle.THIN_DIGITAL -> ThinDigitalClock(currentTime)
                ClockStyle.MONOSPACED -> MonospacedClock(currentTime)
                ClockStyle.MINIMAL_STACKED -> MinimalStackedClock(currentTime)
                ClockStyle.WORD_BASED -> WordBasedClock(currentTime)
                ClockStyle.COMPACT -> CompactClock(currentTime)
                ClockStyle.ELEGANT_SERIF -> ElegantSerifClock(currentTime)
                ClockStyle.DOT_BASED -> MinimalDotBasedClock(currentTime)
                ClockStyle.HANDWRITTEN -> HandwrittenClock(currentTime)
                ClockStyle.OUTLINE -> OutlineClock(currentTime)
                ClockStyle.BOLD_BLOCK -> BoldBlockClock(currentTime)
                ClockStyle.MINIMAL_SECONDS -> MinimalSecondsClock(currentTime)
                ClockStyle.BUBBLY_3D -> Bubbly3DClock(currentTime)
                ClockStyle.FLIP -> FlipClock(currentTime)
                ClockStyle.TIMELINE -> TimelineClock(currentTime)
                ClockStyle.ROTATING_WHEELS -> RotatingWheelsClock(currentTime)
            }
        }
    }
}

@Composable
fun Bubbly3DClock(time: Date) {
    val locale = LocalLocale.current.platformLocale
    val h = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HOUR, locale)
    val m = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_MINUTE, locale)

    val primaryColor = MaterialTheme.colorScheme.primary
    val containerBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        // Hour Pill
        BubblyPillDigit(text = h, containerColor = containerBg, textColor = contentColor, borderColor = primaryColor.copy(alpha = 0.4f))

        // Bubbly Colon Dots
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.6f))
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.6f))
                        )
                    )
            )
        }

        // Minute Pill
        BubblyPillDigit(text = m, containerColor = containerBg, textColor = contentColor, borderColor = primaryColor.copy(alpha = 0.4f))
    }
}

@Composable
private fun BubblyPillDigit(
    text: String,
    containerColor: Color,
    textColor: Color,
    borderColor: Color
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .shadow(6.dp, RoundedCornerShape(26.dp), spotColor = borderColor)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        containerColor.copy(alpha = 0.95f),
                        containerColor.copy(alpha = 0.70f)
                    )
                )
            )
            .border(1.5.dp, borderColor, RoundedCornerShape(26.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            ),
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HandwrittenClock(time: Date) {
    val t = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HM, LocalLocale.current.platformLocale)
    Text(
        text = t,
        style = MaterialTheme.typography.displayLarge.copy(
            fontFamily = FontFamily.Cursive,
            fontSize = 72.sp,
            fontWeight = FontWeight.Normal
        ),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
}

@Composable
fun OutlineClock(time: Date) {
    val locale = LocalLocale.current.platformLocale
    val t = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HM, locale)
    val strokeColor = MaterialTheme.colorScheme.onBackground
    val density = androidx.compose.ui.platform.LocalDensity.current

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .width(260.dp)
            .height(84.dp)
    ) {
        drawIntoCanvas { canvas ->
            val nativePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = with(density) { 3.dp.toPx() }
                color = strokeColor.toArgb()
                textSize = with(density) { 68.sp.toPx() }
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
            }

            val x = size.width / 2f
            val y = (size.height / 2f) - ((nativePaint.descent() + nativePaint.ascent()) / 2f)
            canvas.nativeCanvas.drawText(t, x, y, nativePaint)
        }
    }
}

@Composable
fun FlipClock(time: Date) {
    val locale = LocalLocale.current.platformLocale
    val h = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HOUR, locale)
    val m = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_MINUTE, locale)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        FlipCardGroup(text = h)

        // Seam Colon Separator
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground)
            )
        }

        FlipCardGroup(text = m)
    }
}

@Composable
private fun FlipCardGroup(text: String) {
    val digits = text.padStart(2, '0')
    val containerBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        digits.forEach { digitChar ->
            SingleFlipDigitCard(
                digit = digitChar.toString(),
                containerBg = containerBg,
                contentColor = contentColor,
                borderColor = borderColor
            )
        }
    }
}

@Composable
private fun SingleFlipDigitCard(
    digit: String,
    containerBg: Color,
    contentColor: Color,
    borderColor: Color
) {
    var previousDigit by remember { mutableStateOf(digit) }
    var currentDigit by remember { mutableStateOf(digit) }
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(digit) {
        if (digit != currentDigit) {
            previousDigit = currentDigit
            currentDigit = digit
            rotation.snapTo(0f)
            rotation.animateTo(
                targetValue = 180f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        }
    }

    val displayDigit = if (rotation.value < 90f) previousDigit else currentDigit

    Box(
        modifier = Modifier
            .width(42.dp)
            .height(58.dp)
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayDigit,
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            ),
            color = contentColor,
            textAlign = TextAlign.Center
        )

        // Seam Line splitting top and bottom half
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun TimelineClock(time: Date) {
    val locale = LocalLocale.current.platformLocale
    val t = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HM_A, locale)
    val d = DateTimeUtils.format(time, DateTimeUtils.Patterns.DATE_EEE_MMM_D, locale)

    val cal = remember(time) { Calendar.getInstance().apply { this.time = time } }
    val secondOfDay = cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND)
    val dayProgress = (secondOfDay / 86400f).coerceIn(0f, 1f)

    val minuteOfHour = cal.get(Calendar.MINUTE)
    val hourProgress = ((minuteOfHour * 60 + cal.get(Calendar.SECOND)) / 3600f).coerceIn(0f, 1f)

    val secondOfMinute = cal.get(Calendar.SECOND)
    val minuteProgress = (secondOfMinute / 60f).coerceIn(0f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = t,
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = d.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            ),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Progress Bar 1: DAY
        TimelineProgressBarRow(label = "DAY", progress = dayProgress)
        Spacer(modifier = Modifier.height(6.dp))

        // Progress Bar 2: HOUR
        TimelineProgressBarRow(label = "HOUR", progress = hourProgress)
        Spacer(modifier = Modifier.height(6.dp))

        // Progress Bar 3: MINUTE
        TimelineProgressBarRow(label = "MINUTE", progress = minuteProgress)
    }
}

@Composable
private fun TimelineProgressBarRow(label: String, progress: Float) {
    val trackBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val progressColor = MaterialTheme.colorScheme.primary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.width(220.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(46.dp)
        )

        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
        ) {
            val width = size.width
            val height = size.height
            val strokeCap = androidx.compose.ui.graphics.StrokeCap.Round

            // Draw track
            drawLine(
                color = trackBg,
                start = androidx.compose.ui.geometry.Offset(0f, height / 2),
                end = androidx.compose.ui.geometry.Offset(width, height / 2),
                strokeWidth = height,
                cap = strokeCap
            )

            // Draw filled progress
            if (progress > 0f) {
                drawLine(
                    color = progressColor,
                    start = Offset(0f, height / 2),
                    end = Offset(width * progress, height / 2),
                    strokeWidth = height,
                    cap = strokeCap
                )
            }
        }
    }
}

@Composable
fun RotatingWheelsClock(time: Date) {
    val locale = LocalLocale.current.platformLocale
    val cal = remember(time) { Calendar.getInstance().apply { this.time = time } }
    val hourStr = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HOUR_24, locale)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)

    // Animated rotation angles for smooth wheel turning
    val minuteAngleTarget = - (minute * 6f)
    val secondAngleTarget = - (second * 6f)

    val animatedMinuteAngle by animateFloatAsState(
        targetValue = minuteAngleTarget,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "min_wheel_anim"
    )
    val animatedSecondAngle by animateFloatAsState(
        targetValue = secondAngleTarget,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "sec_wheel_anim"
    )

    val onSurfaceColor = MaterialTheme.colorScheme.onBackground
    val trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(
        modifier = Modifier
            .size(260.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // High-precision Native Canvas Drawing for concentric wheels, ticks, numbers & indicator
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val centerPt = Offset(size.width / 2f, size.height / 2f)
            val innerNumRadius = size.width * 0.23f
            val tickRadius = size.width * 0.33f
            val outerNumRadius = size.width * 0.41f
            val outerTicksRadius = size.width * 0.48f

            val nativeTextPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                color = onSurfaceColor.toArgb()
                textSize = with(density) { 13.sp.toPx() }
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
            }

            drawIntoCanvas { canvas ->
                // 1. Draw Inner Wheel Numbers (Minutes: 00, 05, 10 ... 55)
                for (i in 0 until 12) {
                    val valNum = i * 5
                    val valStr = valNum.toString().padStart(2, '0')
                    val angleDeg = (i * 30f) + animatedMinuteAngle
                    val rad = Math.toRadians(angleDeg.toDouble())

                    val x = centerPt.x + (innerNumRadius * Math.cos(rad)).toFloat()
                    val y = centerPt.y + (innerNumRadius * Math.sin(rad)).toFloat() - ((nativeTextPaint.descent() + nativeTextPaint.ascent()) / 2f)
                    canvas.nativeCanvas.drawText(valStr, x, y, nativeTextPaint)
                }

                // 2. Draw Outer Wheel Numbers (Seconds: 00, 05, 10 ... 55)
                for (i in 0 until 12) {
                    val valNum = i * 5
                    val valStr = valNum.toString().padStart(2, '0')
                    val angleDeg = (i * 30f) + animatedSecondAngle
                    val rad = Math.toRadians(angleDeg.toDouble())

                    val x = centerPt.x + (outerNumRadius * Math.cos(rad)).toFloat()
                    val y = centerPt.y + (outerNumRadius * Math.sin(rad)).toFloat() - ((nativeTextPaint.descent() + nativeTextPaint.ascent()) / 2f)
                    canvas.nativeCanvas.drawText(valStr, x, y, nativeTextPaint)
                }
            }

            // 3. Draw Concentric Seam Ring Ticks (60 ticks for Minutes)
            for (i in 0 until 60) {
                val tickAngleDeg = (i * 6f) + animatedMinuteAngle
                val rad = Math.toRadians(tickAngleDeg.toDouble())
                val isMajor = i % 5 == 0
                val tickLength = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                val strokeW = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                val startX = centerPt.x + (tickRadius * Math.cos(rad)).toFloat()
                val startY = centerPt.y + (tickRadius * Math.sin(rad)).toFloat()
                val endX = centerPt.x + ((tickRadius + tickLength) * Math.cos(rad)).toFloat()
                val endY = centerPt.y + ((tickRadius + tickLength) * Math.sin(rad)).toFloat()

                drawLine(
                    color = if (isMajor) onSurfaceColor else trackColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeW
                )
            }

            // 4. Draw Outer Ring Ticks (60 ticks for Seconds)
            for (i in 0 until 60) {
                val tickAngleDeg = (i * 6f) + animatedSecondAngle
                val rad = Math.toRadians(tickAngleDeg.toDouble())
                val isMajor = i % 5 == 0
                val tickLength = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                val strokeW = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                val startX = centerPt.x + (outerTicksRadius * Math.cos(rad)).toFloat()
                val startY = centerPt.y + (outerTicksRadius * Math.sin(rad)).toFloat()
                val endX = centerPt.x + ((outerTicksRadius - tickLength) * Math.cos(rad)).toFloat()
                val endY = centerPt.y + ((outerTicksRadius - tickLength) * Math.sin(rad)).toFloat()

                drawLine(
                    color = if (isMajor) onSurfaceColor else trackColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeW
                )
            }

            // 5. Draw 3 o'clock Selection Capsule Indicator Frame
            val capsuleLeft = centerPt.x + (innerNumRadius * 0.70f)
            val capsuleTop = centerPt.y - 18.dp.toPx()
            val capsuleRight = centerPt.x + (outerNumRadius * 1.15f)
            val capsuleBottom = centerPt.y + 18.dp.toPx()

            drawRoundRect(
                color = onSurfaceColor,
                topLeft = Offset(capsuleLeft, capsuleTop),
                size = Size(capsuleRight - capsuleLeft, capsuleBottom - capsuleTop),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center Bold Hour Glyph
        Text(
            text = hourStr,
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            ),
            color = onSurfaceColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BoldBlockClock(time: Date) {
    val t = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HM, LocalLocale.current.platformLocale)
    Text(
        text = t,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 84.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-4).sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
}

@Composable
fun MinimalSecondsClock(time: Date) {
    val locale = LocalLocale.current.platformLocale
    val hm = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HM, locale)
    val s = DateTimeUtils.format(time, "ss", locale)

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = hm,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 64.sp,
                fontWeight = FontWeight.Light
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = s,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraLight
            ),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
    }
}

@Composable
fun LargeDigitalClock(time: Date) {
    val formatted = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HM, LocalLocale.current.platformLocale)
    Text(
        text = formatted,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 72.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
}

@Composable
fun ThinDigitalClock(time: Date) {
    val formatted = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HM, LocalLocale.current.platformLocale)
    Text(
        text = formatted,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 76.sp,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = 2.sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
}

@Composable
fun MonospacedClock(time: Date) {
    val formatted = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HMS, LocalLocale.current.platformLocale)
    Text(
        text = "[ $formatted ]",
        style = MaterialTheme.typography.headlineLarge.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 32.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 3.sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
}

@Composable
fun MinimalStackedClock(time: Date) {
    val locale = LocalLocale.current.platformLocale
    val h = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HOUR, locale)
    val m = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_MINUTE, locale)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = h,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 68.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 64.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = m,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 68.sp,
                fontWeight = FontWeight.Light,
                lineHeight = 64.sp
            ),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WordBasedClock(time: Date) {
    val cal = Calendar.getInstance().apply { timeZone = TimeZone.getDefault(); this.time = time }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val wordText = formatTimeToWords(hour, minute)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Text(
            text = wordText.first,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Light,
                letterSpacing = 1.5.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        if (wordText.second.isNotEmpty()) {
            Text(
                text = wordText.second,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CompactClock(time: Date) {
    val locale = LocalLocale.current.platformLocale
    val t = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HM_A, locale)
    val d = DateTimeUtils.format(time, DateTimeUtils.Patterns.DATE_EEE_MMM_D, locale)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = t.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = " • ",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = d,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Light
            ),
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun ElegantSerifClock(time: Date) {
    val locale = LocalLocale.current.platformLocale
    val t = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HH_SPACE_MM, locale)
    val a = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_AM_PM, locale)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = t,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.Serif,
                fontSize = 64.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = a.lowercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Serif,
                letterSpacing = 3.sp
            ),
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun MinimalDotBasedClock(time: Date) {
    val locale = LocalLocale.current.platformLocale
    val h = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_HOUR, locale)
    val m = DateTimeUtils.format(time, DateTimeUtils.Patterns.TIME_MINUTE, locale)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$h $m",
                style = MaterialTheme.typography.displayMedium.copy(
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Light
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        // Dot indicator row representing quarters of the hour
        val cal = Calendar.getInstance().apply { this.time = time }
        val currentMin = cal.get(Calendar.MINUTE)
        val activeQuarter = currentMin / 15

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0..3) {
                Box(
                    modifier = Modifier
                        .size(if (i == activeQuarter) 6.dp else 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (i <= activeQuarter) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

private fun formatTimeToWords(hour: Int, minute: Int): Pair<String, String> {
    val hoursArray = arrayOf(
        "Twelve", "One", "Two", "Three", "Four", "Five",
        "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve"
    )
    val displayHour = hoursArray[hour % 12]
    val nextHour = hoursArray[(hour + 1) % 12]

    return when (minute) {
        0 -> Pair(displayHour, "O'CLOCK")
        15 -> Pair("Quarter Past", displayHour.uppercase(Locale.ROOT))
        30 -> Pair("Half Past", displayHour.uppercase(Locale.ROOT))
        45 -> Pair("Quarter To", nextHour.uppercase(Locale.ROOT))
        in 1..9 -> Pair(displayHour, "OH " + getNumberWord(minute).uppercase(Locale.ROOT))
        in 10..19 -> Pair(displayHour, getNumberWord(minute).uppercase(Locale.ROOT))
        in 20..29 -> Pair(displayHour, ("TWENTY " + getNumberWord(minute % 10)).trim().uppercase(Locale.ROOT))
        in 31..39 -> Pair(displayHour, ("THIRTY " + getNumberWord(minute % 10)).trim().uppercase(Locale.ROOT))
        in 40..49 -> Pair(displayHour, ("FORTY " + getNumberWord(minute % 10)).trim().uppercase(Locale.ROOT))
        in 50..59 -> Pair(displayHour, ("FIFTY " + getNumberWord(minute % 10)).trim().uppercase(Locale.ROOT))
        else -> Pair(displayHour, minute.toString())
    }
}

private fun getNumberWord(num: Int): String {
    return when (num) {
        0 -> ""
        1 -> "One"
        2 -> "Two"
        3 -> "Three"
        4 -> "Four"
        5 -> "Five"
        6 -> "Six"
        7 -> "Seven"
        8 -> "Eight"
        9 -> "Nine"
        10 -> "Ten"
        11 -> "Eleven"
        12 -> "Twelve"
        13 -> "Thirteen"
        14 -> "Fourteen"
        15 -> "Fifteen"
        16 -> "Sixteen"
        17 -> "Seventeen"
        18 -> "Eighteen"
        19 -> "Nineteen"
        else -> ""
    }
}

fun getContextualGreeting(cal: Calendar = Calendar.getInstance()): String {
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Stay present"
    }
}
