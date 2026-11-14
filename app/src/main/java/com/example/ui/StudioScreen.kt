package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.ComplicationsEditor
import com.example.editor.DialThemeEditor
import com.example.editor.ExpressionsEditor
import com.example.editor.FlavorsEditor
import com.example.editor.HandsTimeEditor
import com.example.editor.SavedProjectsScreen
import com.example.editor.WffCodeExportScreen
import com.example.previewer.PreviewControls
import com.example.previewer.WatchFaceCanvas
import com.example.viewmodel.WatchFaceViewModel

enum class StudioTab(val title: String, val icon: ImageVector) {
    DIAL("Dial & Theme", Icons.Default.Palette),
    HANDS("Hands & Time", Icons.Default.Schedule),
    COMPLICATIONS("Complications", Icons.Default.Tune),
    EXPRESSIONS("Expressions (WFF 7)", Icons.Default.ElectricBolt),
    FLAVORS("Flavors", Icons.Default.ColorLens),
    CODE_EXPORT("Export & Watch", Icons.Default.Code),
    MY_FACES("My Faces", Icons.Default.Folder)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: WatchFaceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeConfig by viewModel.activeConfig.collectAsState()
    val simulationState by viewModel.simulationState.collectAsState()
    val validationReport by viewModel.validationReport.collectAsState()
    val savedProjects by viewModel.savedProjects.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isBottomPanelExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = isBottomPanelExpanded) {
        isBottomPanelExpanded = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Studio Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00E5FF))
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Watch,
                        contentDescription = "Watch Face Studio",
                        tint = Color(0xFF090D16),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeConfig.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Wear OS ${activeConfig.targetWearOsVersion}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                    Text(
                        text = "Watch Face Format Studio (v${when (activeConfig.targetWearOsVersion) { 4 -> "1"; 5 -> "2"; 6 -> "3"; else -> "4" }})",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Save & Export Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        selectedTabIndex = StudioTab.CODE_EXPORT.ordinal
                        isBottomPanelExpanded = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(17.dp))
                        .testTag("top_export_watch_button")
                ) {
                    Icon(
                        Icons.Default.Watch,
                        contentDescription = "Export to Pixel Watch",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = {
                        viewModel.saveCurrentProject {
                            Toast.makeText(context, "Saved '${activeConfig.name}'!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("save_project_button")
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Save",
                        tint = Color(0xFF090D16),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF090D16))
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val targetPanelHeight = if (isBottomPanelExpanded) {
                (maxHeight * 0.56f).coerceIn(280.dp, 460.dp)
            } else {
                0.dp
            }
            val animatedPanelHeight by animateDpAsState(
                targetValue = targetPanelHeight,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "bottom_panel_height"
            )
            val animatedWatchSize by animateDpAsState(
                targetValue = if (isBottomPanelExpanded) 168.dp else 230.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "watch_canvas_size"
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Integrated Previewer & Telemetry Test Bench (Top section)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Live Watch Face Canvas
                    Box(
                        modifier = Modifier
                            .size(animatedWatchSize)
                            .clip(if (activeConfig.shape.isCircular) CircleShape else RoundedCornerShape(12.dp))
                            .border(
                                width = 2.dp,
                                color = if (simulationState.isAmbient) Color(0xFF334155) else Color(0xFF00E5FF).copy(alpha = 0.6f),
                                shape = if (activeConfig.shape.isCircular) CircleShape else RoundedCornerShape(12.dp)
                            )
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        WatchFaceCanvas(
                            config = activeConfig,
                            state = simulationState,
                            modifier = Modifier.fillMaxSize(),
                            onComplicationClicked = { slotId ->
                                viewModel.updateSimulationState(
                                    simulationState.copy(selectedSlotId = slotId)
                                )
                                selectedTabIndex = 2 // Switch to Complications tab
                                isBottomPanelExpanded = true
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Interactive Live Controls for scrubbing time, toggling ambient, and testing telemetry
                    PreviewControls(
                        state = simulationState,
                        report = validationReport,
                        onStateChange = { viewModel.updateSimulationState(it) }
                    )

                    Spacer(Modifier.height(8.dp))
                }

                // Expandable Bottom Drawer: Tool Tabs Bar + Options Space Below
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        .border(
                            width = 1.dp,
                            color = if (isBottomPanelExpanded) Color(0xFF00E5FF).copy(alpha = 0.45f) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                        ),
                    color = Color(0xFF0D1424),
                    tonalElevation = 6.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Subtle Handle & Status Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isBottomPanelExpanded = !isBottomPanelExpanded }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("bottom_bar_handle"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBottomPanelExpanded) {
                                    "Editing: ${StudioTab.values()[selectedTabIndex].title} (Tap tab again to close)"
                                } else {
                                    "Tap a tool below to slide up options"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isBottomPanelExpanded) Color(0xFF00E5FF) else Color(0xFF94A3B8)
                            )

                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isBottomPanelExpanded) Color(0xFF00E5FF) else Color(0xFF334155))
                            )

                            Icon(
                                imageVector = if (isBottomPanelExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isBottomPanelExpanded) "Collapse tool options" else "Expand tool options",
                                tint = if (isBottomPanelExpanded) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Tool Tabs Navigation Bar (starts with "Dial & Theme")
                        ScrollableTabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color(0xFF0D1424),
                            contentColor = Color(0xFF00E5FF),
                            edgePadding = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StudioTab.values().forEachIndexed { index, tab ->
                                val isTabSelected = isBottomPanelExpanded && selectedTabIndex == index
                                Tab(
                                    selected = isTabSelected,
                                    onClick = {
                                        if (!isBottomPanelExpanded) {
                                            selectedTabIndex = index
                                            isBottomPanelExpanded = true
                                        } else if (selectedTabIndex == index) {
                                            // Second tap on the active button collapses the bar back down
                                            isBottomPanelExpanded = false
                                        } else {
                                            selectedTabIndex = index
                                        }
                                    },
                                    text = {
                                        Text(
                                            tab.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            tab.icon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    selectedContentColor = Color(0xFF00E5FF),
                                    unselectedContentColor = if (selectedTabIndex == index && !isBottomPanelExpanded) {
                                        Color(0xFFE2E8F0)
                                    } else {
                                        Color(0xFF94A3B8)
                                    },
                                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                                )
                            }
                        }

                        // Active Tool Workspace Content (Slides up below the tab bar when expanded)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(animatedPanelHeight)
                                .background(Color(0xFF090D16))
                        ) {
                            if (animatedPanelHeight > 0.dp) {
                                AnimatedContent(
                                    targetState = selectedTabIndex,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "tab_content_animation"
                                ) { tabIdx ->
                                    when (StudioTab.values()[tabIdx]) {
                                        StudioTab.DIAL -> {
                                            DialThemeEditor(
                                                config = activeConfig,
                                                onConfigChange = { viewModel.updateConfig(it) }
                                            )
                                        }
                                        StudioTab.HANDS -> {
                                            HandsTimeEditor(
                                                config = activeConfig,
                                                onConfigChange = { viewModel.updateConfig(it) }
                                            )
                                        }
                                        StudioTab.COMPLICATIONS -> {
                                            ComplicationsEditor(
                                                config = activeConfig,
                                                onConfigChange = { viewModel.updateConfig(it) }
                                            )
                                        }
                                        StudioTab.EXPRESSIONS -> {
                                            ExpressionsEditor(
                                                config = activeConfig,
                                                simulationState = simulationState,
                                                onConfigChange = { viewModel.updateConfig(it) }
                                            )
                                        }
                                        StudioTab.FLAVORS -> {
                                            FlavorsEditor(
                                                config = activeConfig,
                                                onConfigChange = { viewModel.updateConfig(it) }
                                            )
                                        }
                                        StudioTab.CODE_EXPORT -> {
                                            WffCodeExportScreen(
                                                config = activeConfig
                                            )
                                        }
                                        StudioTab.MY_FACES -> {
                                            SavedProjectsScreen(
                                                savedList = savedProjects,
                                                activeConfig = activeConfig,
                                                onSelectProject = { cfg, id ->
                                                    viewModel.loadProject(cfg, id)
                                                    selectedTabIndex = 0
                                                },
                                                onCreateNew = {
                                                    viewModel.createNewProject()
                                                    selectedTabIndex = 0
                                                },
                                                onDeleteProject = { id -> viewModel.deleteProject(id) },
                                                onDuplicateProject = { ent -> viewModel.duplicateProject(ent) },
                                                onToggleFavorite = { id, fav -> viewModel.toggleFavorite(id, fav) },
                                                onExportProject = { cfg, id ->
                                                    viewModel.loadProject(cfg, id)
                                                    selectedTabIndex = StudioTab.CODE_EXPORT.ordinal
                                                    isBottomPanelExpanded = true
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
    }
}
