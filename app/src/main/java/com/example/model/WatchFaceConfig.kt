package com.example.model

enum class WatchShape(val displayName: String, val sizeDp: Int, val isCircular: Boolean) {
    ROUND_450("Round (450x450)", 450, true),
    ROUND_454("Round (454x454)", 454, true),
    ROUND_416("Round (416x416)", 416, true),
    SQUARE_450("Square (450x450)", 450, false)
}

enum class ClockType(val displayName: String) {
    ANALOG("Analog"),
    DIGITAL("Digital"),
    HYBRID("Hybrid (Analog + Digital)")
}

enum class DialBackgroundStyle(val displayName: String) {
    OBSIDIAN_PURE("Obsidian Black (0% OPR)"),
    CARBON_TEXTURE("Carbon Fiber Grid"),
    RADIAL_GLOW("Deep Radial Glow"),
    PILOT_MATTE("Matte Military"),
    CYBER_GRID("Neon Cyberpunk"),
    SOLAR_SUNBURST("Solar Sunburst")
}

enum class BezelStyle(val displayName: String) {
    DIVER_60("Diver Bezel (0-60)"),
    TACHYMETER("Tachymeter"),
    CARDINAL_NUMERALS("Cardinal (12, 3, 6, 9)"),
    ROMAN_NUMERALS("Roman Numerals (XII, III, VI, IX)"),
    ARABIC_ALL("Full Hours (1-12)"),
    MINIMAL_TICKS("Minimal 60 Ticks"),
    DOT_INDICES("Dot Indices"),
    NONE("Clean / Borderless")
}

enum class HandStyle(val displayName: String) {
    SWORD("Sword (Luminescent)"),
    BATON("Baton (Classic)"),
    SYRINGE("Syringe (Vintage)"),
    SKELETON("Skeleton (Hollow)"),
    ARROW("Arrow / Diver"),
    SLENDER("Slender Minimalist")
}

enum class HandMotion(val displayName: String) {
    SWEEP("Continuous Sweep (Smooth)"),
    STEP_TICK("Stepped (1s Mechanical Tick)")
}

enum class DigitalFontFamily(val displayName: String, val badgeText: String) {
    DIGITAL_7("Digital 7 (7-Segment)", "88:88"),
    DOT_MATRIX("Small Dot Matrix (5×7)", "12:45"),
    TECH_MONO("Share Tech Mono", "10:08"),
    CYBER_DISPLAY("Orbitron Sci-Fi", "23:59"),
    BOLD_GEOMETRIC("Chakra Petch Mech", "09:42"),
    STENCIL_MILITARY("Wallpoet Stencil", "14:20"),
    RETRO_LED("VT323 Retro Terminal", "12:00"),
    PIXEL_ARCADE("Silkscreen Micro Pixel", "08:30"),
    MODERN_SANS("Modern Clean Sans", "10:10")
}

enum class ComplicationPosition(val displayName: String, val angleDeg: Float, val radiusFactor: Float) {
    TOP("Top", 270f, 0.52f),
    BOTTOM("Bottom", 90f, 0.52f),
    LEFT("Left", 180f, 0.52f),
    RIGHT("Right", 0f, 0.52f),
    SUBDIAL_TOP_RIGHT("Subdial Top-Right", 315f, 0.48f),
    SUBDIAL_BOTTOM_LEFT("Subdial Bottom-Left", 135f, 0.48f)
}

enum class ComplicationType(val displayName: String, val wffTagName: String) {
    RANGED_VALUE("Ranged Value Gauge", "RANGED_VALUE"),
    SHORT_TEXT("Short Text + Subtitle", "SHORT_TEXT"),
    ICON("Monochromatic Icon", "MONOCHROMATIC_IMAGE"),
    GOAL_PROGRESS("Goal Progress Meter", "GOAL_PROGRESS"),
    WEIGHTED_ARC("Weighted Multi-Arc (Wear OS 7)", "WEIGHTED_ARC")
}

