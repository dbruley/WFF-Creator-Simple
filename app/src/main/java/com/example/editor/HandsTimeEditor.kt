package com.example.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import com.example.model.ClockType
import com.example.model.DigitalFontFamily
import com.example.model.HandMotion
import com.example.model.HandStyle
import com.example.model.WatchFaceConfig
import com.example.previewer.DigitalClockRenderer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HandsTimeEditor(
    config: WatchFaceConfig,
    onConfigChange: (WatchFaceConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section A: Analog Hands Configuration (if ANALOG or HYBRID)
        if (config.clockType == ClockType.ANALOG || config.clockType == ClockType.HYBRID) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Analog Watch Hands",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF00E5FF)
                    )
                    Spacer(Modifier.height(10.dp))

                    // Hand Style
                    Text("Hour & Minute Hand Style", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HandStyle.values().forEach { style ->
                            FilterChip(
                                selected = config.analogHands.hourHandStyle == style,
                                onClick = {
                                    onConfigChange(
                                        config.copy(
                                            analogHands = config.analogHands.copy(
                                                hourHandStyle = style,
                                                minuteHandStyle = style
                                            )
                                        )
                                    )
                                },
                                label = { Text(style.displayName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00E5FF),
                                    selectedLabelColor = Color(0xFF090D16)
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Hour Hand Length
                    Text("Hour Hand Length: ${(config.analogHands.hourHandLength * 100).toInt()}%", fontSize = 12.sp, color = Color.White)
                    Slider(
                        value = config.analogHands.hourHandLength,
                        onValueChange = {
                            onConfigChange(config.copy(analogHands = config.analogHands.copy(hourHandLength = it)))
                        },
                        valueRange = 0.35f..0.65f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                    )

                    // Minute Hand Length
                    Text("Minute Hand Length: ${(config.analogHands.minuteHandLength * 100).toInt()}%", fontSize = 12.sp, color = Color.White)
                    Slider(
                        value = config.analogHands.minuteHandLength,
                        onValueChange = {
                            onConfigChange(config.copy(analogHands = config.analogHands.copy(minuteHandLength = it)))
                        },
                        valueRange = 0.60f..0.90f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                    )

                    Spacer(Modifier.height(8.dp))

                    // Second Hand Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Second Hand Display", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Can be hidden or visible (auto-dimmed in Ambient mode)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = config.analogHands.secondHandVisible,
                            onCheckedChange = {
                                onConfigChange(config.copy(analogHands = config.analogHands.copy(secondHandVisible = it)))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFF59E0B),
                                checkedTrackColor = Color(0xFFD97706)
                            )
                        )
                    }

                    AnimatedVisibility(visible = config.analogHands.secondHandVisible) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text("Second Hand Motion Mechanics", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HandMotion.values().forEach { motion ->
                                    val isSelected = config.analogHands.secondHandMotion == motion
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFF59E0B) else Color(0xFF1E293B))
                                            .clickable {
                                                onConfigChange(
                                                    config.copy(
                                                        analogHands = config.analogHands.copy(secondHandMotion = motion)
                                                    )
                                                )
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            motion.displayName.substringBefore(" ("),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF090D16) else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Ambient Hollow Hands
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hollow Outline in Ambient Mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Dramatically lowers On-Pixel Ratio to keep Ambient OPR < 15%", fontSize = 11.sp, color = Color(0xFF10B981))
                        }
                        Switch(
                            checked = config.analogHands.ambientHollowHands,
                            onCheckedChange = {
                                onConfigChange(config.copy(analogHands = config.analogHands.copy(ambientHollowHands = it)))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF059669)
                            )
                        )
                    }
                }
            }
        }

        // Section B: Digital Clock Configuration (if DIGITAL or HYBRID)
        if (config.clockType == ClockType.DIGITAL || config.clockType == ClockType.HYBRID) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Digital Clock & Date Engine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(Modifier.height(10.dp))

                    // 12h vs 24h & Seconds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("24-Hour Format", fontSize = 12.sp, color = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = config.digitalClock.is24Hour,
                                onCheckedChange = {
                                    onConfigChange(config.copy(digitalClock = config.digitalClock.copy(is24Hour = it)))
                                },
                                modifier = Modifier.testTag("is_24_hour_switch")
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show Seconds", fontSize = 12.sp, color = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = config.digitalClock.showSeconds,
                                onCheckedChange = {
                                    onConfigChange(config.copy(digitalClock = config.digitalClock.copy(showSeconds = it)))
                                },
                                modifier = Modifier.testTag("show_seconds_switch")
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Leading Zero Toggle (09:08 vs 9:08)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B0F19))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Leading Zero on Hours",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                if (config.digitalClock.leadingZero) "Showing leading zero (e.g. 09:08)" else "No leading zero (e.g. 9:08)",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                        Switch(
                            checked = config.digitalClock.leadingZero,
                            onCheckedChange = {
                                onConfigChange(config.copy(digitalClock = config.digitalClock.copy(leadingZero = it)))
                            },
                            modifier = Modifier.testTag("leading_zero_switch")
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Font Family Selector with Live Previews (including Dot Matrix & Digital 7)
                    Text(
                        "Digital Clock Font (${config.digitalClock.fontFamily.displayName})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DigitalFontFamily.values().forEach { font ->
                            val isSelected = config.digitalClock.fontFamily == font
                            Column(
                                modifier = Modifier
                                    .width(104.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF172554) else Color(0xFF0B0F19))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        onConfigChange(config.copy(digitalClock = config.digitalClock.copy(fontFamily = font)))
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .testTag("digital_font_${font.name.lowercase()}"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (font) {
                                        DigitalFontFamily.DIGITAL_7 -> {
                                            Canvas(modifier = Modifier.size(width = 72.dp, height = 22.dp)) {
                                                DigitalClockRenderer.drawSevenSegmentClock(
                                                    drawScope = this,
                                                    center = Offset(size.width / 2f, size.height / 2f),
                                                    digitHeight = size.height * 0.85f,
                                                    hourStr = if (config.digitalClock.leadingZero) "09" else "9",
                                                    minStr = "08",
                                                    secStr = "",
                                                    hourColor = Color.White,
                                                    minColor = if (isSelected) Color(0xFF38BDF8) else Color(0xFF00E5FF),
                                                    secColor = Color.Transparent,
                                                    glowActive = false,
                                                    isAmbient = false
                                                )
                                            }
                                        }
                                        DigitalFontFamily.DOT_MATRIX -> {
                                            Canvas(modifier = Modifier.size(width = 72.dp, height = 22.dp)) {
                                                DigitalClockRenderer.drawDotMatrixClock(
                                                    drawScope = this,
                                                    center = Offset(size.width / 2f, size.height / 2f),
                                                    digitHeight = size.height * 0.82f,
                                                    hourStr = if (config.digitalClock.leadingZero) "09" else "9",
                                                    minStr = "45",
                                                    secStr = "",
                                                    hourColor = Color.White,
                                                    minColor = if (isSelected) Color(0xFF38BDF8) else Color(0xFF10B981),
                                                    secColor = Color.Transparent,
                                                    glowActive = false,
                                                    isAmbient = false
                                                )
                                            }
                                        }
                                        else -> {
                                            Text(
                                                text = font.badgeText,
                                                fontFamily = DigitalClockRenderer.getComposeFontFamily(font),
                                                fontSize = if (font == DigitalFontFamily.RETRO_LED) 19.sp else if (font == DigitalFontFamily.PIXEL_ARCADE) 12.sp else 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF38BDF8) else Color.White
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = font.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Font Size Slider (18sp to 120sp for small dot matrix up to jumbo clock)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Digital Clock Size: ${config.digitalClock.fontSize}sp",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            when {
                                config.digitalClock.fontSize < 34 -> "Small / Compact"
                                config.digitalClock.fontSize < 64 -> "Standard"
                                config.digitalClock.fontSize < 90 -> "Large"
                                else -> "Jumbo / Full Dial"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Slider(
                        value = config.digitalClock.fontSize.toFloat(),
                        onValueChange = {
                            onConfigChange(config.copy(digitalClock = config.digitalClock.copy(fontSize = it.toInt())))
                        },
                        valueRange = 18f..120f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8)),
                        modifier = Modifier.testTag("digital_font_size_slider")
                    )

                    // Quick Size Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Small (26)" to 26,
                            "Medium (48)" to 48,
                            "Big (78)" to 78,
                            "Jumbo (108)" to 108
                        ).forEach { (label, sizeVal) ->
                            FilterChip(
                                selected = config.digitalClock.fontSize == sizeVal,
                                onClick = {
                                    onConfigChange(config.copy(digitalClock = config.digitalClock.copy(fontSize = sizeVal)))
                                },
                                label = { Text(label, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF38BDF8),
                                    selectedLabelColor = Color(0xFF090D16)
                                ),
                                modifier = Modifier.testTag("size_preset_$sizeVal")
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Vertical Position Offset
                    Text("Vertical Position Offset: ${config.digitalClock.positionYOffset.toInt()}dp", fontSize = 12.sp, color = Color.White)
                    Slider(
                        value = config.digitalClock.positionYOffset,
                        onValueChange = {
                            onConfigChange(config.copy(digitalClock = config.digitalClock.copy(positionYOffset = it)))
                        },
                        valueRange = -100f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                    )

                    Spacer(Modifier.height(8.dp))

                    // Date Display Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Date Banner", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Switch(
                            checked = config.digitalClock.showDate,
                            onCheckedChange = {
                                onConfigChange(config.copy(digitalClock = config.digitalClock.copy(showDate = it)))
                            }
                        )
                    }

                    // Glow Effect Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Neon Glow Drop-Shadow", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Soft illuminated glow on digital numerals", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = config.digitalClock.glowEffect,
                            onCheckedChange = {
                                onConfigChange(config.copy(digitalClock = config.digitalClock.copy(glowEffect = it)))
                            }
                        )
                    }
                }
            }
        }
    }
}
