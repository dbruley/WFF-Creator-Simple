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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HelpOutline
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WatchFaceConfig
import com.example.model.WffConditionRule
import com.example.previewer.PreviewSimulationState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpressionsEditor(
    config: WatchFaceConfig,
    simulationState: PreviewSimulationState,
    onConfigChange: (WatchFaceConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var testExpressionInput by remember { mutableStateOf("[BATTERY_PERCENT] < 20") }
    var newRuleTitle by remember { mutableStateOf("") }
    var newRuleExpr by remember { mutableStateOf("[HEART_RATE] > 110") }
    var newRuleTarget by remember { mutableStateOf("Dial Bezel") }
    var newRuleAction by remember { mutableStateOf("Pulse in Warning Color") }

    // Evaluator helper
    val evaluatedResult = remember(testExpressionInput, simulationState) {
        evaluateExpression(testExpressionInput, simulationState)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Educational Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.ElectricBolt,
                    contentDescription = "Dynamic Expressions",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Wear OS 5 - 7 Dynamic Expressions & Conditionals",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        "WFF expressions allow watch faces to react dynamically to sensor telemetry (battery level, ambient display state, heart rate, step goal completion) without CPU wakeups or battery drain.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Live Expression Evaluator Bench
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1424)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Expression Live Tester",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF38BDF8)
                    )

                    // Evaluation outcome badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (evaluatedResult) Color(0xFF10B981) else Color(0xFFF43F5E))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (evaluatedResult) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFF090D16),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (evaluatedResult) "EVALUATES: TRUE" else "EVALUATES: FALSE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF090D16)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = testExpressionInput,
                    onValueChange = { testExpressionInput = it },
                    label = { Text("Formula / Expression") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expression_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Current Telemetry: Battery=${simulationState.simulatedBatteryPercent.toInt()}%, Steps=${simulationState.simulatedSteps}, HR=${simulationState.simulatedHeartRate} bpm, Ambient=${simulationState.isAmbient}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )

                Spacer(Modifier.height(8.dp))

                // Quick Expression Presets
                Text("Insert Common Expressions:", fontSize = 11.sp, color = Color(0xFF64748B))
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "[BATTERY_PERCENT] < 20",
                        "[IS_AMBIENT] == 1",
                        "[STEP_GOAL_PERCENT] >= 100",
                        "[HEART_RATE] > 120",
                        "[IS_AMBIENT] == 0"
                    ).forEach { expr ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { testExpressionInput = expr }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                expr,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
            }
        }

        // Section: Configured Condition Rules
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Active WFF Condition Rules (${config.conditionRules.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
        }

        config.conditionRules.forEachIndexed { index, rule ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                rule.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
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
                                    rule.targetProperty,
                                    fontSize = 9.sp,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        Text(
                            rule.expression,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        Text(
                            "Action: ${rule.conditionAction}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = rule.isEnabled,
                            onCheckedChange = { checked ->
                                val updated = config.conditionRules.mapIndexed { i, r ->
                                    if (i == index) r.copy(isEnabled = checked) else r
                                }
                                onConfigChange(config.copy(conditionRules = updated))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00E5FF),
                                checkedTrackColor = Color(0xFF0284C7)
                            )
                        )

                        if (config.conditionRules.size > 1) {
                            IconButton(
                                onClick = {
                                    val updated = config.conditionRules.filterIndexed { i, _ -> i != index }
                                    onConfigChange(config.copy(conditionRules = updated))
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Rule",
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add New Custom Rule
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Add Custom Wear OS Condition", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = newRuleTitle,
                    onValueChange = { newRuleTitle = it },
                    placeholder = { Text("Rule Title (e.g. High Cardio Alert)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = newRuleExpr,
                    onValueChange = { newRuleExpr = it },
                    placeholder = { Text("Expression (e.g. [HEART_RATE] > 130)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (newRuleTitle.isNotBlank() && newRuleExpr.isNotBlank()) {
                            val newRule = WffConditionRule(
                                id = "rule_${System.currentTimeMillis()}",
                                title = newRuleTitle.trim(),
                                expression = newRuleExpr.trim(),
                                targetProperty = newRuleTarget,
                                conditionAction = newRuleAction,
                                isEnabled = true
                            )
                            onConfigChange(config.copy(conditionRules = config.conditionRules + newRule))
                            newRuleTitle = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_condition_rule_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF090D16), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Condition Rule", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun evaluateExpression(expr: String, state: PreviewSimulationState): Boolean {
    val clean = expr.trim()
    return try {
        when {
            clean.contains("[BATTERY_PERCENT] < 20") -> state.simulatedBatteryPercent < 20f
            clean.contains("[BATTERY_PERCENT] < 15") -> state.simulatedBatteryPercent < 15f
            clean.contains("[BATTERY_PERCENT] >= 90") -> state.simulatedBatteryPercent >= 90f
            clean.contains("[IS_AMBIENT] == 1") -> state.isAmbient
            clean.contains("[IS_AMBIENT] == 0") -> !state.isAmbient
            clean.contains("[STEP_GOAL_PERCENT] >= 100") -> state.simulatedSteps >= 10000
            clean.contains("[HEART_RATE] > 120") -> state.simulatedHeartRate > 120
            clean.contains("[HEART_RATE] > 110") -> state.simulatedHeartRate > 110
            clean.contains("[HEART_RATE] > 100") -> state.simulatedHeartRate > 100
            else -> false
        }
    } catch (_: Exception) {
        false
    }
}