enum class SimulatedDataSource(
    val displayName: String,
    val defaultValue: String,
    val defaultSubtext: String,
    val defaultProgress: Float,
    val unit: String
) {
    BATTERY("Battery Level", "78%", "Charging", 0.78f, "%"),
    STEPS("Step Count", "8,430", "Goal: 10k", 0.843f, "steps"),
    HEART_RATE("Heart Rate", "72", "Normal", 0.45f, "bpm"),
    WEATHER("Weather Temp", "22°", "Clear Sky", 0.65f, "°C"),
    CALORIES("Active Calories", "460", "Goal: 600", 0.76f, "kcal"),
    NEXT_EVENT("Calendar Event", "11:30", "Team Sync", 0.35f, ""),
    SUNRISE_SUNSET("Sunset / Sunrise", "18:45", "Dusk in 2h", 0.88f, ""),
    UV_INDEX("UV Index", "4.2", "Moderate", 0.42f, "UV")
}

data class WffConditionRule(
    val id: String,
    val title: String,
    val expression: String, // Wear OS 5/6/7 dynamic expression
    val targetProperty: String,
    val conditionAction: String,
    val isEnabled: Boolean = true
)

data class ComplicationSlotConfig(
    val slotId: Int,
    val name: String,
    val position: ComplicationPosition,
    val enabled: Boolean = true,
    val slotType: ComplicationType = ComplicationType.RANGED_VALUE,
    val dataSource: SimulatedDataSource = SimulatedDataSource.BATTERY,
    val customValue: String = "",
    val customProgress: Float = -1f,
    val accentColor: String = "#00E5FF",
    val customAngle: Float? = null,
    val customRadiusFactor: Float? = null
)

data class AnalogHandsConfig(
    val hourHandStyle: HandStyle = HandStyle.SWORD,
    val hourHandLength: Float = 0.52f, // 0.0 to 1.0 of watch radius
    val hourHandWidth: Float = 6f,
    val hourHandColor: String = "#FFFFFF",
    val hourHandAccentColor: String = "#00E5FF",

    val minuteHandStyle: HandStyle = HandStyle.SWORD,
    val minuteHandLength: Float = 0.78f,
    val minuteHandWidth: Float = 4.5f,
    val minuteHandColor: String = "#FFFFFF",
    val minuteHandAccentColor: String = "#00E5FF",

    val secondHandVisible: Boolean = true,
    val secondHandMotion: HandMotion = HandMotion.SWEEP,
    val secondHandLength: Float = 0.88f,
    val secondHandWidth: Float = 2f,
    val secondHandColor: String = "#F59E0B",

    val centerPinColor: String = "#F59E0B",
    val centerPinRadius: Float = 6f,
    val ambientHollowHands: Boolean = true // converts hands to hollow outlines in ambient mode
)

data class DigitalClockConfig(
    val is24Hour: Boolean = false,
    val leadingZero: Boolean = true,
    val showSeconds: Boolean = true,
    val fontFamily: DigitalFontFamily = DigitalFontFamily.TECH_MONO,
    val fontSize: Int = 48,
    val positionYOffset: Float = 0f, // vertical offset in DP from center
    val hourColor: String = "#FFFFFF",
    val minuteColor: String = "#00E5FF",
    val secondColor: String = "#F59E0B",
    val glowEffect: Boolean = true,
    val showDate: Boolean = true,
    val dateFormat: String = "EEE, MMM d",
    val dateColor: String = "#94A3B8"
)

data class FlavorProfile(
    val id: String,
    val displayName: String,
    val primaryColor: String,
    val secondaryColor: String,
    val backgroundColor: String
)

data class AmbientConfig(
    val lowBitMode: Boolean = true, // monochrome contrast for OLED power
    val burnInProtectionShift: Boolean = true,
    val hideSecondHand: Boolean = true,
    val hideComplications: Boolean = false,
    val dimFactor: Float = 0.4f
)

