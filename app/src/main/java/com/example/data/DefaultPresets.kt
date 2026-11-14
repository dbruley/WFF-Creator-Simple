package com.example.data

import com.example.model.AmbientConfig
import com.example.model.AnalogHandsConfig
import com.example.model.BezelStyle
import com.example.model.ClockType
import com.example.model.ComplicationPosition
import com.example.model.ComplicationSlotConfig
import com.example.model.ComplicationType
import com.example.model.DialBackgroundStyle
import com.example.model.DigitalClockConfig
import com.example.model.DigitalFontFamily
import com.example.model.FlavorProfile
import com.example.model.HandMotion
import com.example.model.HandStyle
import com.example.model.SimulatedDataSource
import com.example.model.WatchFaceConfig
import com.example.model.WatchShape

object DefaultPresets {

    val presets: List<WatchFaceConfig> = listOf(
        // Preset 1: Chronograph Velocity (Flagship Wear OS 7)
        WatchFaceConfig(
            id = "preset_chrono_velocity",
            name = "Chrono Velocity",
            targetWearOsVersion = 7,
            shape = WatchShape.ROUND_450,
            clockType = ClockType.HYBRID,
            themeColor = "#00E5FF",
            secondaryColor = "#F59E0B",
            tertiaryColor = "#10B981",
            dialBackgroundStyle = DialBackgroundStyle.CARBON_TEXTURE,
            bezelStyle = BezelStyle.DIVER_60,
            bezelColor = "#38BDF8",
            innerRingVisible = true,
            innerRingColor = "#1E293B",
            analogHands = AnalogHandsConfig(
                hourHandStyle = HandStyle.SWORD,
                hourHandLength = 0.52f,
                hourHandWidth = 7f,
                hourHandColor = "#FFFFFF",
                hourHandAccentColor = "#00E5FF",
                minuteHandStyle = HandStyle.SWORD,
                minuteHandLength = 0.78f,
                minuteHandWidth = 5f,
                minuteHandColor = "#FFFFFF",
                minuteHandAccentColor = "#00E5FF",
                secondHandVisible = true,
                secondHandMotion = HandMotion.SWEEP,
                secondHandLength = 0.88f,
                secondHandWidth = 2.2f,
                secondHandColor = "#EF4444",
                centerPinColor = "#F59E0B"
            ),
            digitalClock = DigitalClockConfig(
                is24Hour = false,
                showSeconds = false,
                fontFamily = DigitalFontFamily.TECH_MONO,
                fontSize = 32,
                positionYOffset = 68f,
                minuteColor = "#00E5FF",
                hourColor = "#FFFFFF",
                showDate = true,
                dateFormat = "EEE, MMM d"
            ),
            complications = listOf(
                ComplicationSlotConfig(
                    slotId = 0,
                    name = "Battery",
                    position = ComplicationPosition.TOP,
                    enabled = true,
                    slotType = ComplicationType.RANGED_VALUE,
                    dataSource = SimulatedDataSource.BATTERY,
                    accentColor = "#00E5FF"
                ),
                ComplicationSlotConfig(
                    slotId = 1,
                    name = "Steps",
                    position = ComplicationPosition.BOTTOM,
                    enabled = true,
                    slotType = ComplicationType.GOAL_PROGRESS,
                    dataSource = SimulatedDataSource.STEPS,
                    accentColor = "#10B981"
                ),
                ComplicationSlotConfig(
                    slotId = 2,
                    name = "Heart Rate",
                    position = ComplicationPosition.LEFT,
                    enabled = true,
                    slotType = ComplicationType.SHORT_TEXT,
                    dataSource = SimulatedDataSource.HEART_RATE,
                    accentColor = "#F43F5E"
                ),
                ComplicationSlotConfig(
                    slotId = 3,
                    name = "Weather",
                    position = ComplicationPosition.RIGHT,
                    enabled = true,
                    slotType = ComplicationType.SHORT_TEXT,
                    dataSource = SimulatedDataSource.WEATHER,
                    accentColor = "#F59E0B"
                )
            ),
            availableFlavors = listOf(
                FlavorProfile("speed_cyan", "Speed Cyan", "#00E5FF", "#F59E0B", "#0B0F19"),
                FlavorProfile("tach_orange", "Tachymeter Orange", "#F59E0B", "#EF4444", "#120B05"),
                FlavorProfile("stealth_carbon", "Stealth Carbon", "#94A3B8", "#38BDF8", "#080808")
            )
        ),

        // Preset 2: Cyberpunk HUD (Futuristic Digital & Arcs)
        WatchFaceConfig(
            id = "preset_cyberpunk_hud",
            name = "Cyberpunk HUD",
            targetWearOsVersion = 7,
            shape = WatchShape.ROUND_450,
            clockType = ClockType.DIGITAL,
            themeColor = "#F43F5E",
            secondaryColor = "#00E5FF",
            tertiaryColor = "#A855F7",
            dialBackgroundStyle = DialBackgroundStyle.CYBER_GRID,
            bezelStyle = BezelStyle.DOT_INDICES,
            bezelColor = "#F43F5E",
            innerRingVisible = true,
            innerRingColor = "#2E1065",
            digitalClock = DigitalClockConfig(
                is24Hour = true,
                showSeconds = true,
                fontFamily = DigitalFontFamily.CYBER_DISPLAY,
                fontSize = 54,
                positionYOffset = -15f,
                hourColor = "#FFFFFF",
                minuteColor = "#00E5FF",
                secondColor = "#F43F5E",
                glowEffect = true,
                showDate = true,
                dateFormat = "dd.MM.yyyy",
                dateColor = "#A855F7"
            ),
            complications = listOf(
                ComplicationSlotConfig(
                    slotId = 0,
                    name = "Power Core",
                    position = ComplicationPosition.TOP,
                    enabled = true,
                    slotType = ComplicationType.WEIGHTED_ARC,
                    dataSource = SimulatedDataSource.BATTERY,
                    accentColor = "#00E5FF"
                ),
                ComplicationSlotConfig(
                    slotId = 1,
                    name = "Bio Pulse",
                    position = ComplicationPosition.BOTTOM,
                    enabled = true,
                    slotType = ComplicationType.RANGED_VALUE,
                    dataSource = SimulatedDataSource.HEART_RATE,
                    accentColor = "#F43F5E"
                ),
                ComplicationSlotConfig(
                    slotId = 2,
                    name = "Distance",
                    position = ComplicationPosition.LEFT,
                    enabled = true,
                    slotType = ComplicationType.SHORT_TEXT,
                    dataSource = SimulatedDataSource.STEPS,
                    accentColor = "#10B981"
                ),
                ComplicationSlotConfig(
                    slotId = 3,
                    name = "Atmosphere",
                    position = ComplicationPosition.RIGHT,
                    enabled = true,
                    slotType = ComplicationType.SHORT_TEXT,
                    dataSource = SimulatedDataSource.WEATHER,
                    accentColor = "#F59E0B"
                )
            )
        ),

        // Preset 3: Aero Pilot Tactical (Analog Military)
        WatchFaceConfig(
            id = "preset_aero_pilot",
            name = "Aero Pilot Mk. VII",
            targetWearOsVersion = 7,
            shape = WatchShape.ROUND_450,
            clockType = ClockType.ANALOG,
            themeColor = "#FACC15",
            secondaryColor = "#38BDF8",
            tertiaryColor = "#4ADE80",
            dialBackgroundStyle = DialBackgroundStyle.PILOT_MATTE,
            bezelStyle = BezelStyle.CARDINAL_NUMERALS,
            bezelColor = "#FACC15",
            innerRingVisible = true,
            innerRingColor = "#334155",
            analogHands = AnalogHandsConfig(
                hourHandStyle = HandStyle.ARROW,
                hourHandLength = 0.50f,
                hourHandWidth = 8f,
                hourHandColor = "#F8FAFC",
                hourHandAccentColor = "#FACC15",
                minuteHandStyle = HandStyle.ARROW,
                minuteHandLength = 0.76f,
                minuteHandWidth = 6f,
                minuteHandColor = "#F8FAFC",
                minuteHandAccentColor = "#FACC15",
                secondHandVisible = true,
                secondHandMotion = HandMotion.STEP_TICK,
                secondHandLength = 0.85f,
                secondHandWidth = 2.5f,
                secondHandColor = "#FACC15"
            ),
            complications = listOf(
                ComplicationSlotConfig(
                    slotId = 0,
                    name = "Altimeter/UV",
                    position = ComplicationPosition.TOP,
                    enabled = true,
                    slotType = ComplicationType.SHORT_TEXT,
                    dataSource = SimulatedDataSource.UV_INDEX,
                    accentColor = "#FACC15"
                ),
                ComplicationSlotConfig(
                    slotId = 1,
                    name = "Fuel Gauge",
                    position = ComplicationPosition.BOTTOM,
                    enabled = true,
                    slotType = ComplicationType.RANGED_VALUE,
                    dataSource = SimulatedDataSource.BATTERY,
                    accentColor = "#38BDF8"
                )
            )
        ),

        // Preset 4: Bauhaus Minimalist (Ultra Low OPR <5%)
        WatchFaceConfig(
            id = "preset_bauhaus_luxe",
            name = "Bauhaus Minimal",
            targetWearOsVersion = 6,
            shape = WatchShape.ROUND_450,
            clockType = ClockType.ANALOG,
            themeColor = "#E2E8F0",
            secondaryColor = "#F59E0B",
            dialBackgroundStyle = DialBackgroundStyle.OBSIDIAN_PURE,
            bezelStyle = BezelStyle.MINIMAL_TICKS,
            bezelColor = "#475569",
            innerRingVisible = false,
            analogHands = AnalogHandsConfig(
                hourHandStyle = HandStyle.SLENDER,
                hourHandLength = 0.48f,
                hourHandWidth = 3.5f,
                hourHandColor = "#F1F5F9",
                minuteHandStyle = HandStyle.SLENDER,
                minuteHandLength = 0.80f,
                minuteHandWidth = 2.5f,
                minuteHandColor = "#F1F5F9",
                secondHandVisible = true,
                secondHandMotion = HandMotion.SWEEP,
                secondHandLength = 0.86f,
                secondHandWidth = 1.2f,
                secondHandColor = "#F59E0B"
            ),
            complications = listOf(
                ComplicationSlotConfig(
                    slotId = 1,
                    name = "Date Event",
                    position = ComplicationPosition.BOTTOM,
                    enabled = true,
                    slotType = ComplicationType.SHORT_TEXT,
                    dataSource = SimulatedDataSource.NEXT_EVENT,
                    accentColor = "#94A3B8"
                )
            ),
            ambientConfig = AmbientConfig(
                lowBitMode = true,
                dimFactor = 0.3f,
                hideSecondHand = true
            )
        ),

        // Preset 5: Fitness Ultra Ring (Wear OS 7 Multi-Segmented Goals)
        WatchFaceConfig(
            id = "preset_fitness_ultra",
            name = "Fitness Ultra Rings",
            targetWearOsVersion = 7,
            shape = WatchShape.ROUND_450,
            clockType = ClockType.HYBRID,
            themeColor = "#10B981",
            secondaryColor = "#00E5FF",
            tertiaryColor = "#F43F5E",
            dialBackgroundStyle = DialBackgroundStyle.OBSIDIAN_PURE,
            bezelStyle = BezelStyle.DOT_INDICES,
            bezelColor = "#10B981",
            innerRingVisible = true,
            innerRingColor = "#1E293B",
            digitalClock = DigitalClockConfig(
                is24Hour = false,
                showSeconds = false,
                fontFamily = DigitalFontFamily.BOLD_GEOMETRIC,
                fontSize = 44,
                positionYOffset = -20f,
                hourColor = "#FFFFFF",
                minuteColor = "#10B981",
                showDate = true,
                dateFormat = "EEE d MMM"
            ),
            analogHands = AnalogHandsConfig(
                hourHandStyle = HandStyle.BATON,
                hourHandLength = 0.45f,
                hourHandWidth = 5f,
                hourHandColor = "#FFFFFF",
                minuteHandStyle = HandStyle.BATON,
                minuteHandLength = 0.70f,
                minuteHandWidth = 3.5f,
                minuteHandColor = "#00E5FF",
                secondHandVisible = true,
                secondHandColor = "#F43F5E"
            ),
            complications = listOf(
                ComplicationSlotConfig(
                    slotId = 0,
                    name = "Activity Rings",
                    position = ComplicationPosition.TOP,
                    enabled = true,
                    slotType = ComplicationType.WEIGHTED_ARC,
                    dataSource = SimulatedDataSource.CALORIES,
                    accentColor = "#F43F5E"
                ),
                ComplicationSlotConfig(
                    slotId = 1,
                    name = "Step Goal",
                    position = ComplicationPosition.BOTTOM,
                    enabled = true,
                    slotType = ComplicationType.GOAL_PROGRESS,
                    dataSource = SimulatedDataSource.STEPS,
                    accentColor = "#10B981"
                ),
                ComplicationSlotConfig(
                    slotId = 2,
                    name = "BPM",
                    position = ComplicationPosition.LEFT,
                    enabled = true,
                    slotType = ComplicationType.SHORT_TEXT,
                    dataSource = SimulatedDataSource.HEART_RATE,
                    accentColor = "#F43F5E"
                ),
                ComplicationSlotConfig(
                    slotId = 3,
                    name = "Battery",
                    position = ComplicationPosition.RIGHT,
                    enabled = true,
                    slotType = ComplicationType.RANGED_VALUE,
                    dataSource = SimulatedDataSource.BATTERY,
                    accentColor = "#00E5FF"
                )
            )
        )
    )
}
