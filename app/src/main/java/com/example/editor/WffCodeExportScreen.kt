package com.example.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.generator.ValidationSeverity
import com.example.generator.WatchFacePackageExporter
import com.example.generator.WffApkBuilder
import com.example.generator.WffValidator
import com.example.generator.WffXmlGenerator
import com.example.model.WatchFaceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ExportFileType(val fileName: String, val tabLabel: String) {
    WATCHFACE_XML("res/raw/watchface.xml", "watchface.xml"),
    MANIFEST_XML("AndroidManifest.xml", "Manifest"),
    WATCH_FACE_INFO("res/xml/watch_face_info.xml", "watch_face_info"),
    BUILD_GRADLE("app/build.gradle.kts", "build.gradle"),
    DEPLOY_GUIDE("README_PIXEL_WATCH.md", "Pixel Guide")
}

@Composable
fun WffCodeExportScreen(
    config: WatchFaceConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    var selectedFileIndex by remember { mutableIntStateOf(0) }
    var watchIp by remember { mutableStateOf("192.168.1.100") }
    var pairingPort by remember { mutableStateOf("38123") }
    var debugPort by remember { mutableStateOf("41567") }
    var isExporting by remember { mutableStateOf(false) }

    val slug = remember(config.name) { WatchFacePackageExporter.generateSlug(config) }
    val packageName = remember(config.name) { WatchFacePackageExporter.generatePackageName(config) }
    val wffVersion = remember(config.targetWearOsVersion) { WatchFacePackageExporter.getWffVersionString(config) }

    // Storage Access Framework launcher for saving compiled Watch Face APK (.apk)
    val saveApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            scope.launch {
                try {
                    val apkBytes = withContext(Dispatchers.Default) {
                        WffApkBuilder.buildWatchFaceApkBytes(context, config)
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(apkBytes) }
                    }
                    Toast.makeText(
                        context,
                        "Saved Watch Face APK (${apkBytes.size / 1024} KB)!",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Save APK failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isExporting = false
                }
            }
        }
    }

    // Storage Access Framework launcher for saving complete Android Studio / Gradle WFF App Project (.zip)
    val saveProjectZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            scope.launch {
                try {
                    val zipBytes = withContext(Dispatchers.Default) {
                        WatchFacePackageExporter.buildAndroidStudioProjectZip(
                            context = context,
                            config = config,
                            watchIp = watchIp,
                            debugPort = debugPort
                        )
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(zipBytes) }
                    }
                    Toast.makeText(
                        context,
                        "Saved Watch Face App Project (${zipBytes.size / 1024} KB)!",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Save failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isExporting = false
                }
            }
        }
    }

    // Storage Access Framework launcher for saving Raw WFF Resource Bundle (.wff.zip)
    val saveRawBundleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            scope.launch {
                try {
                    val zipBytes = withContext(Dispatchers.Default) {
                        WatchFacePackageExporter.buildRawWffBundleZip(context, config)
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(zipBytes) }
                    }
                    Toast.makeText(
                        context,
                        "Saved Raw WFF Bundle (${zipBytes.size / 1024} KB)!",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Save failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isExporting = false
                }
            }
        }
    }

    // Storage Access Framework launcher for saving 450x450 Watch Face Preview PNG
    val savePreviewPngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val pngBytes = withContext(Dispatchers.Default) {
                        WatchFacePackageExporter.renderWatchFacePngBytes(context, config, sizePx = 512)
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(pngBytes) }
                    }
                    Toast.makeText(context, "Saved Watch Face Preview PNG!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Save PNG failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val xmlContent = remember(config) {
        WffXmlGenerator.generateWatchFaceXml(config)
    }

    val manifestContent = remember(config) {
        WatchFacePackageExporter.generateOfficialManifestXml(config)
    }

    val watchFaceInfoContent = remember(config) {
        WatchFacePackageExporter.generateWatchFaceInfoXml(config)
    }

    val gradleContent = remember(config) {
        WatchFacePackageExporter.generateAppBuildGradleKts(config)
    }

    val guideContent = remember(config, watchIp, pairingPort, debugPort) {
        WatchFacePackageExporter.generatePixelWatchDeployGuide(config, watchIp, pairingPort, debugPort)
    }

    val currentContent = when (ExportFileType.values()[selectedFileIndex]) {
        ExportFileType.WATCHFACE_XML -> xmlContent
        ExportFileType.MANIFEST_XML -> manifestContent
        ExportFileType.WATCH_FACE_INFO -> watchFaceInfoContent
        ExportFileType.BUILD_GRADLE -> gradleContent
        ExportFileType.DEPLOY_GUIDE -> guideContent
    }

    val validationReport = remember(config) {
        WffValidator.validate(config)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Primary Export & Install to Pixel Watch Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111C30)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.55f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Watch,
                                contentDescription = "Pixel Watch Export",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Export & Add to Pixel Watch",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                "Package ID: $packageName",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF00E5FF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "WFF v$wffVersion",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF090D16)
                        )
                    }
                }

                Text(
                    "Compile '${config.name}' directly on your device into a standalone Wear OS Watch Face (.APK) or Android Studio Project (.ZIP) with unique package ID ($packageName).",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 15.sp
                )

                // Direct On-Device APK Installer Button
                Button(
                    onClick = {
                        isExporting = true
                        scope.launch {
                            try {
                                WffApkBuilder.installWatchFaceApkOnDevice(context, config)
                            } finally {
                                isExporting = false
                            }
                        }
                    },
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("install_watch_face_apk_button")
                ) {
                    Icon(
                        Icons.Default.Watch,
                        contentDescription = "Install Watch Face APK",
                        tint = Color(0xFF090D16),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Build & Install Watch Face (.APK)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF090D16)
                    )
                }

                // Save APK + Share APK Row (for Wear Installer 2 / Bugjaeger / ADB to Pixel Watch)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            saveApkLauncher.launch("${slug}_watchface.apk")
                        },
                        enabled = !isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("save_watch_face_apk_button")
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Save Watch Face APK",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Save .APK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            isExporting = true
                            scope.launch {
                                try {
                                    val apkBytes = withContext(Dispatchers.Default) {
                                        WffApkBuilder.buildWatchFaceApkBytes(context, config)
                                    }
                                    WatchFacePackageExporter.shareBytesAsFile(
                                        context = context,
                                        fileName = "${slug}_watchface.apk",
                                        mimeType = "application/vnd.android.package-archive",
                                        bytes = apkBytes,
                                        chooserTitle = "Send '${config.name}' APK to Pixel Watch / Wear Installer"
                                    )
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        enabled = !isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("share_watch_face_apk_button")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Watch Face APK",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Share .APK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Secondary Row: Save App Project (.ZIP) + Share App Package (.ZIP)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            saveProjectZipLauncher.launch("${slug}_pixel_watch_app.zip")
                        },
                        enabled = !isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("export_watch_face_project_zip_button")
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Save Watch Face App Project ZIP",
                            tint = Color(0xFF090D16),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Save App (.ZIP)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF090D16)
                        )
                    }

                    Button(
                        onClick = {
                            isExporting = true
                            scope.launch {
                                try {
                                    val zipBytes = withContext(Dispatchers.Default) {
                                        WatchFacePackageExporter.buildAndroidStudioProjectZip(
                                            context = context,
                                            config = config,
                                            watchIp = watchIp,
                                            debugPort = debugPort
                                        )
                                    }
                                    WatchFacePackageExporter.shareBytesAsFile(
                                        context = context,
                                        fileName = "${slug}_pixel_watch_app.zip",
                                        mimeType = "application/zip",
                                        bytes = zipBytes,
                                        chooserTitle = "Export '${config.name}' Watch Face App (.ZIP)"
                                    )
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        enabled = !isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("share_watch_face_project_zip_button")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Watch Face App Package ZIP",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Share App (.ZIP)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Secondary Row: Raw WFF Bundle (.wff.zip) + Save Preview PNG
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            saveRawBundleLauncher.launch("${slug}.wff.zip")
                        },
                        enabled = !isExporting,
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = SolidColor(Color(0xFF334155))
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("save_raw_wff_bundle_button")
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Save Raw WFF Bundle",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Raw WFF (.wff.zip)",
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            savePreviewPngLauncher.launch("${slug}_preview.png")
                        },
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = SolidColor(Color(0xFF334155))
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("save_preview_png_button")
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Save Preview PNG",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Preview (.PNG)",
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                // Device & Pixel Watch Companion Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            WatchFacePackageExporter.activateLiveWatchFaceOnDevice(context, config)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("activate_live_watch_face_button")
                    ) {
                        Icon(
                            Icons.Default.Watch,
                            contentDescription = "Set Active Watch Face on Device",
                            tint = Color(0xFF090D16),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Set Active on Device",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF090D16)
                        )
                    }

                    Button(
                        onClick = {
                            WatchFacePackageExporter.openPixelWatchCompanionApp(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("open_pixel_watch_companion_button")
                    ) {
                        Icon(
                            Icons.Default.Launch,
                            contentDescription = "Open Pixel Watch Companion",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Pixel Watch App",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 2. Wireless ADB Pixel Watch Command Builder Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Pixel Watch Wireless Debugging Installer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }

                Text(
                    "Enter your Pixel Watch Wi-Fi IP and ports from Settings → Developer Options → Wireless Debugging to customize the one-click install scripts inside the exported ZIP:",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = watchIp,
                        onValueChange = { watchIp = it.trim() },
                        label = { Text("Watch IP", fontSize = 10.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF00E5FF),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("watch_ip_input")
                    )

                    OutlinedTextField(
                        value = pairingPort,
                        onValueChange = { pairingPort = it.trim() },
                        label = { Text("Pair Port", fontSize = 10.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF00E5FF),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("watch_pair_port_input")
                    )

                    OutlinedTextField(
                        value = debugPort,
                        onValueChange = { debugPort = it.trim() },
                        label = { Text("Debug Port", fontSize = 10.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF00E5FF),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("watch_debug_port_input")
                    )
                }

                val quickCommand = remember(slug, packageName, watchIp, pairingPort, debugPort) {
                    val ip = watchIp.ifBlank { "192.168.1.100" }
                    val pPort = pairingPort.ifBlank { "38123" }
                    val dPort = debugPort.ifBlank { "41567" }
                    "adb pair $ip:$pPort && adb connect $ip:$dPort && ./install_to_pixel_watch.sh $ip:$dPort"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF070B14))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = quickCommand,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF38BDF8),
                        lineHeight = 14.sp
                    )
                }

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Pixel Watch ADB Command", quickCommand)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied Pixel Watch ADB command!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("copy_adb_command_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy ADB Command", tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Wireless ADB Deploy Command", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }

        // 3. Validation & Compliance Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Wear OS ${config.targetWearOsVersion} WFF Validation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (validationReport.isAmbientCompliant) Color(0xFF10B981) else Color(0xFFF43F5E))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (validationReport.isAmbientCompliant) "PASSED" else "WARNING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF090D16)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                validationReport.issues.forEach { issue ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val icon = when (issue.severity) {
                            ValidationSeverity.PASS -> Icons.Default.CheckCircle
                            ValidationSeverity.WARNING -> Icons.Default.Warning
                            ValidationSeverity.ERROR -> Icons.Default.Warning
                            ValidationSeverity.INFO -> Icons.Default.Info
                        }
                        val tint = when (issue.severity) {
                            ValidationSeverity.PASS -> Color(0xFF10B981)
                            ValidationSeverity.WARNING -> Color(0xFFF59E0B)
                            ValidationSeverity.ERROR -> Color(0xFFF43F5E)
                            ValidationSeverity.INFO -> Color(0xFF38BDF8)
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                issue.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                issue.description,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }

        // 4. Segmented File Selector for Inspecting Generated Source Files
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0D1424))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ExportFileType.values().forEachIndexed { index, fileType ->
                val isSelected = selectedFileIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF00E5FF) else Color.Transparent)
                        .clickable { selectedFileIndex = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        fileType.tabLabel,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF090D16) else Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Action Buttons for Individual File
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(ExportFileType.values()[selectedFileIndex].fileName, currentContent)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied ${ExportFileType.values()[selectedFileIndex].fileName}!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("copy_wff_xml_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy File", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = {
                    val selectedFile = ExportFileType.values()[selectedFileIndex]
                    val simpleName = selectedFile.fileName.substringAfterLast("/")
                    WatchFacePackageExporter.shareBytesAsFile(
                        context = context,
                        fileName = "${slug}_$simpleName",
                        mimeType = if (simpleName.endsWith(".xml")) "text/xml" else "text/plain",
                        bytes = currentContent.toByteArray(Charsets.UTF_8),
                        chooserTitle = "Share ${selectedFile.fileName}"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("share_wff_xml_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share File", fontSize = 12.sp, color = Color.White)
            }
        }

        // Code Viewer Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                ExportFileType.values()[selectedFileIndex].fileName,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF38BDF8)
            )
            Text(
                "${currentContent.lines().size} lines",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }

        // Code Box with Syntax Highlighting & Line Numbers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF070B14))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                val lines = currentContent.lines()
                val lineNums = (1..lines.size).joinToString("\n") { it.toString().padStart(3, ' ') }
                Text(
                    text = lineNums,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Text(
                    text = currentContent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