data class WatchFaceConfig(
    val id: String = "wff_custom_face",
    val name: String = "Chrono Velocity",
    val targetWearOsVersion: Int = 7, // 4, 5, 6, 7
    val shape: WatchShape = WatchShape.ROUND_450,
    val clockType: ClockType = ClockType.HYBRID,
    val themeColor: String = "#00E5FF",
    val secondaryColor: String = "#F59E0B",
    val tertiaryColor: String = "#10B981",
    val dialBackgroundStyle: DialBackgroundStyle = DialBackgroundStyle.OBSIDIAN_PURE,
    val bezelStyle: BezelStyle = BezelStyle.DIVER_60,
    val bezelColor: String = "#38BDF8",
    val innerRingVisible: Boolean = true,
    val innerRingColor: String = "#1E293B",

    val analogHands: AnalogHandsConfig = AnalogHandsConfig(),
    val digitalClock: DigitalClockConfig = DigitalClockConfig(),
    val complications: List<ComplicationSlotConfig> = listOf(
        ComplicationSlotConfig(
            slotId = 0,
            name = "Top Gauge",
            position = ComplicationPosition.TOP,
            enabled = true,
            slotType = ComplicationType.RANGED_VALUE,
            dataSource = SimulatedDataSource.BATTERY,
            accentColor = "#00E5FF"
        ),
        ComplicationSlotConfig(
            slotId = 1,
            name = "Bottom Tracker",
            position = ComplicationPosition.BOTTOM,
            enabled = true,
            slotType = ComplicationType.GOAL_PROGRESS,
            dataSource = SimulatedDataSource.STEPS,
            accentColor = "#10B981"
        ),
        ComplicationSlotConfig(
            slotId = 2,
            name = "Left Heart",
            position = ComplicationPosition.LEFT,
            enabled = true,
            slotType = ComplicationType.SHORT_TEXT,
            dataSource = SimulatedDataSource.HEART_RATE,
            accentColor = "#F43F5E"
        ),
        ComplicationSlotConfig(
            slotId = 3,
            name = "Right Weather",
            position = ComplicationPosition.RIGHT,
            enabled = true,
            slotType = ComplicationType.SHORT_TEXT,
            dataSource = SimulatedDataSource.WEATHER,
            accentColor = "#F59E0B"
        )
    ),
    val ambientConfig: AmbientConfig = AmbientConfig(),
    val availableFlavors: List<FlavorProfile> = listOf(
        FlavorProfile("cyan_glow", "Cyber Cyan", "#00E5FF", "#F59E0B", "#090D16"),
        FlavorProfile("solar_amber", "Solar Blaze", "#F59E0B", "#EF4444", "#0D0A05"),
        FlavorProfile("emerald_sport", "Viper Emerald", "#10B981", "#06B6D4", "#05110B"),
        FlavorProfile("stealth_mono", "Stealth Titanium", "#E2E8F0", "#64748B", "#000000")
    ),
    val conditionRules: List<WffConditionRule> = listOf(
        WffConditionRule("rule_battery_warning", "Low Battery Dial Tint", "[BATTERY_PERCENT] < 20", "Theme Color", "Switch accent to warning red #EF4444", true),
        WffConditionRule("rule_ambient_hide_sec", "Hide Second Hand in Ambient", "[IS_AMBIENT] == 1", "Second Hand", "Suppress rendering to save battery", true),
        WffConditionRule("rule_step_goal", "Step Goal Completed Glow", "[STEP_GOAL_PERCENT] >= 100", "Bottom Tracker", "Flash emerald highlight on goal ring", true),
        WffConditionRule("rule_high_bpm", "Elevated Heart Rate Pulse", "[HEART_RATE] > 120", "Left Heart", "Pulsate slot border in crimson", true)
    )
)
