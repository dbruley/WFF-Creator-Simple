package com.example.editor

import com.example.previewer.parseHexColor
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.model.BezelStyle
import com.example.model.ClockType
import com.example.model.DialBackgroundStyle
import com.example.model.WatchFaceConfig
import com.example.model.WatchShape

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialThemeEditor(
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
        // Project Name
        OutlinedTextField(
            value = config.name,
            onValueChange = { onConfigChange(config.copy(name = it)) },
            label = { Text("Watch Face Name") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("watch_face_name_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        // Target Wear OS Version
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Target Wear OS Version (Watch Face Format)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    "Select WFF specification version. Wear OS 7 provides multi-tier flavors, weighted multi-arcs, and dynamic expressions.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair(4, "Wear OS 4\n(WFF v1)"),
                        Pair(5, "Wear OS 5\n(WFF v2)"),
                        Pair(6, "Wear OS 6\n(WFF v3)"),
                        Pair(7, "Wear OS 7 ★\n(WFF v4)")
                    ).forEach { (ver, label) ->
                        val isSelected = config.targetWearOsVersion == ver
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B))
                                .clickable { onConfigChange(config.copy(targetWearOsVersion = ver)) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF090D16) else Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Clock Engine Type
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Clock Engine Type", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClockType.values().forEach { type ->
                        val isSelected = config.clockType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B))
                                .clickable { onConfigChange(config.copy(clockType = type)) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.displayName.substringBefore(" "),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF090D16) else Color.White
                            )
                        }
                    }
                }
            }
        }

        // Shape & Resolution
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Device Screen Shape & Clip", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WatchShape.values().forEach { shape ->
                        FilterChip(
                            selected = config.shape == shape,
                            onClick = { onConfigChange(config.copy(shape = shape)) },
                            label = { Text(shape.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E5FF),
                                selectedLabelColor = Color(0xFF090D16)
                            )
                        )
                    }
                }
            }
        }

        // Dial Bezel Style
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Outer Bezel & Index Style", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BezelStyle.values().forEach { bezel ->
                        FilterChip(
                            selected = config.bezelStyle == bezel,
                            onClick = { onConfigChange(config.copy(bezelStyle = bezel)) },
                            label = { Text(bezel.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF59E0B),
                                selectedLabelColor = Color(0xFF090D16)
                            )
                        )
                    }
                }
            }
        }

        // Dial Background Texture
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Dial Background Texture", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DialBackgroundStyle.values().forEach { bg ->
                        FilterChip(
                            selected = config.dialBackgroundStyle == bg,
                            onClick = { onConfigChange(config.copy(dialBackgroundStyle = bg)) },
                            label = { Text(bg.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color(0xFF090D16)
                            )
                        )
                    }
                }
            }
        }

        // Theme Palette Colors
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Primary Theme Accent Color", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "#00E5FF", // Cyan
                        "#F59E0B", // Amber
                        "#10B981", // Emerald
                        "#F43F5E", // Coral
                        "#A855F7", // Violet
                        "#FACC15", // Gold
                        "#E2E8F0"  // Platinum
                    ).forEach { hex ->
                        val isSelected = config.themeColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onConfigChange(
                                        config.copy(
                                            themeColor = hex,
                                            bezelColor = hex,
                                            digitalClock = config.digitalClock.copy(minuteColor = hex)
                                        )
                                    )
                                }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.themeColor,
                    onValueChange = { hex ->
                        val formatted = if (hex.startsWith("#")) hex else "#$hex"
                        if (formatted.length <= 7) {
                            onConfigChange(
                                config.copy(
                                    themeColor = formatted,
                                    bezelColor = formatted,
                                    digitalClock = config.digitalClock.copy(minuteColor = formatted)
                                )
                            )
                        }
                    },
                    label = { Text("Custom Theme Hex (#RRGGBB)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(14.dp))
                Text("Secondary Accent Color", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "#F59E0B", // Amber
                        "#EF4444", // Red
                        "#00E5FF", // Cyan
                        "#10B981", // Emerald
                        "#38BDF8", // Sky
                        "#F472B6"  // Pink
                    ).forEach { hex ->
                        val isSelected = config.secondaryColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onConfigChange(config.copy(secondaryColor = hex))
                                }
                        )
                    }
                }
            }
        }

        // Inner Accent Ring Switch
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Inner Decorative Bezel Ring", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Text("Draws an inner technical sub-dial boundary", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
                Switch(
                    checked = config.innerRingVisible,
                    onCheckedChange = { onConfigChange(config.copy(innerRingVisible = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00E5FF),
                        checkedTrackColor = Color(0xFF0284C7)
                    )
                )
            }
        }
    }
}
