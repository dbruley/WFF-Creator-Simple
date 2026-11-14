package com.example.editor

import com.example.previewer.parseHexColor
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ComplicationPosition
import com.example.model.ComplicationSlotConfig
import com.example.model.ComplicationType
import com.example.model.SimulatedDataSource
import com.example.model.WatchFaceConfig

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComplicationsEditor(
    config: WatchFaceConfig,
    onConfigChange: (WatchFaceConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var expandedSlotId by remember { mutableStateOf<Int?>(config.complications.firstOrNull()?.slotId) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Wear OS Complication Slots",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    "Configured slots: ${config.complications.count { it.enabled }} of ${config.complications.size}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            if (config.complications.size < 6) {
                Button(
                    onClick = {
                        val newId = (config.complications.maxOfOrNull { it.slotId } ?: -1) + 1
                        val availablePos = ComplicationPosition.values().firstOrNull { pos ->
                            config.complications.none { it.position == pos }
                        } ?: ComplicationPosition.BOTTOM
                        val newSlot = ComplicationSlotConfig(
                            slotId = newId,
                            name = "Slot #$newId",
                            position = availablePos,
                            enabled = true,
                            slotType = if (config.targetWearOsVersion >= 7) ComplicationType.WEIGHTED_ARC else ComplicationType.RANGED_VALUE,
                            dataSource = SimulatedDataSource.BATTERY,
                            accentColor = "#00E5FF"
                        )
                        onConfigChange(config.copy(complications = config.complications + newSlot))
                        expandedSlotId = newId
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    modifier = Modifier.height(34.dp).testTag("add_slot_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add slot", tint = Color(0xFF090D16), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Slot", fontSize = 12.sp, color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            }
        }

        // List each complication slot as an expandable, highly-detailed card
        config.complications.forEach { slot ->
            val isExpanded = expandedSlotId == slot.slotId
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded) Color(0xFF162035) else Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isExpanded) 1.5.dp else 1.dp,
                        color = if (isExpanded) Color(0xFF00E5FF) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedSlotId = if (isExpanded) null else slot.slotId },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(slot.accentColor))
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Slot #${slot.slotId}: ${slot.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    "${slot.position.displayName} • ${slot.slotType.displayName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = slot.enabled,
                                onCheckedChange = { checked ->
                                    val updated = config.complications.map {
                                        if (it.slotId == slot.slotId) it.copy(enabled = checked) else it
                                    }
                                    onConfigChange(config.copy(complications = updated))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00E5FF),
                                    checkedTrackColor = Color(0xFF0284C7)
                                )
                            )

                            if (config.complications.size > 1) {
                                IconButton(
                                    onClick = {
                                        val updated = config.complications.filter { it.slotId != slot.slotId }
                                        onConfigChange(config.copy(complications = updated))
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Slot",
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Expanded Details
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Slot Name
                            OutlinedTextField(
                                value = slot.name,
                                onValueChange = { newName ->
                                    val updated = config.complications.map {
                                        if (it.slotId == slot.slotId) it.copy(name = newName) else it
                                    }
                                    onConfigChange(config.copy(complications = updated))
                                },
                                label = { Text("Slot Display Label") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            // Position
                            Text("Dial Position", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ComplicationPosition.values().forEach { pos ->
                                    FilterChip(
                                        selected = slot.position == pos,
                                        onClick = {
                                            val updated = config.complications.map {
                                                if (it.slotId == slot.slotId) it.copy(position = pos) else it
                                            }
                                            onConfigChange(config.copy(complications = updated))
                                        },
                                        label = { Text(pos.displayName, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF090D16)
                                        )
                                    )
                                }
                            }

                            // Coordinate Fine-Tuning
                            val currentAngle = slot.customAngle ?: slot.position.angleDeg
                            val currentRadius = slot.customRadiusFactor ?: slot.position.radiusFactor

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Position Angle: ${currentAngle.toInt()}°", fontSize = 11.sp, color = Color.White)
                                if (slot.customAngle != null || slot.customRadiusFactor != null) {
                                    Text(
                                        "Reset to Preset",
                                        fontSize = 11.sp,
                                        color = Color(0xFF00E5FF),
                                        modifier = Modifier.clickable {
                                            val updated = config.complications.map {
                                                if (it.slotId == slot.slotId) it.copy(customAngle = null, customRadiusFactor = null) else it
                                            }
                                            onConfigChange(config.copy(complications = updated))
                                        }
                                    )
                                }
                            }
                            Slider(
                                value = currentAngle,
                                onValueChange = { angle ->
                                    val updated = config.complications.map {
                                        if (it.slotId == slot.slotId) it.copy(customAngle = angle) else it
                                    }
                                    onConfigChange(config.copy(complications = updated))
                                },
                                valueRange = 0f..360f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                            )

                            Text("Distance from Center: ${(currentRadius * 100).toInt()}%", fontSize = 11.sp, color = Color.White)
                            Slider(
                                value = currentRadius,
                                onValueChange = { r ->
                                    val updated = config.complications.map {
                                        if (it.slotId == slot.slotId) it.copy(customRadiusFactor = r) else it
                                    }
                                    onConfigChange(config.copy(complications = updated))
                                },
                                valueRange = 0.20f..0.80f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                            )

                            // Slot Type
                            Text("WFF Complication Type", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ComplicationType.values().forEach { type ->
                                    val isWff7 = type == ComplicationType.WEIGHTED_ARC
                                    FilterChip(
                                        selected = slot.slotType == type,
                                        onClick = {
                                            val updated = config.complications.map {
                                                if (it.slotId == slot.slotId) it.copy(slotType = type) else it
                                            }
                                            onConfigChange(config.copy(complications = updated))
                                        },
                                        label = {
                                            Text(
                                                if (isWff7) "${type.displayName}" else type.displayName,
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (isWff7) Color(0xFFF59E0B) else Color(0xFF10B981),
                                            selectedLabelColor = Color(0xFF090D16)
                                        )
                                    )
                                }
                            }

                            // Data Source Provider
                            Text("Default Wear OS Data Provider", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SimulatedDataSource.values().forEach { ds ->
                                    FilterChip(
                                        selected = slot.dataSource == ds,
                                        onClick = {
                                            val updated = config.complications.map {
                                                if (it.slotId == slot.slotId) it.copy(dataSource = ds) else it
                                            }
                                            onConfigChange(config.copy(complications = updated))
                                        },
                                        label = { Text(ds.displayName, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF00E5FF),
                                            selectedLabelColor = Color(0xFF090D16)
                                        )
                                    )
                                }
                            }

                            // Accent Color for this slot
                            Text("Gauge / Subdial Accent Color", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "#00E5FF", // Cyan
                                    "#10B981", // Emerald
                                    "#F59E0B", // Amber
                                    "#F43F5E", // Coral
                                    "#38BDF8", // Sky
                                    "#A855F7", // Purple
                                    "#FFFFFF"  // White
                                ).forEach { hex ->
                                    val isColSelected = slot.accentColor.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(hex))
                                            .border(
                                                width = if (isColSelected) 2.5.dp else 1.dp,
                                                color = if (isColSelected) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                val updated = config.complications.map {
                                                    if (it.slotId == slot.slotId) it.copy(accentColor = hex) else it
                                                }
                                                onConfigChange(config.copy(complications = updated))
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
