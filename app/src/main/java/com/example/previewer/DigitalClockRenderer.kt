package com.example.previewer

import android.content.Context
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.DigitalFontFamily
import com.example.model.WatchFaceConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DigitalClockRenderer {

    private val typefaceCache = mutableMapOf<DigitalFontFamily, Typeface>()

    fun getComposeFontFamily(fontFamily: DigitalFontFamily): FontFamily {
        return when (fontFamily) {
            DigitalFontFamily.TECH_MONO -> FontFamily(Font(R.font.share_tech_mono, FontWeight.Normal))
            DigitalFontFamily.CYBER_DISPLAY -> FontFamily(Font(R.font.orbitron, FontWeight.Bold))
            DigitalFontFamily.BOLD_GEOMETRIC -> FontFamily(Font(R.font.chakra_petch, FontWeight.Bold))
            DigitalFontFamily.STENCIL_MILITARY -> FontFamily(Font(R.font.wallpoet, FontWeight.Normal))
            DigitalFontFamily.RETRO_LED -> FontFamily(Font(R.font.vt323, FontWeight.Normal))
            DigitalFontFamily.PIXEL_ARCADE -> FontFamily(Font(R.font.silkscreen, FontWeight.Normal))
            DigitalFontFamily.MODERN_SANS -> FontFamily.SansSerif
            DigitalFontFamily.DIGITAL_7 -> FontFamily(Font(R.font.orbitron, FontWeight.Bold))
            DigitalFontFamily.DOT_MATRIX -> FontFamily(Font(R.font.silkscreen, FontWeight.Normal))
        }
    }

    fun getAndroidTypeface(context: Context, fontFamily: DigitalFontFamily): Typeface {
        typefaceCache[fontFamily]?.let { return it }
        val resId = when (fontFamily) {
            DigitalFontFamily.TECH_MONO -> R.font.share_tech_mono
            DigitalFontFamily.CYBER_DISPLAY -> R.font.orbitron
            DigitalFontFamily.BOLD_GEOMETRIC -> R.font.chakra_petch
            DigitalFontFamily.STENCIL_MILITARY -> R.font.wallpoet
            DigitalFontFamily.RETRO_LED -> R.font.vt323
            DigitalFontFamily.PIXEL_ARCADE -> R.font.silkscreen
            DigitalFontFamily.MODERN_SANS -> null
            DigitalFontFamily.DIGITAL_7 -> null
            DigitalFontFamily.DOT_MATRIX -> null
        }
        val tf = try {
            if (resId != null) {
                ResourcesCompat.getFont(context, resId) ?: Typeface.MONOSPACE
            } else {
                Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
        } catch (_: Exception) {
            Typeface.MONOSPACE
        }
        typefaceCache[fontFamily] = tf
        return tf
    }

    // 5x7 LED Dot Matrix patterns for digits '0'-'9' and ':'
    private val DOT_MATRIX_GLYPHS: Map<Char, List<Int>> = mapOf(
        '0' to listOf(
            0b01110,
            0b10001,
            0b10011,
            0b10101,
            0b11001,
            0b10001,
            0b01110
        ),
        '1' to listOf(
            0b00100,
            0b01100,
            0b00100,
            0b00100,
            0b00100,
            0b00100,
            0b01110
        ),
        '2' to listOf(
            0b01110,
            0b10001,
            0b00001,
            0b00110,
            0b01000,
            0b10000,
            0b11111
        ),
        '3' to listOf(
            0b11110,
            0b00001,
            0b00001,
            0b01110,
            0b00001,
            0b00001,
            0b11110
        ),
        '4' to listOf(
            0b00010,
            0b00110,
            0b01010,
            0b10010,
            0b11111,
            0b00010,
            0b00010
        ),
        '5' to listOf(
            0b11111,
            0b10000,
            0b11110,
            0b00001,
            0b00001,
            0b10001,
            0b01110
        ),
        '6' to listOf(
            0b00110,
            0b01000,
            0b10000,
            0b11110,
            0b10001,
            0b10001,
            0b01110
        ),
        '7' to listOf(
            0b11111,
            0b00001,
            0b00010,
            0b00100,
            0b01000,
            0b01000,
            0b01000
        ),
        '8' to listOf(
            0b01110,
            0b10001,
            0b10001,
            0b01110,
            0b10001,
            0b10001,
            0b01110
        ),
        '9' to listOf(
            0b01110,
            0b10001,
            0b10001,
            0b01111,
            0b00001,
            0b00010,
            0b01100
        ),
        ':' to listOf(
            0b000,
            0b010,
            0b010,
            0b000,
            0b010,
            0b010,
            0b000
        )
    )

    // 7-Segment truth table: segments [A, B, C, D, E, F, G]
    //   --A--
    //  |     |
    //  F     B
    //  |     |
    //   --G--
    //  |     |
    //  E     C
    //  |     |
    //   --D--
    private val SEVEN_SEGMENT_MAP: Map<Char, BooleanArray> = mapOf(
        '0' to booleanArrayOf(true, true, true, true, true, true, false),
        '1' to booleanArrayOf(false, true, true, false, false, false, false),
        '2' to booleanArrayOf(true, true, false, true, true, false, true),
        '3' to booleanArrayOf(true, true, true, true, false, false, true),
        '4' to booleanArrayOf(false, true, true, false, false, true, true),
        '5' to booleanArrayOf(true, false, true, true, false, true, true),
        '6' to booleanArrayOf(true, false, true, true, true, true, true),
        '7' to booleanArrayOf(true, true, true, false, false, false, false),
        '8' to booleanArrayOf(true, true, true, true, true, true, true),
        '9' to booleanArrayOf(true, true, true, true, false, true, true)
    )

    fun drawDigitalClock(
        drawScope: DrawScope,
        context: Context,
        config: WatchFaceConfig,
        center: Offset,
        radius: Float,
        hour: Int,
        minute: Int,
        second: Int,
        timeMillis: Long,
        isAmbient: Boolean,
        lowBit: Boolean,
        dimAlpha: Float,
        parseColor: (String, Color) -> Color
    ) {
        val dig = config.digitalClock
        val yOffsetPx = (dig.positionYOffset / 225f) * radius
        val clockCenterY = center.y + yOffsetPx

        val displayHour = if (dig.is24Hour) {
            hour
        } else {
            if (hour % 12 == 0) 12 else hour % 12
        }

        val hourFmt = if (dig.leadingZero) {
            String.format(Locale.US, "%02d", displayHour)
        } else {
            displayHour.toString()
        }
        val minFmt = String.format(Locale.US, "%02d", minute)
        val secFmt = if (dig.showSeconds && !isAmbient) {
            String.format(Locale.US, "%02d", second)
        } else {
            ""
        }

        val hourColor = if (lowBit) Color.White else parseColor(dig.hourColor, Color.White).copy(alpha = dimAlpha)
        val minColor = if (lowBit) Color.White else parseColor(dig.minuteColor, Color.Cyan).copy(alpha = dimAlpha)
        val secColor = if (lowBit) Color.White else parseColor(dig.secondColor, Color(0xFFF59E0B)).copy(alpha = dimAlpha)
        val glowActive = dig.glowEffect && !isAmbient

        // Scale proportionally to watch face radius and support bigger sizes up to 120sp
        val scaledHeight = (dig.fontSize.toFloat() / 48f) * (radius * 0.28f)

        when (dig.fontFamily) {
            DigitalFontFamily.DIGITAL_7 -> {
                drawSevenSegmentClock(
                    drawScope = drawScope,
                    center = Offset(center.x, clockCenterY),
                    digitHeight = scaledHeight,
                    hourStr = hourFmt,
                    minStr = minFmt,
                    secStr = secFmt,
                    hourColor = hourColor,
                    minColor = minColor,
                    secColor = secColor,
                    glowActive = glowActive,
                    isAmbient = isAmbient
                )
            }
            DigitalFontFamily.DOT_MATRIX -> {
                // Dot Matrix is naturally crisp and compact; scale smoothly across 18..120
                drawDotMatrixClock(
                    drawScope = drawScope,
                    center = Offset(center.x, clockCenterY),
                    digitHeight = scaledHeight * 0.86f,
                    hourStr = hourFmt,
                    minStr = minFmt,
                    secStr = secFmt,
                    hourColor = hourColor,
                    minColor = minColor,
                    secColor = secColor,
                    glowActive = glowActive,
                    isAmbient = isAmbient
                )
            }
            else -> {
                drawTypefaceClock(
                    drawScope = drawScope,
                    context = context,
                    fontFamily = dig.fontFamily,
                    center = Offset(center.x, clockCenterY),
                    textSizePx = scaledHeight,
                    hourStr = hourFmt,
                    minStr = minFmt,
                    secStr = secFmt,
                    hourColor = hourColor,
                    minColor = minColor,
                    secColor = secColor,
                    glowActive = glowActive,
                    lowBit = lowBit
                )
            }
        }

        // Date Line below Digital Clock
        if (dig.showDate) {
            val dateColor = if (lowBit) {
                Color.White.copy(alpha = 0.8f)
            } else {
                parseColor(dig.dateColor, Color.LightGray).copy(alpha = dimAlpha)
            }
            val dateText = try {
                val sdf = SimpleDateFormat(dig.dateFormat, Locale.getDefault())
                val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
                sdf.format(cal.time).uppercase(Locale.getDefault())
            } catch (_: Exception) {
                "FRI, OCT 24"
            }

            val dateTextSize = (radius * 0.082f).coerceIn(14f, 28f)
            val datePaint = Paint().apply {
                isAntiAlias = !lowBit
                color = dateColor.toArgb()
                textSize = dateTextSize
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                letterSpacing = 0.1f
            }
            val dateY = clockCenterY + (scaledHeight * 0.58f) + dateTextSize * 1.15f
            drawScope.drawContext.canvas.nativeCanvas.drawText(
                dateText,
                center.x,
                dateY,
                datePaint
            )
        }
    }

    private fun drawTypefaceClock(
        drawScope: DrawScope,
        context: Context,
        fontFamily: DigitalFontFamily,
        center: Offset,
        textSizePx: Float,
        hourStr: String,
        minStr: String,
        secStr: String,
        hourColor: Color,
        minColor: Color,
        secColor: Color,
        glowActive: Boolean,
        lowBit: Boolean
    ) {
        val tf = getAndroidTypeface(context, fontFamily)
        val mainSize = when (fontFamily) {
            DigitalFontFamily.RETRO_LED -> textSizePx * 1.18f
            DigitalFontFamily.PIXEL_ARCADE -> textSizePx * 0.84f
            else -> textSizePx
        }

        val paint = Paint().apply {
            isAntiAlias = !lowBit
            textSize = mainSize
            typeface = tf
            isFakeBoldText = fontFamily == DigitalFontFamily.MODERN_SANS
            if (glowActive) {
                setShadowLayer(12f, 0f, 0f, minColor.toArgb())
            }
        }

        val colonStr = ":"
        val hourWidth = paint.measureText(hourStr)
        val colonWidth = paint.measureText(colonStr)
        val minWidth = paint.measureText(minStr)

        val secPaint = Paint(paint).apply {
            textSize = mainSize * 0.56f
            color = secColor.toArgb()
            if (glowActive) {
                setShadowLayer(8f, 0f, 0f, secColor.toArgb())
            }
        }
        val secDisplay = if (secStr.isNotEmpty()) ":$secStr" else ""
        val secWidth = if (secDisplay.isNotEmpty()) secPaint.measureText(secDisplay) else 0f

        val totalWidth = hourWidth + colonWidth + minWidth + secWidth
        var curX = center.x - totalWidth / 2f
        val baselineY = center.y + (mainSize * 0.35f)

        val nativeCanvas = drawScope.drawContext.canvas.nativeCanvas

        // Draw Hours
        paint.color = hourColor.toArgb()
        if (glowActive) paint.setShadowLayer(12f, 0f, 0f, hourColor.toArgb())
        nativeCanvas.drawText(hourStr, curX, baselineY, paint)
        curX += hourWidth

        // Draw Colon + Minutes
        paint.color = minColor.toArgb()
        if (glowActive) paint.setShadowLayer(12f, 0f, 0f, minColor.toArgb())
        nativeCanvas.drawText(colonStr + minStr, curX, baselineY, paint)
        curX += colonWidth + minWidth

        // Draw Seconds
        if (secDisplay.isNotEmpty()) {
            nativeCanvas.drawText(secDisplay, curX, baselineY, secPaint)
        }
    }

    /**
     * Renders an authentic 5x7 LED Dot Matrix clock on the watch canvas.
     */
    fun drawDotMatrixClock(
        drawScope: DrawScope,
        center: Offset,
        digitHeight: Float,
        hourStr: String,
        minStr: String,
        secStr: String,
        hourColor: Color,
        minColor: Color,
        secColor: Color,
        glowActive: Boolean,
        isAmbient: Boolean
    ) {
        val rows = 7
        val cellStep = digitHeight / rows.toFloat()
        val dotRadius = cellStep * 0.39f
        val charGap = cellStep * 1.15f

        val secScale = 0.62f
        val secCellStep = cellStep * secScale
        val secDotRadius = dotRadius * secScale
        val secCharGap = charGap * secScale

        fun measureTokenWidth(text: String, step: Float, gap: Float): Float {
            if (text.isEmpty()) return 0f
            var w = 0f
            text.forEachIndexed { idx, ch ->
                val cols = if (ch == ':') 3 else 5
                w += cols * step
                if (idx < text.lastIndex) w += gap
            }
            return w
        }

        val mainText = "$hourStr:$minStr"
        val mainWidth = measureTokenWidth(mainText, cellStep, charGap)
        val secText = if (secStr.isNotEmpty()) ":$secStr" else ""
        val secWidth = if (secText.isNotEmpty()) secCharGap + measureTokenWidth(secText, secCellStep, secCharGap) else 0f

        val totalWidth = mainWidth + secWidth
        var curX = center.x - totalWidth / 2f
        val topY = center.y - digitHeight / 2f

        val unlitColor = Color.White.copy(alpha = if (isAmbient) 0.03f else 0.12f)

        // Helper to draw a single character in 5x7 dot matrix
        fun renderMatrixChar(
            ch: Char,
            startX: Float,
            startY: Float,
            step: Float,
            radius: Float,
            litColor: Color
        ): Float {
            val cols = if (ch == ':') 3 else 5
            val glyph = DOT_MATRIX_GLYPHS[ch] ?: DOT_MATRIX_GLYPHS['0']!!
            for (r in 0 until rows) {
                val rowMask = glyph[r]
                val cy = startY + (r + 0.5f) * step
                for (c in 0 until cols) {
                    val bit = (rowMask shr (cols - 1 - c)) and 1
                    val cx = startX + (c + 0.5f) * step
                    if (bit == 1) {
                        if (glowActive) {
                            drawScope.drawCircle(
                                color = litColor.copy(alpha = 0.28f),
                                radius = radius * 1.85f,
                                center = Offset(cx, cy)
                            )
                        }
                        drawScope.drawCircle(
                            color = litColor,
                            radius = radius,
                            center = Offset(cx, cy)
                        )
                    } else if (!isAmbient && ch != ':') {
                        drawScope.drawCircle(
                            color = unlitColor,
                            radius = radius * 0.65f,
                            center = Offset(cx, cy)
                        )
                    }
                }
            }
            return cols * step
        }

        // Draw Hours
        hourStr.forEach { ch ->
            curX += renderMatrixChar(ch, curX, topY, cellStep, dotRadius, hourColor) + charGap
        }
        // Draw Colon
        curX += renderMatrixChar(':', curX, topY, cellStep, dotRadius, minColor) + charGap
        // Draw Minutes
        minStr.forEachIndexed { idx, ch ->
            curX += renderMatrixChar(ch, curX, topY, cellStep, dotRadius, minColor)
            if (idx < minStr.lastIndex) curX += charGap
        }

        // Draw Seconds (aligned to bottom of main matrix digits)
        if (secText.isNotEmpty()) {
            curX += secCharGap
            val secHeight = rows * secCellStep
            val secTopY = topY + (digitHeight - secHeight)
            secText.forEachIndexed { idx, ch ->
                curX += renderMatrixChar(ch, curX, secTopY, secCellStep, secDotRadius, secColor)
                if (idx < secText.lastIndex) curX += secCharGap
            }
        }
    }

    /**
     * Renders an authentic Digital-7 (7-segment LCD/LED) clock with beveled segments and ghost segments.
     */
    fun drawSevenSegmentClock(
        drawScope: DrawScope,
        center: Offset,
        digitHeight: Float,
        hourStr: String,
        minStr: String,
        secStr: String,
        hourColor: Color,
        minColor: Color,
        secColor: Color,
        glowActive: Boolean,
        isAmbient: Boolean
    ) {
        val digitWidth = digitHeight * 0.54f
        val segThickness = (digitHeight * 0.115f).coerceAtLeast(2.2f)
        val charGap = digitHeight * 0.13f
        val colonWidth = segThickness * 1.8f

        val secScale = 0.60f
        val secDigitHeight = digitHeight * secScale
        val secDigitWidth = digitWidth * secScale
        val secThickness = (segThickness * secScale).coerceAtLeast(1.8f)
        val secGap = charGap * secScale
        val secColonWidth = colonWidth * secScale

        val hoursTotalW = hourStr.length * digitWidth + (hourStr.length - 1).coerceAtLeast(0) * charGap
        val minsTotalW = minStr.length * digitWidth + (minStr.length - 1).coerceAtLeast(0) * charGap
        val mainWidth = hoursTotalW + charGap + colonWidth + charGap + minsTotalW

        val secsTotalW = if (secStr.isNotEmpty()) {
            secGap + secColonWidth + secGap + secStr.length * secDigitWidth + (secStr.length - 1) * secGap
        } else 0f

        val totalWidth = mainWidth + secsTotalW
        var curX = center.x - totalWidth / 2f
        val topY = center.y - digitHeight / 2f

        val nativeCanvas = drawScope.drawContext.canvas.nativeCanvas

        // Draw Hours
        hourStr.forEach { ch ->
            drawSingleSevenSegmentDigit(
                nativeCanvas = nativeCanvas,
                ch = ch,
                x = curX,
                y = topY,
                w = digitWidth,
                h = digitHeight,
                t = segThickness,
                litColor = hourColor,
                glowActive = glowActive,
                isAmbient = isAmbient
            )
            curX += digitWidth + charGap
        }

        // Draw Colon
        drawSevenSegmentColon(
            drawScope = drawScope,
            x = curX,
            y = topY,
            w = colonWidth,
            h = digitHeight,
            dotSize = segThickness * 1.05f,
            color = minColor,
            glowActive = glowActive
        )
        curX += colonWidth + charGap

        // Draw Minutes
        minStr.forEachIndexed { idx, ch ->
            drawSingleSevenSegmentDigit(
                nativeCanvas = nativeCanvas,
                ch = ch,
                x = curX,
                y = topY,
                w = digitWidth,
                h = digitHeight,
                t = segThickness,
                litColor = minColor,
                glowActive = glowActive,
                isAmbient = isAmbient
            )
            curX += digitWidth
            if (idx < minStr.lastIndex) curX += charGap
        }

        // Draw Seconds
        if (secStr.isNotEmpty()) {
            curX += secGap
            val secTopY = topY + (digitHeight - secDigitHeight)
            drawSevenSegmentColon(
                drawScope = drawScope,
                x = curX,
                y = secTopY,
                w = secColonWidth,
                h = secDigitHeight,
                dotSize = secThickness * 1.05f,
                color = secColor,
                glowActive = glowActive
            )
            curX += secColonWidth + secGap
            secStr.forEachIndexed { idx, ch ->
                drawSingleSevenSegmentDigit(
                    nativeCanvas = nativeCanvas,
                    ch = ch,
                    x = curX,
                    y = secTopY,
                    w = secDigitWidth,
                    h = secDigitHeight,
                    t = secThickness,
                    litColor = secColor,
                    glowActive = glowActive,
                    isAmbient = isAmbient
                )
                curX += secDigitWidth
                if (idx < secStr.lastIndex) curX += secGap
            }
        }
    }

    private fun drawSevenSegmentColon(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        dotSize: Float,
        color: Color,
        glowActive: Boolean
    ) {
        val cx = x + w / 2f
        val topDotY = y + h * 0.32f
        val botDotY = y + h * 0.68f
        if (glowActive) {
            drawScope.drawCircle(color.copy(alpha = 0.28f), radius = dotSize * 1.3f, center = Offset(cx, topDotY))
            drawScope.drawCircle(color.copy(alpha = 0.28f), radius = dotSize * 1.3f, center = Offset(cx, botDotY))
        }
        drawScope.drawCircle(color, radius = dotSize * 0.58f, center = Offset(cx, topDotY))
        drawScope.drawCircle(color, radius = dotSize * 0.58f, center = Offset(cx, botDotY))
    }

    private fun drawSingleSevenSegmentDigit(
        nativeCanvas: android.graphics.Canvas,
        ch: Char,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        t: Float,
        litColor: Color,
        glowActive: Boolean,
        isAmbient: Boolean
    ) {
        val activeSegments = SEVEN_SEGMENT_MAP[ch] ?: SEVEN_SEGMENT_MAP['8']!!
        val halfH = h / 2f
        val gap = t * 0.22f
        val slant = 0.08f // Classic Digital-7 forward italic slant

        fun sx(px: Float, py: Float): Float {
            // Slant relative to vertical center of the digit
            val relY = (y + halfH) - py
            return px + relY * slant
        }

        fun horizontalSegmentPath(left: Float, right: Float, cy: Float): Path {
            val ht = t / 2f
            val l = left + gap
            val r = right - gap
            return Path().apply {
                moveTo(sx(l, cy), cy)
                lineTo(sx(l + ht, cy - ht), cy - ht)
                lineTo(sx(r - ht, cy - ht), cy - ht)
                lineTo(sx(r, cy), cy)
                lineTo(sx(r - ht, cy + ht), cy + ht)
                lineTo(sx(l + ht, cy + ht), cy + ht)
                close()
            }
        }

        fun verticalSegmentPath(cx: Float, top: Float, bottom: Float): Path {
            val ht = t / 2f
            val tp = top + gap
            val bt = bottom - gap
            return Path().apply {
                moveTo(sx(cx, tp), tp)
                lineTo(sx(cx + ht, tp + ht), tp + ht)
                lineTo(sx(cx + ht, bt - ht), bt - ht)
                lineTo(sx(cx, bt), bt)
                lineTo(sx(cx - ht, bt - ht), bt - ht)
                lineTo(sx(cx - ht, tp + ht), tp + ht)
                close()
            }
        }

        val leftX = x + t / 2f
        val rightX = x + w - t / 2f
        val topY = y + t / 2f
        val midY = y + halfH
        val botY = y + h - t / 2f

        val segments = listOf(
            horizontalSegmentPath(leftX, rightX, topY),   // A (Top)
            verticalSegmentPath(rightX, topY, midY),      // B (Top-Right)
            verticalSegmentPath(rightX, midY, botY),      // C (Bottom-Right)
            horizontalSegmentPath(leftX, rightX, botY),   // D (Bottom)
            verticalSegmentPath(leftX, midY, botY),       // E (Bottom-Left)
            verticalSegmentPath(leftX, topY, midY),       // F (Top-Left)
            horizontalSegmentPath(leftX, rightX, midY)    // G (Middle)
        )

        val unlitPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = litColor.copy(alpha = 0.11f).toArgb()
        }

        val litPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = litColor.toArgb()
            if (glowActive) {
                setShadowLayer(10f, 0f, 0f, litColor.toArgb())
            }
        }

        segments.forEachIndexed { idx, path ->
            if (activeSegments[idx]) {
                nativeCanvas.drawPath(path, litPaint)
            } else if (!isAmbient) {
                nativeCanvas.drawPath(path, unlitPaint)
            }
        }
    }
}
