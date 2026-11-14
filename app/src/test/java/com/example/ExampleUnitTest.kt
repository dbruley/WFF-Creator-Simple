package com.example

import com.example.data.DefaultPresets
import com.example.generator.ValidationSeverity
import com.example.generator.WffValidator
import com.example.generator.WffXmlGenerator
import com.example.model.WatchFaceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testWffXmlGenerationWearOs7() {
        val config = WatchFaceConfig(
            name = "Test Face",
            targetWearOsVersion = 7
        )
        val xml = WffXmlGenerator.generateWatchFaceXml(config)

        assertNotNull(xml)
        assertTrue(xml.contains("<WatchFace"))
        assertTrue(xml.contains("""clipShape="CIRCLE""""))
        assertTrue(xml.contains("""<Metadata key="TARGET_WEAR_OS_VERSION" value="7" />"""))
        assertTrue(xml.contains("""<Metadata key="WFF_VERSION" value="4" />"""))
        assertTrue(xml.contains("<UserConfigurations>"))
        assertTrue(xml.contains("<Flavors>"))
        assertTrue(xml.contains("<Scene"))
        assertTrue(xml.contains("<AnalogClock"))
        assertTrue(xml.contains("<DigitalClock"))
        assertTrue(xml.contains("<ComplicationSlot"))
        assertTrue(xml.contains("</WatchFace>"))
    }

    @Test
    fun testWffValidatorOprCheck() {
        val config = WatchFaceConfig(name = "OPR Test")
        val report = WffValidator.validate(config)

        assertNotNull(report)
        assertTrue(report.activeOprPercentage > 0f)
        assertTrue(report.ambientOprPercentage > 0f)
        assertTrue("Ambient OPR should satisfy Wear OS requirement (< 15%)", report.isAmbientCompliant)
        assertTrue(report.issues.any { it.severity == ValidationSeverity.PASS })
    }

    @Test
    fun testDefaultPresetsAvailable() {
        assertTrue(DefaultPresets.presets.size >= 5)
        val chrono = DefaultPresets.presets.firstOrNull { it.id == "preset_chrono_velocity" }
        assertNotNull(chrono)
        assertEquals(7, chrono?.targetWearOsVersion)
    }

    @Test
    fun testWearOs7ConditionRulesPresent() {
        val config = WatchFaceConfig(name = "Rules Face", targetWearOsVersion = 7)
        assertTrue(config.conditionRules.isNotEmpty())
        assertTrue(config.conditionRules.any { it.expression.contains("[BATTERY_PERCENT]") })
        assertTrue(config.conditionRules.any { it.expression.contains("[IS_AMBIENT]") })
    }
}
