package com.example.editor

import com.example.previewer.parseHexColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.model.FlavorProfile
import com.example.model.WatchFaceConfig

@Composable
fun FlavorsEditor(
    config: WatchFaceConfig,
    onConfigChange: (WatchFaceConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var newFlavorName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Educational Banner on Wear OS Flavors
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Info",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Wear OS 5 - 7 Flavors Specification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        "Flavors are pre-packaged design profiles embedded in the WFF XML. On Wear OS watches, users can scroll through these in the native watch face picker to select their preferred palette instantly.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // List of Flavors
        Text(
            "Configured Flavors (${config.availableFlavors.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White
        )

        config.availableFlavors.forEachIndexed { index, flavor ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Palette Swatch preview
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .padding(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(flavor.primaryColor))
                            )
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(flavor.secondaryColor))
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Text(
                                flavor.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                "ID: ${flavor.id} • ${flavor.primaryColor} / ${flavor.secondaryColor}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Row {
                        // Apply flavor to live canvas button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .clickable {
                                    onConfigChange(
                                        config.copy(
                                            themeColor = flavor.primaryColor,
                                            secondaryColor = flavor.secondaryColor,
                                            bezelColor = flavor.primaryColor,
                                            digitalClock = config.digitalClock.copy(minuteColor = flavor.primaryColor)
                                        )
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Apply", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold)
                        }

                        if (config.availableFlavors.size > 1) {
                            IconButton(
                                onClick = {
                                    val updated = config.availableFlavors.filterIndexed { i, _ -> i != index }
                                    onConfigChange(config.copy(availableFlavors = updated))
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Flavor",
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add New Flavor
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Add Custom Flavor", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newFlavorName,
                        onValueChange = { newFlavorName = it },
                        placeholder = { Text("e.g. Neon Emerald") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_flavor_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newFlavorName.isNotBlank()) {
                                val id = newFlavorName.lowercase().replace(" ", "_")
                                val newFlavor = FlavorProfile(
                                    id = id,
                                    displayName = newFlavorName.trim(),
                                    primaryColor = config.themeColor,
                                    secondaryColor = config.secondaryColor,
                                    backgroundColor = "#090D16"
                                )
                                onConfigChange(config.copy(availableFlavors = config.availableFlavors + newFlavor))
                                newFlavorName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        modifier = Modifier.testTag("save_flavor_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF090D16))
                    }
                }
            }
        }
    }
}
