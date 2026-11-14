package com.example.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.DefaultPresets
import com.example.data.WatchFaceEntity
import com.example.data.WatchFaceJsonHelper
import com.example.model.WatchFaceConfig
import com.example.previewer.PreviewSimulationState
import com.example.previewer.WatchFaceCanvas
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedProjectsScreen(
    savedList: List<WatchFaceEntity>,
    activeConfig: WatchFaceConfig,
    onSelectProject: (WatchFaceConfig, Long) -> Unit,
    onCreateNew: () -> Unit,
    onDeleteProject: (Long) -> Unit,
    onDuplicateProject: (WatchFaceEntity) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onExportProject: ((WatchFaceConfig, Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Create New Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Create Watch Face",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            "Build a custom Wear OS 7 Watch Face Format design from scratch",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Button(
                        onClick = onCreateNew,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        modifier = Modifier.testTag("create_new_watch_face_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New", tint = Color(0xFF090D16))
                        Spacer(Modifier.width(4.dp))
                        Text("New", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Saved Projects
        item {
            Text(
                "My Saved Watch Faces (${savedList.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (savedList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Watch,
                            contentDescription = "No saved designs",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No saved watch faces yet",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            "Tap 'New' or load one of the presets below to get started.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        } else {
            items(savedList, key = { it.id }) { entity ->
                val projectConfig = WatchFaceJsonHelper.fromJson(entity.configJson)
                val isActive = activeConfig.id == projectConfig.id

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) Color(0xFF16233B) else Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) Color(0xFF00E5FF) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectProject(projectConfig, entity.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini Watch Canvas preview
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        ) {
                            WatchFaceCanvas(
                                config = projectConfig,
                                state = PreviewSimulationState(
                                    isAmbient = false,
                                    simulatedHour = 10,
                                    simulatedMinute = 8,
                                    simulatedSecond = 32f
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    entity.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                if (isActive) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF00E5FF))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "ACTIVE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF090D16)
                                        )
                                    }
                                }
                            }

                            Text(
                                "Wear OS ${entity.targetWearOsVersion} (WFF v${when (entity.targetWearOsVersion) { 4 -> "1"; 5 -> "2"; 6 -> "3"; else -> "4" }}) • ${entity.clockType}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )

                            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(entity.updatedAt))
                            Text(
                                "Modified: $dateStr",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onExportProject != null) {
                                IconButton(
                                    onClick = { onExportProject(projectConfig, entity.id) },
                                    modifier = Modifier.testTag("export_saved_project_${entity.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Watch,
                                        contentDescription = "Export to Pixel Watch",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onToggleFavorite(entity.id, !entity.isFavorite) }
                            ) {
                                Icon(
                                    imageVector = if (entity.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (entity.isFavorite) Color(0xFFF43F5E) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDuplicateProject(entity) }
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDeleteProject(entity.id) }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Presets Library
        item {
            Text(
                "Presets & Templates",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 14.dp)
            )
        }

        items(DefaultPresets.presets, key = { it.id }) { preset ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1424)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .clickable {
                        onSelectProject(preset, -1L)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    ) {
                        WatchFaceCanvas(
                            config = preset,
                            state = PreviewSimulationState(
                                isAmbient = false,
                                simulatedHour = 10,
                                simulatedMinute = 8,
                                simulatedSecond = 32f
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            preset.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            "Wear OS ${preset.targetWearOsVersion} • ${preset.clockType.displayName} • ${preset.bezelStyle.displayName}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (onExportProject != null) {
                            Button(
                                onClick = { onExportProject(preset, -1L) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF090D16))
                            }
                        }

                        Button(
                            onClick = { onSelectProject(preset, -1L) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Load", fontSize = 11.sp, color = Color(0xFF00E5FF))
                        }
                    }
                }
            }
        }
    }
}
