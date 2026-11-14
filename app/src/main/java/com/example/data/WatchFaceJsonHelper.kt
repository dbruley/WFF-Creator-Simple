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
import com.example.model.WffConditionRule
import org.json.JSONArray
import org.json.JSONObject

object WatchFaceJsonHelper {

    fun toJson(config: WatchFaceConfig): String {
        val root = JSONObject()
        root.put("id", config.id)
        root.put("name", config.name)
        root.put("targetWearOsVersion", config.targetWearOsVersion)
        root.put("shape", config.shape.name)
        root.put("clockType", config.clockType.name)
        root.put("themeColor", config.themeColor)
        root.put("secondaryColor", config.secondaryColor)
        root.put("tertiaryColor", config.tertiaryColor)
        root.put("dialBackgroundStyle", config.dialBackgroundStyle.name)
        root.put("bezelStyle", config.bezelStyle.name)
        root.put("bezelColor", config.bezelColor)
        root.put("innerRingVisible", config.innerRingVisible)
        root.put("innerRingColor", config.innerRingColor)

        // Analog Hands
        val hands = JSONObject()
        hands.put("hourHandStyle", config.analogHands.hourHandStyle.name)
        hands.put("hourHandLength", config.analogHands.hourHandLength.toDouble())
        hands.put("hourHandWidth", config.analogHands.hourHandWidth.toDouble())
        hands.put("hourHandColor", config.analogHands.hourHandColor)
        hands.put("hourHandAccentColor", config.analogHands.hourHandAccentColor)
        hands.put("minuteHandStyle", config.analogHands.minuteHandStyle.name)
        hands.put("minuteHandLength", config.analogHands.minuteHandLength.toDouble())
        hands.put("minuteHandWidth", config.analogHands.minuteHandWidth.toDouble())
        hands.put("minuteHandColor", config.analogHands.minuteHandColor)
        hands.put("minuteHandAccentColor", config.analogHands.minuteHandAccentColor)
        hands.put("secondHandVisible", config.analogHands.secondHandVisible)
        hands.put("secondHandMotion", config.analogHands.secondHandMotion.name)
        hands.put("secondHandLength", config.analogHands.secondHandLength.toDouble())
        hands.put("secondHandWidth", config.analogHands.secondHandWidth.toDouble())
        hands.put("secondHandColor", config.analogHands.secondHandColor)
        hands.put("centerPinColor", config.analogHands.centerPinColor)
        hands.put("centerPinRadius", config.analogHands.centerPinRadius.toDouble())
        hands.put("ambientHollowHands", config.analogHands.ambientHollowHands)
        root.put("analogHands", hands)

        // Digital Clock
        val dig = JSONObject()
        dig.put("is24Hour", config.digitalClock.is24Hour)
        dig.put("leadingZero", config.digitalClock.leadingZero)
        dig.put("showSeconds", config.digitalClock.showSeconds)
        dig.put("fontFamily", config.digitalClock.fontFamily.name)
        dig.put("fontSize", config.digitalClock.fontSize)
        dig.put("positionYOffset", config.digitalClock.positionYOffset.toDouble())
        dig.put("hourColor", config.digitalClock.hourColor)
        dig.put("minuteColor", config.digitalClock.minuteColor)
        dig.put("secondColor", config.digitalClock.secondColor)
        dig.put("glowEffect", config.digitalClock.glowEffect)
        dig.put("showDate", config.digitalClock.showDate)
        dig.put("dateFormat", config.digitalClock.dateFormat)
        dig.put("dateColor", config.digitalClock.dateColor)
        root.put("digitalClock", dig)

        // Complications
        val comps = JSONArray()
        config.complications.forEach { c ->
            val co = JSONObject()
            co.put("slotId", c.slotId)
            co.put("name", c.name)
            co.put("position", c.position.name)
            co.put("enabled", c.enabled)
            co.put("slotType", c.slotType.name)
            co.put("dataSource", c.dataSource.name)
            co.put("customValue", c.customValue)
            co.put("customProgress", c.customProgress.toDouble())
            co.put("accentColor", c.accentColor)
            if (c.customAngle != null) co.put("customAngle", c.customAngle.toDouble())
            if (c.customRadiusFactor != null) co.put("customRadiusFactor", c.customRadiusFactor.toDouble())
            comps.put(co)
        }
        root.put("complications", comps)

        // Ambient Config
        val amb = JSONObject()
        amb.put("lowBitMode", config.ambientConfig.lowBitMode)
        amb.put("burnInProtectionShift", config.ambientConfig.burnInProtectionShift)
        amb.put("hideSecondHand", config.ambientConfig.hideSecondHand)
        amb.put("hideComplications", config.ambientConfig.hideComplications)
        amb.put("dimFactor", config.ambientConfig.dimFactor.toDouble())
        root.put("ambientConfig", amb)

        // Flavors
        val flavors = JSONArray()
        config.availableFlavors.forEach { f ->
            val fo = JSONObject()
            fo.put("id", f.id)
            fo.put("displayName", f.displayName)
            fo.put("primaryColor", f.primaryColor)
            fo.put("secondaryColor", f.secondaryColor)
            fo.put("backgroundColor", f.backgroundColor)
            flavors.put(fo)
        }
        root.put("availableFlavors", flavors)

        // Condition Rules (Wear OS 7)
        val rules = JSONArray()
        config.conditionRules.forEach { r ->
            val ro = JSONObject()
            ro.put("id", r.id)
            ro.put("title", r.title)
            ro.put("expression", r.expression)
            ro.put("targetProperty", r.targetProperty)
            ro.put("conditionAction", r.conditionAction)
            ro.put("isEnabled", r.isEnabled)
            rules.put(ro)
        }
        root.put("conditionRules", rules)

        return root.toString(2)
    }

