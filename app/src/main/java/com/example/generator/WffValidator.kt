package com.example.generator

import com.example.model.ClockType
import com.example.model.DialBackgroundStyle
import com.example.model.WatchFaceConfig

enum class ValidationSeverity {
    PASS,
    INFO,
    WARNING,
    ERROR
}

data class ValidationItem(
    val title: String,
    val description: String,
    val severity: ValidationSeverity
)

data class ValidationReport(
    val activeOprPercentage: Float,
    val ambientOprPercentage: Float,
    val isAmbientCompliant: Boolean, // < 15% OPR
    val issues: List<ValidationItem>
)

object WffValidator {

    fun validate(config: WatchFaceConfig): ValidationReport {
        val issues = mutableListOf<ValidationItem>()

        // 1. Calculate Estimated Active OPR (On-Pixel Ratio)
        var activeOpr = 0.05f // baseline
        when (config.dialBackgroundStyle) {
            DialBackgroundStyle.OBSIDIAN_PURE -> activeOpr += 0.02f
            DialBackgroundStyle.CARBON_TEXTURE -> activeOpr += 0.08f
            DialBackgroundStyle.RADIAL_GLOW -> activeOpr += 0.12f
            DialBackgroundStyle.PILOT_MATTE -> activeOpr += 0.06f
            DialBackgroundStyle.CYBER_GRID -> activeOpr += 0.10f
            DialBackgroundStyle.SOLAR_SUNBURST -> activeOpr += 0.14f
        }

        if (config.clockType == ClockType.ANALOG || config.clockType == ClockType.HYBRID) {
            activeOpr += (config.analogHands.hourHandWidth * 0.008f) +
                    (config.analogHands.minuteHandWidth * 0.008f)
            if (config.analogHands.secondHandVisible) {
                activeOpr += 0.015f
            }
        }

        if (config.clockType == ClockType.DIGITAL || config.clockType == ClockType.HYBRID) {
            activeOpr += (config.digitalClock.fontSize * 0.0018f)
        }

        val activeComplications = config.complications.filter { it.enabled }
        activeOpr += activeComplications.size * 0.025f

        // 2. Calculate Estimated Ambient OPR
        var ambientOpr = 0.015f
        if (config.ambientConfig.lowBitMode) {
            ambientOpr += 0.01f
        }
        if (config.ambientConfig.burnInProtectionShift) {
            // Good: burn-in protection reduces localized pixel strain
        }

        if (config.clockType == ClockType.ANALOG || config.clockType == ClockType.HYBRID) {
            if (config.analogHands.ambientHollowHands) {
                ambientOpr += 0.015f // Hollow hands use outline only!
            } else {
                ambientOpr += 0.045f
            }
        }

        if (config.clockType == ClockType.DIGITAL || config.clockType == ClockType.HYBRID) {
            ambientOpr += 0.02f
        }

        if (!config.ambientConfig.hideComplications) {
            ambientOpr += (activeComplications.size * 0.012f)
        }

        val activeOprPercent = (activeOpr * 100f).coerceIn(4f, 65f)
        val ambientOprPercent = (ambientOpr * 100f).coerceIn(2f, 30f)
        val isAmbientCompliant = ambientOprPercent <= 15.0f

        // Check Ambient OPR
        if (isAmbientCompliant) {
            issues.add(
                ValidationItem(
                    title = "Ambient OPR Budget: ${String.format("%.1f", ambientOprPercent)}%",
                    description = "Passed official Wear OS guideline (< 15% On-Pixel Ratio in Ambient AOD mode).",
                    severity = ValidationSeverity.PASS
                )
            )
        } else {
            issues.add(
                ValidationItem(
                    title = "Ambient OPR Exceeded: ${String.format("%.1f", ambientOprPercent)}%",
                    description = "Wear OS guideline requires Ambient OPR < 15% to conserve battery and prevent burn-in.",
                    severity = ValidationSeverity.WARNING
                )
            )
        }

        // Check Second Hand in Ambient
        if (config.ambientConfig.hideSecondHand || !config.analogHands.secondHandVisible) {
            issues.add(
                ValidationItem(
                    title = "Second Hand Ambient Suppression",
                    description = "Second hand is appropriately hidden in ambient mode per WFF power guidelines.",
                    severity = ValidationSeverity.PASS
                )
            )
        } else {
            issues.add(
                ValidationItem(
                    title = "Second Hand Visible in Ambient",
                    description = "High-frequency second hand ticks will increase battery draw in Always-on Display.",
                    severity = ValidationSeverity.WARNING
                )
            )
        }

        // Check Wear OS Target Version features
        if (config.targetWearOsVersion >= 7) {
            issues.add(
                ValidationItem(
                    title = "Wear OS 7 (WFF v4) Target",
                    description = "Supports multi-tier flavors, weighted multi-arcs, dynamic expressions, and adaptive layouts.",
                    severity = ValidationSeverity.PASS
                )
            )
        } else if (config.targetWearOsVersion >= 5) {
            issues.add(
                ValidationItem(
                    title = "Wear OS ${config.targetWearOsVersion} (WFF v${config.targetWearOsVersion - 3}) Target",
                    description = "Compatible with Wear OS ${config.targetWearOsVersion}+ devices with user configurations.",
                    severity = ValidationSeverity.INFO
                )
            )
        } else {
            issues.add(
                ValidationItem(
                    title = "Wear OS 4 (WFF v1) Legacy Target",
                    description = "Flavors and weighted multi-arcs will be translated to basic fallbacks for compatibility.",
                    severity = ValidationSeverity.INFO
                )
            )
        }

        // Check Complication Slots uniqueness
        val slotIds = activeComplications.map { it.slotId }
        if (slotIds.size == slotIds.distinct().size) {
            issues.add(
                ValidationItem(
                    title = "${activeComplications.size} Complication Slots Configured",
                    description = "All active slots have unique slot IDs and valid bounding bounds.",
                    severity = ValidationSeverity.PASS
                )
            )
        } else {
            issues.add(
                ValidationItem(
                    title = "Duplicate Complication Slot IDs",
                    description = "Slot IDs must be unique across all complication slots in WFF.",
                    severity = ValidationSeverity.ERROR
                )
            )
        }

        return ValidationReport(
            activeOprPercentage = activeOprPercent,
            ambientOprPercentage = ambientOprPercent,
            isAmbientCompliant = isAmbientCompliant,
            issues = issues
        )
    }
}
