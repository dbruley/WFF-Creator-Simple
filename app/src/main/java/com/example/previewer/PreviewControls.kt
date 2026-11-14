package com.example.previewer

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import com.example.generator.ValidationReport

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewControls(
    state: PreviewSimulationState,
    report: ValidationReport,
    onStateChange: (PreviewSimulationState) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
        color = Color(0xFF0F172A),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Row 1: Mode Switcher (Active vs Ambient) & Guidelines & Live Clock Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ambient Mode Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (!state.isAmbient) Color(0xFF00E5FF) else Color.Transparent)
                            .clickable { onStateChange(state.copy(isAmbient = false)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("mode_active_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Active Mode",
                                tint = if (!state.isAmbient) Color(0xFF090D16) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!state.isAmbient) Color(0xFF090D16) else Color(0xFF94A3B8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (state.isAmbient) Color(0xFFF59E0B) else Color.Transparent)
                            .clickable { onStateChange(state.copy(isAmbient = true)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("mode_ambient_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Ambient Mode",
                                tint = if (state.isAmbient) Color(0xFF090D16) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Ambient (AOD)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isAmbient) Color(0xFF090D16) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Guidelines Toggle
                FilterChip(
                    selected = state.showGuidelines,
                    onClick = { onStateChange(state.copy(showGuidelines = !state.showGuidelines)) },
                    label = { Text("Grid & Safe-Zone", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.GridOn,
                            contentDescription = "Toggle Grid",
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x3300E5FF),
                        selectedLabelColor = Color(0xFF00E5FF)
                    ),
                    modifier = Modifier.testTag("guidelines_toggle")
                )
            }

            Spacer(Modifier.height(10.dp))

            // Row 2: Wear OS Battery & OPR (On-Pixel Ratio) telemetry badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF131D31))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val opr = if (state.isAmbient) report.ambientOprPercentage else report.activeOprPercentage
                    val isPass = !state.isAmbient || report.isAmbientCompliant
                    Icon(
                        imageVector = if (isPass) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "OPR status",
                        tint = if (isPass) Color(0xFF10B981) else Color(0xFFF43F5E),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (state.isAmbient) "Ambient OPR: ${String.format("%.1f", opr)}%" else "Active OPR: ${String.format("%.1f", opr)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                Text(
                    text = if (state.isAmbient) "Budget: <15% (Wear OS Spec)" else "Recommended: <35%",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Row 3: Interactive Time Scrubbing / Live Clock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time Simulator: ${String.format("%02d:%02d:%02d", state.simulatedHour, state.simulatedMinute, state.simulatedSecond.toInt())}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF38BDF8)
                )

                Button(
                    onClick = { onStateChange(state.copy(isLiveTime = !state.isLiveTime)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isLiveTime) Color(0xFFF43F5E) else Color(0xFF2563EB)
                    ),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("live_clock_toggle")
                ) {
                    Icon(
                        imageVector = if (state.isLiveTime) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Live clock",
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.isLiveTime) "Pause Clock" else "Live Clock", fontSize = 11.sp)
                }
            }

            AnimatedVisibility(visible = !state.isLiveTime) {
                Column {
                    // Hour Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hour", fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(44.dp))
                        Slider(
                            value = state.simulatedHour.toFloat(),
                            onValueChange = { onStateChange(state.copy(simulatedHour = it.toInt())) },
                            valueRange = 0f..23f,
                            steps = 22,
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .testTag("hour_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF)
                            )
                        )
                        Text("${state.simulatedHour}h", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(32.dp))
                    }

                    // Minute Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Min", fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(44.dp))
                        Slider(
                            value = state.simulatedMinute.toFloat(),
                            onValueChange = { onStateChange(state.copy(simulatedMinute = it.toInt())) },
                            valueRange = 0f..59f,
                            steps = 58,
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .testTag("minute_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF38BDF8)
                            )
                        )
                        Text("${state.simulatedMinute}m", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(32.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Row 4: Complication Telemetry Quick-Scenarios
            Text(
                "Simulate Watch Complication Telemetry:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )

            Spacer(Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AssistChip(
                    onClick = {
                        onStateChange(
                            state.copy(
                                simulatedBatteryPercent = 12f,
                                simulatedSteps = 2300,
                                simulatedHeartRate = 65,
                                simulatedTemp = "18°C"
                            )
                        )
                    },
                    label = { Text("Low Battery (12%)", fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFF43F5E)),
                    modifier = Modifier.testTag("scenario_low_battery")
                )
                AssistChip(
                    onClick = {
                        onStateChange(
                            state.copy(
                                simulatedBatteryPercent = 94f,
                                simulatedSteps = 12540,
                                simulatedHeartRate = 148,
                                simulatedTemp = "24°C"
                            )
                        )
                    },
                    label = { Text("Workout (12.5k steps / 148 bpm)", fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF10B981)),
                    modifier = Modifier.testTag("scenario_workout")
                )
                AssistChip(
                    onClick = {
                        onStateChange(
                            state.copy(
                                simulatedBatteryPercent = 78f,
                                simulatedSteps = 8430,
                                simulatedHeartRate = 72,
                                simulatedTemp = "22°C"
                            )
                        )
                    },
                    label = { Text("Standard Day", fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF38BDF8)),
                    modifier = Modifier.testTag("scenario_standard")
                )
            }
        }
    }
}