    fun fromJson(jsonStr: String): WatchFaceConfig {
        return try {
            val root = JSONObject(jsonStr)
            val shapeStr = root.optString("shape", WatchShape.ROUND_450.name)
            val shape = try { WatchShape.valueOf(shapeStr) } catch (_: Exception) { WatchShape.ROUND_450 }
            val clockTypeStr = root.optString("clockType", ClockType.HYBRID.name)
            val clockType = try { ClockType.valueOf(clockTypeStr) } catch (_: Exception) { ClockType.HYBRID }
            val bgStyleStr = root.optString("dialBackgroundStyle", DialBackgroundStyle.OBSIDIAN_PURE.name)
            val bgStyle = try { DialBackgroundStyle.valueOf(bgStyleStr) } catch (_: Exception) { DialBackgroundStyle.OBSIDIAN_PURE }
            val bezelStyleStr = root.optString("bezelStyle", BezelStyle.DIVER_60.name)
            val bezelStyle = try { BezelStyle.valueOf(bezelStyleStr) } catch (_: Exception) { BezelStyle.DIVER_60 }

            val handsObj = root.optJSONObject("analogHands")
            val analogHands = if (handsObj != null) {
                AnalogHandsConfig(
                    hourHandStyle = parseHandStyle(handsObj.optString("hourHandStyle")),
                    hourHandLength = handsObj.optDouble("hourHandLength", 0.52).toFloat(),
                    hourHandWidth = handsObj.optDouble("hourHandWidth", 6.0).toFloat(),
                    hourHandColor = handsObj.optString("hourHandColor", "#FFFFFF"),
                    hourHandAccentColor = handsObj.optString("hourHandAccentColor", "#00E5FF"),
                    minuteHandStyle = parseHandStyle(handsObj.optString("minuteHandStyle")),
                    minuteHandLength = handsObj.optDouble("minuteHandLength", 0.78).toFloat(),
                    minuteHandWidth = handsObj.optDouble("minuteHandWidth", 4.5).toFloat(),
                    minuteHandColor = handsObj.optString("minuteHandColor", "#FFFFFF"),
                    minuteHandAccentColor = handsObj.optString("minuteHandAccentColor", "#00E5FF"),
                    secondHandVisible = handsObj.optBoolean("secondHandVisible", true),
                    secondHandMotion = parseHandMotion(handsObj.optString("secondHandMotion")),
                    secondHandLength = handsObj.optDouble("secondHandLength", 0.88).toFloat(),
                    secondHandWidth = handsObj.optDouble("secondHandWidth", 2.0).toFloat(),
                    secondHandColor = handsObj.optString("secondHandColor", "#F59E0B"),
                    centerPinColor = handsObj.optString("centerPinColor", "#F59E0B"),
                    centerPinRadius = handsObj.optDouble("centerPinRadius", 6.0).toFloat(),
                    ambientHollowHands = handsObj.optBoolean("ambientHollowHands", true)
                )
            } else AnalogHandsConfig()

            val digObj = root.optJSONObject("digitalClock")
            val digitalClock = if (digObj != null) {
                DigitalClockConfig(
                    is24Hour = digObj.optBoolean("is24Hour", false),
                    leadingZero = digObj.optBoolean("leadingZero", true),
                    showSeconds = digObj.optBoolean("showSeconds", true),
                    fontFamily = parseFontFamily(digObj.optString("fontFamily")),
                    fontSize = digObj.optInt("fontSize", 48),
                    positionYOffset = digObj.optDouble("positionYOffset", 0.0).toFloat(),
                    hourColor = digObj.optString("hourColor", "#FFFFFF"),
                    minuteColor = digObj.optString("minuteColor", "#00E5FF"),
                    secondColor = digObj.optString("secondColor", "#F59E0B"),
                    glowEffect = digObj.optBoolean("glowEffect", true),
                    showDate = digObj.optBoolean("showDate", true),
                    dateFormat = digObj.optString("dateFormat", "EEE, MMM d"),
                    dateColor = digObj.optString("dateColor", "#94A3B8")
                )
            } else DigitalClockConfig()

            val compsList = mutableListOf<ComplicationSlotConfig>()
            val compsArray = root.optJSONArray("complications")
            if (compsArray != null) {
                for (i in 0 until compsArray.length()) {
                    val co = compsArray.getJSONObject(i)
                    val customAngleVal = if (co.has("customAngle")) co.optDouble("customAngle").toFloat() else null
                    val customRadiusVal = if (co.has("customRadiusFactor")) co.optDouble("customRadiusFactor").toFloat() else null
                    compsList.add(
                        ComplicationSlotConfig(
                            slotId = co.optInt("slotId", i),
                            name = co.optString("name", "Slot $i"),
                            position = parsePosition(co.optString("position")),
                            enabled = co.optBoolean("enabled", true),
                            slotType = parseSlotType(co.optString("slotType")),
                            dataSource = parseDataSource(co.optString("dataSource")),
                            customValue = co.optString("customValue", ""),
                            customProgress = co.optDouble("customProgress", -1.0).toFloat(),
                            accentColor = co.optString("accentColor", "#00E5FF"),
                            customAngle = customAngleVal,
                            customRadiusFactor = customRadiusVal
                        )
                    )
                }
            }

            val ambObj = root.optJSONObject("ambientConfig")
            val ambientConfig = if (ambObj != null) {
                AmbientConfig(
                    lowBitMode = ambObj.optBoolean("lowBitMode", true),
                    burnInProtectionShift = ambObj.optBoolean("burnInProtectionShift", true),
                    hideSecondHand = ambObj.optBoolean("hideSecondHand", true),
                    hideComplications = ambObj.optBoolean("hideComplications", false),
                    dimFactor = ambObj.optDouble("dimFactor", 0.4).toFloat()
                )
            } else AmbientConfig()

            val flavorsList = mutableListOf<FlavorProfile>()
            val flavorsArray = root.optJSONArray("availableFlavors")
            if (flavorsArray != null) {
                for (i in 0 until flavorsArray.length()) {
                    val fo = flavorsArray.getJSONObject(i)
                    flavorsList.add(
                        FlavorProfile(
                            id = fo.optString("id", "flavor_$i"),
                            displayName = fo.optString("displayName", "Flavor $i"),
                            primaryColor = fo.optString("primaryColor", "#00E5FF"),
                            secondaryColor = fo.optString("secondaryColor", "#F59E0B"),
                            backgroundColor = fo.optString("backgroundColor", "#090D16")
                        )
                    )
                }
            }

            val rulesList = mutableListOf<WffConditionRule>()
            val rulesArray = root.optJSONArray("conditionRules")
            if (rulesArray != null) {
                for (i in 0 until rulesArray.length()) {
                    val ro = rulesArray.getJSONObject(i)
                    rulesList.add(
                        WffConditionRule(
                            id = ro.optString("id", "rule_$i"),
                            title = ro.optString("title", "Rule $i"),
                            expression = ro.optString("expression", ""),
                            targetProperty = ro.optString("targetProperty", ""),
                            conditionAction = ro.optString("conditionAction", ""),
                            isEnabled = ro.optBoolean("isEnabled", true)
                        )
                    )
                }
            }

            WatchFaceConfig(
                id = root.optString("id", "custom_face"),
                name = root.optString("name", "Custom Face"),
                targetWearOsVersion = root.optInt("targetWearOsVersion", 7),
                shape = shape,
                clockType = clockType,
                themeColor = root.optString("themeColor", "#00E5FF"),
                secondaryColor = root.optString("secondaryColor", "#F59E0B"),
                tertiaryColor = root.optString("tertiaryColor", "#10B981"),
                dialBackgroundStyle = bgStyle,
                bezelStyle = bezelStyle,
                bezelColor = root.optString("bezelColor", "#38BDF8"),
                innerRingVisible = root.optBoolean("innerRingVisible", true),
                innerRingColor = root.optString("innerRingColor", "#1E293B"),
                analogHands = analogHands,
                digitalClock = digitalClock,
                complications = if (compsList.isNotEmpty()) compsList else WatchFaceConfig().complications,
                ambientConfig = ambientConfig,
                availableFlavors = if (flavorsList.isNotEmpty()) flavorsList else WatchFaceConfig().availableFlavors,
                conditionRules = if (rulesList.isNotEmpty()) rulesList else WatchFaceConfig().conditionRules
            )
        } catch (_: Exception) {
            WatchFaceConfig()
        }
    }

    private fun parseHandStyle(str: String): HandStyle = try { HandStyle.valueOf(str) } catch (_: Exception) { HandStyle.SWORD }
    private fun parseHandMotion(str: String): HandMotion = try { HandMotion.valueOf(str) } catch (_: Exception) { HandMotion.SWEEP }
    private fun parseFontFamily(str: String): DigitalFontFamily = try { DigitalFontFamily.valueOf(str) } catch (_: Exception) { DigitalFontFamily.TECH_MONO }
    private fun parsePosition(str: String): ComplicationPosition = try { ComplicationPosition.valueOf(str) } catch (_: Exception) { ComplicationPosition.TOP }
    private fun parseSlotType(str: String): ComplicationType = try { ComplicationType.valueOf(str) } catch (_: Exception) { ComplicationType.RANGED_VALUE }
    private fun parseDataSource(str: String): SimulatedDataSource = try { SimulatedDataSource.valueOf(str) } catch (_: Exception) { SimulatedDataSource.BATTERY }
}
