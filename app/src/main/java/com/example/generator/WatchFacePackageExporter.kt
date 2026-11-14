package com.example.generator

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.DefaultPresets
import com.example.data.WatchFaceJsonHelper
import com.example.model.DigitalFontFamily
import com.example.model.WatchFaceConfig
import com.example.previewer.PreviewSimulationState
import com.example.previewer.renderWatchFaceFrame
import com.example.watchface.WatchFaceWallpaperService
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object WatchFacePackageExporter {

    private const val LIVE_PREFS_NAME = "watch_face_live_prefs"
    private const val KEY_ACTIVE_FACE_JSON = "active_watch_face_json"

    fun generateSlug(config: WatchFaceConfig): String {
        val raw = config.name
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        val valid = if (raw.isEmpty()) "custom_face" else if (raw[0].isDigit()) "wf_$raw" else raw
        return valid.take(28).trimEnd('_')
    }

    fun generatePackageName(config: WatchFaceConfig): String {
        return "com.aistudio.watchface.${generateSlug(config)}"
    }

    fun getWffVersionString(config: WatchFaceConfig): String {
        return when (config.targetWearOsVersion) {
            4 -> "1"
            5 -> "2"
            6 -> "3"
            else -> "4"
        }
    }

    /**
     * Renders the user's WatchFaceConfig into a high-resolution Android Bitmap using the
     * exact same Compose DrawScope pipeline as the interactive studio previewer.
     */
    fun renderWatchFaceBitmap(
        context: Context,
        config: WatchFaceConfig,
        sizePx: Int = 450,
        isAmbient: Boolean = false
    ): Bitmap {
        val imageBitmap = ImageBitmap(sizePx, sizePx)
        val composeCanvas = Canvas(imageBitmap)
        val drawScope = CanvasDrawScope()
        val state = PreviewSimulationState(
            isAmbient = isAmbient,
            showGuidelines = false,
            simulatedHour = 10,
            simulatedMinute = 8,
            simulatedSecond = 32f,
            isLiveTime = false
        )

        drawScope.draw(
            density = Density(sizePx / 450f, 1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = composeCanvas,
            size = Size(sizePx.toFloat(), sizePx.toFloat())
        ) {
            renderWatchFaceFrame(
                context = context,
                config = config,
                state = state,
                hour = 10,
                minute = 8,
                second = 32f
            )
        }
        return imageBitmap.asAndroidBitmap()
    }

    fun renderWatchFacePngBytes(
        context: Context,
        config: WatchFaceConfig,
        sizePx: Int = 450
    ): ByteArray {
        val bitmap = renderWatchFaceBitmap(context, config, sizePx, isAmbient = false)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    /**
     * Generates the official Wear OS Watch Face Format (WFF v1-v4) AndroidManifest.xml
     * compatible with Pixel Watch 1, 2, 3, and Wear OS 4-7.
     */
    fun generateOfficialManifestXml(config: WatchFaceConfig): String {
        val pkgName = generatePackageName(config)
        val wffVer = getWffVersionString(config)
        val safeLabel = escapeXml(config.name)
        return """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$pkgName">

    <!-- Required for Wear OS / Pixel Watch standalone Watch Face Format packages -->
    <uses-feature android:name="android.hardware.type.watch" />

    <!-- Watch Face Format bundles are resource-only (android:hasCode="false") -->
    <application
        android:allowBackup="true"
        android:hasCode="false"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/watch_face_name">

        <!-- Standalone Wear OS Watch Face declaration -->
        <meta-data
            android:name="com.google.android.wearable.standalone"
            android:value="true" />

        <!-- Official Watch Face Format version property (Wear OS ${config.targetWearOsVersion} -> WFF v$wffVer) -->
        <property
            android:name="com.google.wear.watchface.format.version"
            android:value="$wffVer" />

        <property
            android:name="com.google.wear.watchface.format.publisher"
            android:value="WatchFaceStudio-4.0" />

        <!-- Links res/xml/watch_face_info.xml for Pixel Watch Companion & Picker -->
        <meta-data
            android:name="com.google.android.wearable.watchface.info"
            android:resource="@xml/watch_face_info" />

        <!-- Watch Face Format WallpaperService declaration for Pixel Watch picker -->
        <service
            android:name="com.google.android.wearable.watchface.wff.WffWatchFaceService"
            android:exported="true"
            android:label="$safeLabel"
            android:permission="android.permission.BIND_WALLPAPER">
            <intent-filter>
                <action android:name="android.service.wallpaper.WallpaperService" />
                <category android:name="com.google.android.wearable.watchface.category.WATCH_FACE" />
            </intent-filter>
            <meta-data
                android:name="com.google.android.wearable.watchface.preview"
                android:resource="@drawable/preview" />
            <meta-data
                android:name="com.google.android.wearable.watchface.preview_circular"
                android:resource="@drawable/preview" />
            <meta-data
                android:name="android.service.wallpaper"
                android:resource="@xml/watch_face_info" />
        </service>
    </application>
</manifest>"""
    }

    /**
     * Generates the official res/xml/watch_face_info.xml metadata file for Wear OS WFF.
     */
    fun generateWatchFaceInfoXml(config: WatchFaceConfig): String {
        val flavorsSupported = if (config.targetWearOsVersion >= 5) "true" else "false"
        return """<?xml version="1.0" encoding="utf-8"?>
<WatchFaceInfo>
    <Preview value="@drawable/preview" />
    <Category value="CATEGORY_EMPTY" />
    <AvailableInRetail value="true" />
    <MultipleInstancesAllowed value="true" />
    <Editable value="true" />
    <FlavorsSupported value="$flavorsSupported" />
</WatchFaceInfo>"""
    }

    fun generateStringsXml(config: WatchFaceConfig): String {
        val safeName = escapeXml(config.name)
        return """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">$safeName</string>
    <string name="watch_face_name">$safeName</string>
</resources>"""
    }

    fun generateAppBuildGradleKts(config: WatchFaceConfig): String {
        val pkgName = generatePackageName(config)
        val minSdk = if (config.targetWearOsVersion >= 5) 34 else 33
        return """plugins {
    id("com.android.application")
}

android {
    namespace = "$pkgName"
    compileSdk = 36

    defaultConfig {
        applicationId = "$pkgName"
        minSdk = $minSdk // Wear OS ${config.targetWearOsVersion} (WFF v${getWffVersionString(config)})
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    // Declarative Watch Face Format (WFF) requires zero runtime DEX code.
    // The Pixel Watch system renderer natively parses res/raw/watchface.xml.
}
"""
    }

    private fun generateRootBuildGradleKts(): String {
        return """plugins {
    id("com.android.application") version "8.7.2" apply false
}
"""
    }

    private fun generateSettingsGradleKts(config: WatchFaceConfig): String {
        val slug = generateSlug(config)
        return """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "$slug"
include(":app")
"""
    }

    private fun generateGradleProperties(): String {
        return """org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
"""
    }

    fun generateMacLinuxInstallScript(config: WatchFaceConfig, watchIp: String = "", debugPort: String = "5555"): String {
        val pkgName = generatePackageName(config)
        val defaultTarget = if (watchIp.isNotBlank()) "$watchIp:$debugPort" else "\${1:-}"
        return """#!/usr/bin/env bash
# ==============================================================================
# One-Click Pixel Watch WFF App Builder & Installer (macOS / Linux)
# Watch Face: ${config.name}
# Package ID: $pkgName
# ==============================================================================
set -e

echo "========================================================"
echo " Building Watch Face App: ${config.name}"
echo " Package ID: $pkgName"
echo "========================================================"

WATCH_TARGET="$defaultTarget"
if [ -n "${'$'}WATCH_TARGET" ]; then
    echo "-> Connecting to Pixel Watch at ${'$'}WATCH_TARGET..."
    adb connect "${'$'}WATCH_TARGET" || true
fi

if [ -x "./gradlew" ]; then
    ./gradlew :app:assembleDebug
elif command -v gradle >/dev/null 2>&1; then
    gradle :app:assembleDebug
else
    echo "ERROR: Neither ./gradlew nor gradle found in PATH."
    echo "Open this folder in Android Studio and click Run (Green Play button)!"
    exit 1
fi

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "${'$'}APK_PATH" ]; then
    echo "ERROR: APK not found at ${'$'}APK_PATH"
    exit 1
fi

echo "-> Installing ${config.name} onto connected Pixel Watch..."
adb install -r "${'$'}APK_PATH"

echo "-> Activating ${config.name} on Pixel Watch..."
adb shell am broadcast -a com.google.android.wearable.app.action.SET_WATCH_FACE -e package "$pkgName" || true

echo ""
echo "SUCCESS! '${config.name}' is now installed on your Pixel Watch!"
echo "Long-press your Pixel Watch screen or open the Google Pixel Watch phone app to select it."
"""
    }

    fun generateWindowsInstallScript(config: WatchFaceConfig, watchIp: String = "", debugPort: String = "5555"): String {
        val pkgName = generatePackageName(config)
        val connectLine = if (watchIp.isNotBlank()) "adb connect $watchIp:$debugPort" else "if not \"%~1\"==\"\" adb connect %~1"
        return """@echo off
REM ==============================================================================
REM One-Click Pixel Watch WFF App Builder & Installer (Windows)
REM Watch Face: ${config.name}
REM Package ID: $pkgName
REM ==============================================================================
echo ========================================================
echo  Building Watch Face App: ${config.name}
echo  Package ID: $pkgName
echo ========================================================

$connectLine

if exist gradlew.bat (
    call gradlew.bat :app:assembleDebug
) else (
    call gradle :app:assembleDebug
)

if not exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ERROR: Build failed or Gradle not found. Open this folder in Android Studio and press Run!
    pause
    exit /b 1
)

echo Installing ${config.name} onto Pixel Watch...
adb install -r "app\build\outputs\apk\debug\app-debug.apk"

echo Done! Long-press your Pixel Watch face or open the Pixel Watch app to select "${config.name}".
pause
"""
    }

    fun generatePixelWatchDeployGuide(
        config: WatchFaceConfig,
        watchIp: String = "192.168.1.100",
        pairingPort: String = "38123",
        debugPort: String = "41567"
    ): String {
        val pkgName = generatePackageName(config)
        val wffVer = getWffVersionString(config)
        val ip = watchIp.ifBlank { "<watch_ip>" }
        val pPort = pairingPort.ifBlank { "<pairing_port>" }
        val dPort = debugPort.ifBlank { "<debug_port>" }

        return """============================================================
PIXEL WATCH & WEAR OS INSTALLATION GUIDE
Watch Face Name : ${config.name}
App Package ID  : $pkgName
Format Version  : Wear OS ${config.targetWearOsVersion} (Watch Face Format v$wffVer)
============================================================

METHOD 1: EXPORT & ONE-CLICK INSTALL TO PIXEL WATCH (RECOMMENDED)
-----------------------------------------------------------------
1. Tap "Save App Project (.ZIP)" or "Share App Package (.ZIP)" above.
2. Extract the downloaded ZIP on your computer.
3. Enable Wireless Debugging on your Pixel Watch:
   • On Pixel Watch, open Settings -> System -> About -> Versions.
   • Tap "Build number" 7 times until "You are now a developer!" appears.
   • Go back to Settings -> Developer options.
   • Turn ON "ADB debugging" and select "Wireless debugging".
   • Tap "Pair new device" to see your Watch IP, Pairing Code, and Port.
4. Pair & Install:
   • Terminal / Command Prompt:
     $ adb pair $ip:$pPort
     $ adb connect $ip:$dPort
   • Run the included one-click installer script inside the ZIP:
     - Mac/Linux : ./install_to_pixel_watch.sh $ip:$dPort
     - Windows   : install_to_pixel_watch.bat $ip:$dPort
   • OR open the unzipped folder in Android Studio and click Run (▶)!

METHOD 2: SELECTING THE WATCH FACE ON YOUR PIXEL WATCH
------------------------------------------------------
Once installed, each exported watch face has its own unique Package ID
($pkgName) so you can install as many watch faces as you want:
• On your Pixel Watch: Long-press the current watch face, swipe to the right
  to tap "+ Add new", and select "${config.name}".
• Or on your phone: Open the "Google Pixel Watch" companion app -> Watch faces
  -> "+ Add new" -> select "${config.name}".

METHOD 3: ACTIVATE LIVE WATCH FACE DIRECTLY ON THIS DEVICE
----------------------------------------------------------
• Tap "Set Active Watch Face on Device" above to immediately apply
  "${config.name}" via the built-in WatchFaceWallpaperService.
"""
    }

    /**
     * Builds a complete, standalone Android Studio / Gradle Wear OS Watch Face Format
     * project ZIP archive including rendered preview PNGs, launcher icons, fonts, and
     * one-click Pixel Watch installer scripts.
     */
    fun buildAndroidStudioProjectZip(
        context: Context,
        config: WatchFaceConfig,
        watchIp: String = "",
        debugPort: String = "5555"
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val previewPng = renderWatchFacePngBytes(context, config, sizePx = 450)
        val iconPng = renderWatchFacePngBytes(context, config, sizePx = 192)
        val wffXml = WffXmlGenerator.generateWatchFaceXml(config)
        val manifestXml = generateOfficialManifestXml(config)
        val watchFaceInfoXml = generateWatchFaceInfoXml(config)
        val stringsXml = generateStringsXml(config)

        ZipOutputStream(baos).use { zos ->
            fun addTextEntry(path: String, content: String) {
                val entry = ZipEntry(path)
                zos.putNextEntry(entry)
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            fun addBinaryEntry(path: String, bytes: ByteArray) {
                val entry = ZipEntry(path)
                zos.putNextEntry(entry)
                zos.write(bytes)
                zos.closeEntry()
            }

            // Root project files
            addTextEntry("settings.gradle.kts", generateSettingsGradleKts(config))
            addTextEntry("build.gradle.kts", generateRootBuildGradleKts())
            addTextEntry("gradle.properties", generateGradleProperties())
            addTextEntry("README_PIXEL_WATCH.md", generatePixelWatchDeployGuide(config, watchIp.ifBlank { "192.168.1.100" }, "38123", debugPort.ifBlank { "5555" }))
            addTextEntry("install_to_pixel_watch.sh", generateMacLinuxInstallScript(config, watchIp, debugPort))
            addTextEntry("install_to_pixel_watch.bat", generateWindowsInstallScript(config, watchIp, debugPort))

            // App module files
            addTextEntry("app/build.gradle.kts", generateAppBuildGradleKts(config))
            addTextEntry("app/src/main/AndroidManifest.xml", manifestXml)
            addTextEntry("app/src/main/res/raw/watchface.xml", wffXml)
            addTextEntry("app/src/main/res/xml/watch_face_info.xml", watchFaceInfoXml)
            addTextEntry("app/src/main/res/values/strings.xml", stringsXml)

            // Rendered Preview & App Launcher Icons
            addBinaryEntry("app/src/main/res/drawable/preview.png", previewPng)
            addBinaryEntry("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png", iconPng)
            addBinaryEntry("app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png", iconPng)

            // Bundle custom TTF font if selected
            getFontResourceAndFileName(config.digitalClock.fontFamily)?.let { (resId, fontFileName) ->
                try {
                    val fontBytes = context.resources.openRawResource(resId).use { it.readBytes() }
                    addBinaryEntry("app/src/main/res/font/$fontFileName", fontBytes)
                } catch (_: Exception) {
                    // Ignore if font copy fails
                }
            }
        }
        return baos.toByteArray()
    }

    /**
     * Builds a Raw Watch Face Format Resource Bundle (.wff.zip) matching Google's
     * Watch Face Designer "Raw resources" export specification (for AAPT2 / WFF tools).
     */
    fun buildRawWffBundleZip(context: Context, config: WatchFaceConfig): ByteArray {
        val baos = ByteArrayOutputStream()
        val previewPng = renderWatchFacePngBytes(context, config, sizePx = 450)
        val iconPng = renderWatchFacePngBytes(context, config, sizePx = 192)
        val wffXml = WffXmlGenerator.generateWatchFaceXml(config)
        val manifestXml = generateOfficialManifestXml(config)
        val watchFaceInfoXml = generateWatchFaceInfoXml(config)
        val stringsXml = generateStringsXml(config)

        ZipOutputStream(baos).use { zos ->
            fun addTextEntry(path: String, content: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            fun addBinaryEntry(path: String, bytes: ByteArray) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(bytes)
                zos.closeEntry()
            }

            addTextEntry("AndroidManifest.xml", manifestXml)
            addTextEntry("res/raw/watchface.xml", wffXml)
            addTextEntry("res/xml/watch_face_info.xml", watchFaceInfoXml)
            addTextEntry("res/values/strings.xml", stringsXml)
            addBinaryEntry("res/drawable/preview.png", previewPng)
            addBinaryEntry("res/mipmap-xxxhdpi/ic_launcher.png", iconPng)
            addBinaryEntry("res/mipmap-xxxhdpi/ic_launcher_round.png", iconPng)

            getFontResourceAndFileName(config.digitalClock.fontFamily)?.let { (resId, fontFileName) ->
                try {
                    val fontBytes = context.resources.openRawResource(resId).use { it.readBytes() }
                    addBinaryEntry("res/font/$fontFileName", fontBytes)
                } catch (_: Exception) {
                }
            }
        }
        return baos.toByteArray()
    }

    private fun getFontResourceAndFileName(fontFamily: DigitalFontFamily): Pair<Int, String>? {
        return when (fontFamily) {
            DigitalFontFamily.TECH_MONO -> R.font.share_tech_mono to "share_tech_mono.ttf"
            DigitalFontFamily.CYBER_DISPLAY -> R.font.orbitron to "orbitron.ttf"
            DigitalFontFamily.BOLD_GEOMETRIC -> R.font.chakra_petch to "chakra_petch.ttf"
            DigitalFontFamily.STENCIL_MILITARY -> R.font.wallpoet to "wallpoet.ttf"
            DigitalFontFamily.RETRO_LED -> R.font.vt323 to "vt323.ttf"
            DigitalFontFamily.PIXEL_ARCADE -> R.font.silkscreen to "silkscreen.ttf"
            DigitalFontFamily.DIGITAL_7 -> R.font.orbitron to "orbitron.ttf"
            DigitalFontFamily.DOT_MATRIX -> R.font.silkscreen to "silkscreen.ttf"
            DigitalFontFamily.MODERN_SANS -> null
        }
    }

    /**
     * Shares exported binary/text files via FileProvider using a zero-permission chooser Intent.
     */
    fun shareBytesAsFile(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        chooserTitle: String
    ) {
        try {
            val exportDir = File(context.cacheDir, "watchface_exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val outFile = File(exportDir, fileName)
            outFile.writeBytes(bytes)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Export share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves the active WatchFaceConfig so WatchFaceWallpaperService renders it live.
     */
    fun saveActiveWatchFaceForService(context: Context, config: WatchFaceConfig) {
        val prefs = context.getSharedPreferences(LIVE_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACTIVE_FACE_JSON, WatchFaceJsonHelper.toJson(config)).apply()
    }

    /**
     * Loads the active WatchFaceConfig for WatchFaceWallpaperService.
     */
    fun loadActiveWatchFaceForService(context: Context): WatchFaceConfig {
        val prefs = context.getSharedPreferences(LIVE_PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ACTIVE_FACE_JSON, null)
        return if (!json.isNullOrBlank()) {
            try {
                WatchFaceJsonHelper.fromJson(json)
            } catch (_: Exception) {
                DefaultPresets.presets.first()
            }
        } else {
            DefaultPresets.presets.first()
        }
    }

    /**
     * Saves the given watch face as active and launches the system Watch Face / Live Wallpaper picker.
     */
    fun activateLiveWatchFaceOnDevice(context: Context, config: WatchFaceConfig) {
        saveActiveWatchFaceForService(context, config)
        Toast.makeText(
            context,
            "Activated '${config.name}'! Select it in the Watch Face / Wallpaper picker.",
            Toast.LENGTH_SHORT
        ).show()

        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, WatchFaceWallpaperService::class.java)
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "'${config.name}' saved as active watch face design!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Opens the Google Pixel Watch companion app or Wear OS companion app if installed,
     * or opens the official Google Pixel Watch companion listing via zero-permission ACTION_VIEW.
     */
    fun openPixelWatchCompanionApp(context: Context) {
        val pm = context.packageManager
        val pixelWatchPkg = "com.google.android.apps.wear.companion"
        val wearOsPkg = "com.google.android.wearable.app"

        val launchIntent = pm.getLaunchIntentForPackage(pixelWatchPkg)
            ?: pm.getLaunchIntentForPackage(wearOsPkg)

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            try {
                val storeIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$pixelWatchPkg")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(storeIntent)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "Google Pixel Watch companion app not detected on this device.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
