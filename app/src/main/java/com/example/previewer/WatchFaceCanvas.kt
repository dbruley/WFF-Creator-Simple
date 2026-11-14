package com.example.previewer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.BezelStyle
import com.example.model.ClockType
import com.example.model.ComplicationPosition
import com.example.model.ComplicationSlotConfig
import com.example.model.ComplicationType
import com.example.model.DialBackgroundStyle
import com.example.model.HandMotion
import com.example.model.HandStyle
import com.example.model.SimulatedDataSource
import com.example.model.WatchFaceConfig
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

data class PreviewSimulationState(
    val isAmbient: Boolean = false,
    val showGuidelines: Boolean = false,
    val simulatedHour: Int = 10,
    val simulatedMinute: Int = 8,
    val simulatedSecond: Float = 32f,
    val isLiveTime: Boolean = false,
    val selectedSlotId: Int? = null,
    val simulatedBatteryPercent: Float = 78f,
    val simulatedSteps: Int = 8430,
    val simulatedHeartRate: Int = 72,
    val simulatedTemp: String = "22°C"
)

@Composable
fun WatchFaceCanvas(
    config: WatchFaceConfig,
    state: PreviewSimulationState,
    modifier: Modifier = Modifier,
    onComplicationClicked: ((Int) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Animation tick for live sweeping time or burn-in shift
    val infiniteTransition = rememberInfiniteTransition(label = "watch_face_tick")
    val smoothTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "smooth_tick"
    )

    // Calculate actual render time
    val (hour, minute, second) = if (state.isLiveTime) {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val s = cal.get(Calendar.SECOND) + (cal.get(Calendar.MILLISECOND) / 1000f)
        Triple(h, m, s)
    } else {
        Triple(
            state.simulatedHour,
            state.simulatedMinute,
            if (config.analogHands.secondHandMotion == HandMotion.SWEEP) {
                (state.simulatedSecond % 60f) + smoothTick * 0.15f
            } else {
                state.simulatedSecond
            }
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.selectedSlotId) {
                    detectTapGestures { offset ->
                        // Detect click on complication slots
                        val center = size.width / 2f
                        val slotRadius = center * 0.22f
                        config.complications.filter { it.enabled }.forEach { slot ->
                            val slotCenter = getSlotCenter(slot, center)
                            val dist = (offset - slotCenter).getDistance()
                            if (dist <= slotRadius) {
                                onComplicationClicked?.invoke(slot.slotId)
                            }
                        }
                    }
                }
        ) {
            renderWatchFaceFrame(
                context = context,
                config = config,
                state = state,
                hour = hour,
                minute = minute,
                second = second
            )
        }
    }
}

fun DrawScope.renderWatchFaceFrame(
    context: android.content.Context,
    config: WatchFaceConfig,
    state: PreviewSimulationState,
    hour: Int,
    minute: Int,
    second: Float
) {
    val width = size.width
    val height = size.height
    val center = Offset(width / 2f, height / 2f)
    val radius = (minOf(width, height) / 2f) - 4f

    // Apply Burn-In Protection shift in Ambient mode (Wear OS standard)
    val burnInOffset = if (state.isAmbient && config.ambientConfig.burnInProtectionShift) {
        Offset(sin(second.toDouble() * 0.1).toFloat() * 2f, cos(second.toDouble() * 0.1).toFloat() * 2f)
    } else Offset.Zero

    val shiftedCenter = center + burnInOffset

    // 1. Draw Dial Background & Case Clip
    drawDialBackground(config, state, shiftedCenter, radius)

    // 2. Draw Bezel Markings & Ticks
    drawBezel(config, state, shiftedCenter, radius)

    // 3. Draw Complication Slots
    config.complications.filter { it.enabled }.forEach { slot ->
        drawComplicationSlot(slot, config, state, shiftedCenter, radius)
    }

    // 4. Draw Digital Clock Engine (if DIGITAL or HYBRID)
    if (config.clockType == ClockType.DIGITAL || config.clockType == ClockType.HYBRID) {
        drawDigitalClock(context, config, state, hour, minute, second, shiftedCenter, radius)
    }

    // 5. Draw Analog Clock Hands (if ANALOG or HYBRID)
    if (config.clockType == ClockType.ANALOG || config.clockType == ClockType.HYBRID) {
        drawAnalogHands(config, state, hour, minute, second, shiftedCenter, radius)
    }

    // 6. Draw Guidelines Overlay if enabled
    if (state.showGuidelines) {
        drawGuidelinesOverlay(config, center, radius)
    }
}

private fun DrawScope.drawDialBackground(
    config: WatchFaceConfig,
    state: PreviewSimulationState,
    center: Offset,
    radius: Float
) {
    if (state.isAmbient) {
        // Strict low-power ambient background (Pure black for OLED savings)
        if (config.shape.isCircular) {
            drawCircle(Color.Black, radius = radius, center = center)
            drawCircle(Color(0xFF334155), radius = radius, center = center, style = Stroke(1.5f))
        } else {
            drawRect(Color.Black, topLeft = Offset.Zero, size = size)
            drawRect(Color(0xFF334155), topLeft = Offset.Zero, size = size, style = Stroke(1.5f))
        }
        return
    }

    // Active mode background
    when (config.dialBackgroundStyle) {
        DialBackgroundStyle.OBSIDIAN_PURE -> {
            drawCircle(Color(0xFF090D16), radius = radius, center = center)
        }
        DialBackgroundStyle.CARBON_TEXTURE -> {
            drawCircle(Color(0xFF0F172A), radius = radius, center = center)
            // Carbon fiber weave diagonal grid lines
            val step = 14f
            for (x in (-radius.toInt()..radius.toInt() step step.toInt())) {
                drawLine(
                    color = Color(0x2238BDF8),
                    start = Offset(center.x + x, center.y - radius),
                    end = Offset(center.x + x + radius, center.y + radius),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0x1538BDF8),
                    start = Offset(center.x + x + radius, center.y - radius),
                    end = Offset(center.x + x, center.y + radius),
                    strokeWidth = 1f
                )
            }
        }
        DialBackgroundStyle.RADIAL_GLOW -> {
            val themeColor = parseHexColor(config.themeColor)
            val brush = Brush.radialGradient(
                colors = listOf(themeColor.copy(alpha = 0.28f), Color(0xFF0B0F19), Color(0xFF04060A)),
                center = center,
                radius = radius
            )
            drawCircle(brush = brush, radius = radius, center = center)
        }
        DialBackgroundStyle.PILOT_MATTE -> {
            drawCircle(Color(0xFF131722), radius = radius, center = center)
        }
        DialBackgroundStyle.CYBER_GRID -> {
            drawCircle(Color(0xFF070A12), radius = radius, center = center)
            // Concentric cyber rings
            drawCircle(Color(0x3300E5FF), radius = radius * 0.75f, center = center, style = Stroke(1f))
            drawCircle(Color(0x22F43F5E), radius = radius * 0.50f, center = center, style = Stroke(1f))
        }
        DialBackgroundStyle.SOLAR_SUNBURST -> {
            val secondaryColor = parseHexColor(config.secondaryColor)
            val brush = Brush.radialGradient(
                colors = listOf(secondaryColor.copy(alpha = 0.25f), Color(0xFF181008), Color(0xFF080604)),
                center = center,
                radius = radius
            )
            drawCircle(brush = brush, radius = radius, center = center)
        }
    }

    // Outer Bezel Rim
    val bezelColor = parseHexColor(config.bezelColor)
    drawCircle(
        color = bezelColor,
        radius = radius,
        center = center,
        style = Stroke(width = 4f)
    )

    // Inner Accent Ring
    if (config.innerRingVisible) {
        val innerRingColor = parseHexColor(config.innerRingColor)
        drawCircle(
            color = innerRingColor,
            radius = radius * 0.82f,
            center = center,
            style = Stroke(width = 1.5f)
        )
    }
}

private fun DrawScope.drawBezel(
    config: WatchFaceConfig,
    state: PreviewSimulationState,
    center: Offset,
    radius: Float
) {
    val themeColor = if (state.isAmbient) Color(0xFF94A3B8) else parseHexColor(config.themeColor)
    val secondaryColor = if (state.isAmbient) Color(0xFF64748B) else parseHexColor(config.secondaryColor)

    when (config.bezelStyle) {
        BezelStyle.DIVER_60 -> {
            // 60-minute diver markings
            for (i in 0 until 60) {
                val angle = i * 6f
                val isMajor = (i % 5 == 0)
                val tickLength = if (isMajor) 14f else 6f
                val tickColor = if (isMajor) secondaryColor else themeColor.copy(alpha = 0.6f)
                val strokeWidth = if (isMajor) 3f else 1.2f

                rotate(angle, center) {
                    drawLine(
                        color = tickColor,
                        start = Offset(center.x, center.y - radius + 4f),
                        end = Offset(center.x, center.y - radius + 4f + tickLength),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            // Luminescent Diver Triangle at 12 o'clock
            val triPath = Path().apply {
                moveTo(center.x, center.y - radius + 4f)
                lineTo(center.x - 8f, center.y - radius + 22f)
                lineTo(center.x + 8f, center.y - radius + 22f)
                close()
            }
            drawPath(triPath, color = secondaryColor, style = Fill)
        }
        BezelStyle.CARDINAL_NUMERALS -> {
            // Numbers 12, 3, 6, 9
            val textPaint = android.graphics.Paint().apply {
                color = if (state.isAmbient) android.graphics.Color.WHITE else parseAndroidColor(config.themeColor)
                textSize = radius * 0.16f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            drawContext.canvas.nativeCanvas.drawText("12", center.x, center.y - radius * 0.76f + (textPaint.textSize / 3), textPaint)
            drawContext.canvas.nativeCanvas.drawText("6", center.x, center.y + radius * 0.82f, textPaint)
            drawContext.canvas.nativeCanvas.drawText("3", center.x + radius * 0.78f, center.y + (textPaint.textSize / 3), textPaint)
            drawContext.canvas.nativeCanvas.drawText("9", center.x - radius * 0.78f, center.y + (textPaint.textSize / 3), textPaint)

            // Ticks for other hours
            for (i in 0 until 12) {
                if (i % 3 != 0) {
                    val angle = i * 30f
                    rotate(angle, center) {
                        drawLine(
                            color = secondaryColor,
                            start = Offset(center.x, center.y - radius + 6f),
                            end = Offset(center.x, center.y - radius + 18f),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
        BezelStyle.ROMAN_NUMERALS -> {
            val textPaint = android.graphics.Paint().apply {
                color = if (state.isAmbient) android.graphics.Color.WHITE else parseAndroidColor(config.themeColor)
                textSize = radius * 0.14f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.SERIF
            }
            drawContext.canvas.nativeCanvas.drawText("XII", center.x, center.y - radius * 0.76f + (textPaint.textSize / 3), textPaint)
            drawContext.canvas.nativeCanvas.drawText("VI", center.x, center.y + radius * 0.82f, textPaint)
            drawContext.canvas.nativeCanvas.drawText("III", center.x + radius * 0.78f, center.y + (textPaint.textSize / 3), textPaint)
            drawContext.canvas.nativeCanvas.drawText("IX", center.x - radius * 0.78f, center.y + (textPaint.textSize / 3), textPaint)
        }
        BezelStyle.MINIMAL_TICKS -> {
            for (i in 0 until 60) {
                val angle = i * 6f
                val isHour = (i % 5 == 0)
                val len = if (isHour) 12f else 5f
                val col = if (isHour) themeColor else themeColor.copy(alpha = 0.4f)
                rotate(angle, center) {
                    drawLine(
                        color = col,
                        start = Offset(center.x, center.y - radius + 6f),
                        end = Offset(center.x, center.y - radius + 6f + len),
                        strokeWidth = if (isHour) 2.5f else 1f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        BezelStyle.DOT_INDICES -> {
            for (i in 0 until 12) {
                val angle = i * 30f
                val dotRadius = if (i % 3 == 0) 5f else 3f
                rotate(angle, center) {
                    drawCircle(
                        color = secondaryColor,
                        radius = dotRadius,
                        center = Offset(center.x, center.y - radius + 12f)
                    )
                }
            }
        }
        else -> {
            // Clean / None / Tachymeter
        }
    }
}

private fun DrawScope.drawComplicationSlot(
    slot: ComplicationSlotConfig,
    config: WatchFaceConfig,
    state: PreviewSimulationState,
    center: Offset,
    radius: Float
) {
    if (state.isAmbient && config.ambientConfig.hideComplications) return

    val slotCenter = getSlotCenter(slot, radius, center)
    val slotRadius = radius * 0.22f
    val isSelected = state.selectedSlotId == slot.slotId

    // Background circle for subdial
    val subdialBg = if (state.isAmbient) Color(0xFF0F172A) else Color(0xCC090E18)
    drawCircle(color = subdialBg, radius = slotRadius, center = slotCenter)

    // Selection ring if currently inspected
    if (isSelected) {
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = slotRadius + 4f,
            center = slotCenter,
            style = Stroke(width = 2.5f)
        )
    }

    // Dynamic Condition: High BPM Alert
    val isHighBpmActive = slot.dataSource == SimulatedDataSource.HEART_RATE &&
            state.simulatedHeartRate > 120 &&
            config.conditionRules.any { it.id == "rule_high_bpm" && it.isEnabled }

    if (isHighBpmActive && !state.isAmbient) {
        drawCircle(
            color = Color(0xFFF43F5E),
            radius = slotRadius + 3f,
            center = slotCenter,
            style = Stroke(width = 2f)
        )
    }

    // Dynamic Condition: Step Goal Achieved
    val isGoalAchieved = slot.dataSource == SimulatedDataSource.STEPS &&
            state.simulatedSteps >= 10000 &&
            config.conditionRules.any { it.id == "rule_step_goal" && it.isEnabled }

    if (isGoalAchieved && !state.isAmbient) {
        drawCircle(
            color = Color(0xFF10B981),
            radius = slotRadius + 3f,
            center = slotCenter,
            style = Stroke(width = 2f)
        )
    }

    // Dynamic Condition: Low Battery Alert
    val isLowBatteryActive = slot.dataSource == SimulatedDataSource.BATTERY &&
            state.simulatedBatteryPercent < 20f &&
            config.conditionRules.any { it.id == "rule_battery_warning" && it.isEnabled }

    val effectiveSlotColor = if (isLowBatteryActive) "#EF4444" else slot.accentColor
    val accentColor = if (state.isAmbient) Color(0xFFE2E8F0) else parseHexColor(effectiveSlotColor)

    // Calculate simulated value & progress
    val (displayVal, progress) = when (slot.dataSource) {
        SimulatedDataSource.BATTERY -> Pair("${state.simulatedBatteryPercent.toInt()}%", state.simulatedBatteryPercent / 100f)
        SimulatedDataSource.STEPS -> Pair("${state.simulatedSteps}", (state.simulatedSteps / 10000f).coerceIn(0f, 1f))
        SimulatedDataSource.HEART_RATE -> Pair("${state.simulatedHeartRate}", (state.simulatedHeartRate / 180f).coerceIn(0f, 1f))
        SimulatedDataSource.WEATHER -> Pair(state.simulatedTemp, 0.65f)
        SimulatedDataSource.CALORIES -> Pair("460", 0.76f)
        SimulatedDataSource.NEXT_EVENT -> Pair("11:30", 0.45f)
        SimulatedDataSource.SUNRISE_SUNSET -> Pair("18:45", 0.85f)
        SimulatedDataSource.UV_INDEX -> Pair("4.2", 0.42f)
    }

    val effectiveValue = if (slot.customValue.isNotEmpty()) slot.customValue else displayVal
    val effectiveProgress = if (slot.customProgress >= 0f) slot.customProgress else progress

    when (slot.slotType) {
        ComplicationType.RANGED_VALUE, ComplicationType.GOAL_PROGRESS -> {
            // Gauge Arc
            val arcRect = Size(slotRadius * 1.6f, slotRadius * 1.6f)
            val arcTopLeft = Offset(slotCenter.x - arcRect.width / 2f, slotCenter.y - arcRect.height / 2f)

            // Background Track
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcRect,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            // Active Track
            drawArc(
                color = accentColor,
                startAngle = 135f,
                sweepAngle = 270f * effectiveProgress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcRect,
                style = Stroke(width = 5.5f, cap = StrokeCap.Round)
            )

            // Text inside
            drawComplicationText(
                slotCenter = slotCenter,
                topText = slot.name.take(4).uppercase(),
                mainText = effectiveValue,
                color = if (state.isAmbient) android.graphics.Color.WHITE else parseAndroidColor(slot.accentColor),
                textSize = slotRadius * 0.42f
            )
        }
        ComplicationType.WEIGHTED_ARC -> {
            // Wear OS 7 multi-segment weighted arc indicator
            val arcRect = Size(slotRadius * 1.65f, slotRadius * 1.65f)
            val arcTopLeft = Offset(slotCenter.x - arcRect.width / 2f, slotCenter.y - arcRect.height / 2f)
            val colors = listOf(accentColor, Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF38BDF8))
            val segmentSweep = 60f
            for (idx in 0 until 4) {
                val start = 135f + idx * 70f
                drawArc(
                    color = if (state.isAmbient) Color.Gray else colors[idx],
                    startAngle = start,
                    sweepAngle = segmentSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcRect,
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )
            }
            drawComplicationText(
                slotCenter = slotCenter,
                topText = "WFF7",
                mainText = effectiveValue,
                color = android.graphics.Color.WHITE,
                textSize = slotRadius * 0.40f
            )
        }
        ComplicationType.SHORT_TEXT -> {
            drawCircle(
                color = accentColor.copy(alpha = 0.15f),
                radius = slotRadius * 0.85f,
                center = slotCenter
            )
            drawComplicationText(
                slotCenter = slotCenter,
                topText = slot.name.take(6).uppercase(),
                mainText = effectiveValue,
                color = if (state.isAmbient) android.graphics.Color.WHITE else parseAndroidColor(slot.accentColor),
                textSize = slotRadius * 0.44f
            )
        }
        ComplicationType.ICON -> {
            drawCircle(
                color = accentColor.copy(alpha = 0.2f),
                radius = slotRadius * 0.8f,
                center = slotCenter
            )
            // Icon glyph badge
            drawComplicationText(
                slotCenter = slotCenter,
                topText = slot.dataSource.name.take(3),
                mainText = "●",
                color = if (state.isAmbient) android.graphics.Color.WHITE else parseAndroidColor(slot.accentColor),
                textSize = slotRadius * 0.5f
            )
        }
    }
}

private fun DrawScope.drawComplicationText(
    slotCenter: Offset,
    topText: String,
    mainText: String,
    color: Int,
    textSize: Float
) {
    val mainPaint = android.graphics.Paint().apply {
        this.color = color
        this.textSize = textSize
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val topPaint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.LTGRAY
        this.textSize = textSize * 0.58f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }

    drawContext.canvas.nativeCanvas.drawText(topText, slotCenter.x, slotCenter.y - textSize * 0.45f, topPaint)
    drawContext.canvas.nativeCanvas.drawText(mainText, slotCenter.x, slotCenter.y + textSize * 0.65f, mainPaint)
}

private fun DrawScope.drawDigitalClock(
    context: android.content.Context,
    config: WatchFaceConfig,
    state: PreviewSimulationState,
    hour: Int,
    minute: Int,
    second: Float,
    center: Offset,
    radius: Float
) {
    val lowBit = state.isAmbient && config.ambientConfig.lowBitMode
    val dimAlpha = if (state.isAmbient) config.ambientConfig.dimFactor.coerceIn(0.25f, 1f) else 1f
    DigitalClockRenderer.drawDigitalClock(
        drawScope = this,
        context = context,
        config = config,
        center = center,
        radius = radius,
        hour = hour,
        minute = minute,
        second = second.toInt(),
        timeMillis = System.currentTimeMillis(),
        isAmbient = state.isAmbient,
        lowBit = lowBit,
        dimAlpha = dimAlpha,
        parseColor = { hex, _ -> parseHexColor(hex) }
    )
}

private fun DrawScope.drawAnalogHands(
    config: WatchFaceConfig,
    state: PreviewSimulationState,
    hour: Int,
    minute: Int,
    second: Float,
    center: Offset,
    radius: Float
) {
    // 1. Calculate hand angles
    val hourAngle = (hour % 12 + minute / 60f + second / 3600f) * 30f
    val minuteAngle = (minute + second / 60f) * 6f
    val secondAngle = if (config.analogHands.secondHandMotion == HandMotion.SWEEP) {
        second * 6f
    } else {
        second.toInt() * 6f
    }

    val isAmbient = state.isAmbient
    val hollowHands = isAmbient && config.analogHands.ambientHollowHands

    // 2. Hour Hand
    val hourLen = radius * config.analogHands.hourHandLength
    val hourWidth = config.analogHands.hourHandWidth
    val hourColor = if (isAmbient) Color.White else parseHexColor(config.analogHands.hourHandColor)

    rotate(hourAngle, center) {
        drawHandShape(
            style = config.analogHands.hourHandStyle,
            length = hourLen,
            width = hourWidth,
            center = center,
            color = hourColor,
            accentColor = parseHexColor(config.analogHands.hourHandAccentColor),
            isHollow = hollowHands
        )
    }

    // 3. Minute Hand
    val minLen = radius * config.analogHands.minuteHandLength
    val minWidth = config.analogHands.minuteHandWidth
    val minColor = if (isAmbient) Color.White else parseHexColor(config.analogHands.minuteHandColor)

    rotate(minuteAngle, center) {
        drawHandShape(
            style = config.analogHands.minuteHandStyle,
            length = minLen,
            width = minWidth,
            center = center,
            color = minColor,
            accentColor = parseHexColor(config.analogHands.minuteHandAccentColor),
            isHollow = hollowHands
        )
    }

    // 4. Second Hand (Strict Wear OS standard: HIDDEN in ambient/AOD mode to save power)
    if (!isAmbient && config.analogHands.secondHandVisible && !config.ambientConfig.hideSecondHand) {
        val secLen = radius * config.analogHands.secondHandLength
        val secColor = parseHexColor(config.analogHands.secondHandColor)

        rotate(secondAngle, center) {
            // Counterweight tail
            drawLine(
                color = secColor,
                start = Offset(center.x, center.y + 22f),
                end = Offset(center.x, center.y - secLen),
                strokeWidth = config.analogHands.secondHandWidth,
                cap = StrokeCap.Round
            )
            // Accent circle on needle
            drawCircle(
                color = secColor,
                radius = 4f,
                center = Offset(center.x, center.y - secLen * 0.72f)
            )
        }
    }

    // 5. Center Pivot Cap
    val pinRadius = config.analogHands.centerPinRadius
    val pinColor = if (isAmbient) Color.White else parseHexColor(config.analogHands.centerPinColor)
    drawCircle(color = pinColor, radius = pinRadius, center = center)
    drawCircle(color = Color.Black, radius = pinRadius * 0.45f, center = center)
}

private fun DrawScope.drawHandShape(
    style: HandStyle,
    length: Float,
    width: Float,
    center: Offset,
    color: Color,
    accentColor: Color,
    isHollow: Boolean
) {
    val halfW = width / 2f
    when (style) {
        HandStyle.SWORD -> {
            val path = Path().apply {
                moveTo(center.x - halfW * 0.8f, center.y + 12f)
                lineTo(center.x - halfW * 1.3f, center.y - length * 0.75f)
                lineTo(center.x, center.y - length)
                lineTo(center.x + halfW * 1.3f, center.y - length * 0.75f)
                lineTo(center.x + halfW * 0.8f, center.y + 12f)
                close()
            }
            if (isHollow) {
                drawPath(path, color = color, style = Stroke(width = 2f))
            } else {
                drawPath(path, color = color, style = Fill)
                // Center luminescent groove
                drawLine(
                    color = accentColor,
                    start = Offset(center.x, center.y + 4f),
                    end = Offset(center.x, center.y - length * 0.82f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }
        }
        HandStyle.BATON -> {
            if (isHollow) {
                drawRect(
                    color = color,
                    topLeft = Offset(center.x - halfW, center.y - length),
                    size = Size(width, length + 10f),
                    style = Stroke(width = 2f)
                )
            } else {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(center.x - halfW, center.y - length),
                    size = Size(width, length + 10f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(halfW, halfW)
                )
                drawCircle(
                    color = accentColor,
                    radius = halfW * 0.8f,
                    center = Offset(center.x, center.y - length + halfW + 2f)
                )
            }
        }
        HandStyle.ARROW -> {
            val path = Path().apply {
                moveTo(center.x - halfW, center.y + 10f)
                lineTo(center.x - halfW, center.y - length * 0.7f)
                lineTo(center.x - halfW * 2.5f, center.y - length * 0.7f)
                lineTo(center.x, center.y - length)
                lineTo(center.x + halfW * 2.5f, center.y - length * 0.7f)
                lineTo(center.x + halfW, center.y - length * 0.7f)
                lineTo(center.x + halfW, center.y + 10f)
                close()
            }
            if (isHollow) {
                drawPath(path, color = color, style = Stroke(width = 2f))
            } else {
                drawPath(path, color = color, style = Fill)
            }
        }
        else -> {
            // Slender / Baton fallback
            drawLine(
                color = color,
                start = Offset(center.x, center.y + 8f),
                end = Offset(center.x, center.y - length),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawGuidelinesOverlay(
    config: WatchFaceConfig,
    center: Offset,
    radius: Float
) {
    // 1. Center Crosshairs
    drawLine(
        color = Color(0x8800E5FF),
        start = Offset(center.x, center.y - radius),
        end = Offset(center.x, center.y + radius),
        strokeWidth = 1f
    )
    drawLine(
        color = Color(0x8800E5FF),
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = 1f
    )

    // 2. Safe zone ring (10% inward boundary per Wear OS guidelines)
    drawCircle(
        color = Color(0x66F59E0B),
        radius = radius * 0.90f,
        center = center,
        style = Stroke(width = 1f)
    )

    // 3. Complication slot bounding boxes
    config.complications.filter { it.enabled }.forEach { slot ->
        val slotCenter = getSlotCenter(slot, radius, center)
        val slotRadius = radius * 0.22f
        drawCircle(
            color = Color(0xAA10B981),
            radius = slotRadius,
            center = slotCenter,
            style = Stroke(width = 1.5f)
        )
    }
}

private fun getSlotCenter(slot: ComplicationSlotConfig, radius: Float, center: Offset): Offset {
    val angleDeg = slot.customAngle ?: slot.position.angleDeg
    val rFactor = slot.customRadiusFactor ?: slot.position.radiusFactor
    val r = radius * rFactor
    val rad = Math.toRadians(angleDeg.toDouble())
    return Offset(
        (center.x + r * cos(rad)).toFloat(),
        (center.y + r * sin(rad)).toFloat()
    )
}

private fun getSlotCenter(slot: ComplicationSlotConfig, size: Float): Offset {
    val center = Offset(size, size)
    val angleDeg = slot.customAngle ?: slot.position.angleDeg
    val rFactor = slot.customRadiusFactor ?: slot.position.radiusFactor
    val r = size * rFactor
    val rad = Math.toRadians(angleDeg.toDouble())
    return Offset(
        (center.x + r * cos(rad)).toFloat(),
        (center.y + r * sin(rad)).toFloat()
    )
}

fun parseHexColor(hex: String, fallback: Color = Color.White): Color {
    return try {
        val clean = hex.removePrefix("#")
        val parsed = clean.toLong(16)
        if (clean.length == 6) {
            Color(parsed or 0xFF000000)
        } else if (clean.length == 8) {
            Color(parsed)
        } else fallback
    } catch (_: Exception) {
        fallback
    }
}

fun parseAndroidColor(hex: String, fallback: Int = android.graphics.Color.WHITE): Int {
    return try {
        android.graphics.Color.parseColor(hex)
    } catch (_: Exception) {
        fallback
    }
}
