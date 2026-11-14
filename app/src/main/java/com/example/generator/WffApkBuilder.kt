package com.example.generator

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.WatchFaceConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * On-device Watch Face Format (WFF) APK Compiler & Package Installer.
 * Compiles a WatchFaceConfig directly into a standalone, signed Wear OS Watch Face (.apk)
 * on the user's phone without requiring an external computer or Gradle build.
 */
object WffApkBuilder {

    // Resource IDs in the generated WFF APK:
    // 0x7f010000 -> @drawable/preview (res/drawable/preview.png)
    // 0x7f020000 -> @xml/watch_face_info (res/xml/watch_face_info.xml)
    // 0x7f030000 -> @raw/watchface (res/raw/watchface.xml)
    // 0x7f040000 -> @string/watch_face_name
    private const val RES_DRAWABLE_PREVIEW = 0x7f010000
    private const val RES_XML_WATCH_FACE_INFO = 0x7f020000
    private const val RES_RAW_WATCHFACE = 0x7f030000
    private const val RES_STRING_WATCH_FACE_NAME = 0x7f040000

    private val cachedKeyPair: KeyPair by lazy {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        kpg.generateKeyPair()
    }

    /**
     * Compiles a complete, standalone Watch Face Format (WFF) .apk binary in memory.
     */
    fun buildWatchFaceApkBytes(context: Context, config: WatchFaceConfig): ByteArray {
        val pkgName = WatchFacePackageExporter.generatePackageName(config)
        val wffVer = WatchFacePackageExporter.getWffVersionString(config).toIntOrNull() ?: 4
        val minSdk = if (config.targetWearOsVersion >= 5) 34 else 33

        // 1. Generate APK entry payloads
        val manifestBytes = buildBinaryAndroidManifest(
            packageName = pkgName,
            watchFaceLabel = config.name,
            wffVersion = wffVer,
            minSdk = minSdk,
            targetSdk = 36
        )
        val watchFaceInfoBytes = buildBinaryWatchFaceInfoXml(
            flavorsSupported = config.targetWearOsVersion >= 5
        )
        val rawWffXmlBytes = WffXmlGenerator.generateWatchFaceXml(config).toByteArray(Charsets.UTF_8)
        val previewPngBytes = WatchFacePackageExporter.renderWatchFacePngBytes(context, config, sizePx = 450)
        val resourcesArscBytes = buildResourcesArsc(
            packageName = pkgName,
            watchFaceName = config.name
        )

        val entries = linkedMapOf(
            "AndroidManifest.xml" to manifestBytes,
            "resources.arsc" to resourcesArscBytes,
            "res/xml/watch_face_info.xml" to watchFaceInfoBytes,
            "res/raw/watchface.xml" to rawWffXmlBytes,
            "res/drawable/preview.png" to previewPngBytes
        )

        // 2. Generate JAR / APK v1 Signature files (META-INF/MANIFEST.MF, CERT.SF, CERT.RSA)
        val (manifestMf, certSf, certRsa) = signApkEntries(entries)

        // 3. Assemble ZIP / APK with 4-byte alignment for STORED entries (resources.arsc, res/raw/*, *.png)
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun writeEntry(name: String, data: ByteArray, storeUncompressed: Boolean) {
                val entry = ZipEntry(name)
                if (storeUncompressed) {
                    entry.method = ZipEntry.STORED
                    entry.size = data.size.toLong()
                    entry.compressedSize = data.size.toLong()
                    val crc = CRC32()
                    crc.update(data)
                    entry.crc = crc.value
                } else {
                    entry.method = ZipEntry.DEFLATED
                }
                zos.putNextEntry(entry)
                zos.write(data)
                zos.closeEntry()
            }

            // Write core APK resources
            writeEntry("AndroidManifest.xml", manifestBytes, storeUncompressed = false)
            writeEntry("resources.arsc", resourcesArscBytes, storeUncompressed = true)
            writeEntry("res/xml/watch_face_info.xml", watchFaceInfoBytes, storeUncompressed = false)
            writeEntry("res/raw/watchface.xml", rawWffXmlBytes, storeUncompressed = true)
            writeEntry("res/drawable/preview.png", previewPngBytes, storeUncompressed = true)

            // Write META-INF signature
            writeEntry("META-INF/MANIFEST.MF", manifestMf, storeUncompressed = false)
            writeEntry("META-INF/CERT.SF", certSf, storeUncompressed = false)
            writeEntry("META-INF/CERT.RSA", certRsa, storeUncompressed = false)
        }

        return baos.toByteArray()
    }

    /**
     * Builds the watch face APK, writes it to the FileProvider cache directory, and launches
     * the Android Package Installer to install the watch face app on the device.
     */
    fun installWatchFaceApkOnDevice(context: Context, config: WatchFaceConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(
                    context,
                    "Please enable 'Install unknown apps' to install watch face APKs directly.",
                    Toast.LENGTH_LONG
                ).show()
                try {
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                } catch (_: Exception) {
                    // Proceed to attempt install intent
                }
            }
        }

        try {
            val slug = WatchFacePackageExporter.generateSlug(config)
            val apkBytes = buildWatchFaceApkBytes(context, config)
            val exportDir = File(context.cacheDir, "watchface_exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val apkFile = File(exportDir, "${slug}_watchface.apk")
            apkFile.writeBytes(apkBytes)

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            @Suppress("DEPRECATION")
            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Could not launch package installer: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Builds the Binary AndroidManifest.xml (ResXMLTree) for a Wear OS Watch Face Format APK.
     */
    private fun buildBinaryAndroidManifest(
        packageName: String,
        watchFaceLabel: String,
        wffVersion: Int,
        minSdk: Int,
        targetSdk: Int
    ): ByteArray {
        val builder = BinaryXmlBuilder()

        // Register android: attribute names with their system resource IDs (sorted by ID where applicable)
        val attrLabel = builder.addAttrString("label", 0x01010001)
        val attrIcon = builder.addAttrString("icon", 0x01010002)
        val attrName = builder.addAttrString("name", 0x01010003)
        val attrPermission = builder.addAttrString("permission", 0x01010006)
        val attrHasCode = builder.addAttrString("hasCode", 0x0101000c)
        val attrExported = builder.addAttrString("exported", 0x01010010)
        val attrValue = builder.addAttrString("value", 0x01010024)
        val attrResource = builder.addAttrString("resource", 0x01010025)
        val attrMinSdk = builder.addAttrString("minSdkVersion", 0x0101020c)
        val attrVersionCode = builder.addAttrString("versionCode", 0x0101021b)
        val attrVersionName = builder.addAttrString("versionName", 0x0101021c)
        val attrTargetSdk = builder.addAttrString("targetSdkVersion", 0x01010270)
        val attrAllowBackup = builder.addAttrString("allowBackup", 0x01010280)
        val attrRequired = builder.addAttrString("required", 0x0101028e)

        val nsPrefix = builder.addString("android")
        val nsUri = builder.addString("http://schemas.android.com/apk/res/android")

        val tagManifest = builder.addString("manifest")
        val tagUsesSdk = builder.addString("uses-sdk")
        val tagUsesFeature = builder.addString("uses-feature")
        val tagApplication = builder.addString("application")
        val tagMetaData = builder.addString("meta-data")
        val tagProperty = builder.addString("property")
        val tagService = builder.addString("service")
        val tagIntentFilter = builder.addString("intent-filter")
        val tagAction = builder.addString("action")
        val tagCategory = builder.addString("category")

        val attrPackageNoNs = builder.addString("package")
        val valPackage = builder.addString(packageName)
        val valVerName = builder.addString("1.0")
        val valWatchFeature = builder.addString("android.hardware.type.watch")
        val valLabel = builder.addString(watchFaceLabel)
        val valStandalone = builder.addString("com.google.android.wearable.standalone")
        val valWffVersionProp = builder.addString("com.google.wear.watchface.format.version")
        val valWffPublisherProp = builder.addString("com.google.wear.watchface.format.publisher")
        val valWffPublisher = builder.addString("WatchFaceFormatStudio-4.0")
        val valWffInfoMeta = builder.addString("com.google.android.wearable.watchface.info")
        val valServiceName = builder.addString("com.google.android.wearable.watchface.wff.WffWatchFaceService")
        val valBindWallpaper = builder.addString("android.permission.BIND_WALLPAPER")
        val valWallpaperServiceAction = builder.addString("android.service.wallpaper.WallpaperService")
        val valWatchFaceCategory = builder.addString("com.google.android.wearable.watchface.category.WATCH_FACE")
        val valPreviewMeta = builder.addString("com.google.android.wearable.watchface.preview")
        val valPreviewCircMeta = builder.addString("com.google.android.wearable.watchface.preview_circular")

        builder.startNamespace(nsPrefix, nsUri)

        // <manifest>
        builder.startElement(
            nameIdx = tagManifest,
            attrs = listOf(
                BinaryAttr(nsUri, attrVersionCode, -1, BinaryXmlBuilder.TYPE_INT_DEC, 1),
                BinaryAttr(nsUri, attrVersionName, valVerName, BinaryXmlBuilder.TYPE_STRING, valVerName),
                BinaryAttr(-1, attrPackageNoNs, valPackage, BinaryXmlBuilder.TYPE_STRING, valPackage)
            )
        )

        // <uses-sdk android:minSdkVersion="33" android:targetSdkVersion="36" />
        builder.startElement(
            nameIdx = tagUsesSdk,
            attrs = listOf(
                BinaryAttr(nsUri, attrMinSdk, -1, BinaryXmlBuilder.TYPE_INT_DEC, minSdk),
                BinaryAttr(nsUri, attrTargetSdk, -1, BinaryXmlBuilder.TYPE_INT_DEC, targetSdk)
            )
        )
        builder.endElement(tagUsesSdk)

        // <uses-feature android:name="android.hardware.type.watch" android:required="true" />
        builder.startElement(
            nameIdx = tagUsesFeature,
            attrs = listOf(
                BinaryAttr(nsUri, attrName, valWatchFeature, BinaryXmlBuilder.TYPE_STRING, valWatchFeature),
                BinaryAttr(nsUri, attrRequired, -1, BinaryXmlBuilder.TYPE_INT_BOOLEAN, -1)
            )
        )
        builder.endElement(tagUsesFeature)

        // <application android:label="..." android:icon="@drawable/preview" android:hasCode="false" android:allowBackup="true">
        builder.startElement(
            nameIdx = tagApplication,
            attrs = listOf(
                BinaryAttr(nsUri, attrLabel, valLabel, BinaryXmlBuilder.TYPE_STRING, valLabel),
                BinaryAttr(nsUri, attrIcon, -1, BinaryXmlBuilder.TYPE_REFERENCE, RES_DRAWABLE_PREVIEW),
                BinaryAttr(nsUri, attrHasCode, -1, BinaryXmlBuilder.TYPE_INT_BOOLEAN, 0),
                BinaryAttr(nsUri, attrAllowBackup, -1, BinaryXmlBuilder.TYPE_INT_BOOLEAN, -1)
            )
        )

        // <property android:name="com.google.wear.watchface.format.version" android:value="4" />
        builder.startElement(
            nameIdx = tagProperty,
            attrs = listOf(
                BinaryAttr(nsUri, attrName, valWffVersionProp, BinaryXmlBuilder.TYPE_STRING, valWffVersionProp),
                BinaryAttr(nsUri, attrValue, -1, BinaryXmlBuilder.TYPE_INT_DEC, wffVersion)
            )
        )
        builder.endElement(tagProperty)

        // <property android:name="com.google.wear.watchface.format.publisher" android:value="WatchFaceFormatStudio-4.0" />
        builder.startElement(
            nameIdx = tagProperty,
            attrs = listOf(
                BinaryAttr(nsUri, attrName, valWffPublisherProp, BinaryXmlBuilder.TYPE_STRING, valWffPublisherProp),
                BinaryAttr(nsUri, attrValue, valWffPublisher, BinaryXmlBuilder.TYPE_STRING, valWffPublisher)
            )
        )
        builder.endElement(tagProperty)

        // <meta-data android:name="com.google.android.wearable.standalone" android:value="true" />
        builder.startElement(
            nameIdx = tagMetaData,
            attrs = listOf(
                BinaryAttr(nsUri, attrName, valStandalone, BinaryXmlBuilder.TYPE_STRING, valStandalone),
                BinaryAttr(nsUri, attrValue, -1, BinaryXmlBuilder.TYPE_INT_BOOLEAN, -1)
            )
        )
        builder.endElement(tagMetaData)

        // <meta-data android:name="com.google.android.wearable.watchface.info" android:resource="@xml/watch_face_info" />
        builder.startElement(
            nameIdx = tagMetaData,
            attrs = listOf(
                BinaryAttr(nsUri, attrName, valWffInfoMeta, BinaryXmlBuilder.TYPE_STRING, valWffInfoMeta),
                BinaryAttr(nsUri, attrResource, -1, BinaryXmlBuilder.TYPE_REFERENCE, RES_XML_WATCH_FACE_INFO)
            )
        )
        builder.endElement(tagMetaData)

        // <service ...>
        builder.startElement(
            nameIdx = tagService,
            attrs = listOf(
                BinaryAttr(nsUri, attrLabel, valLabel, BinaryXmlBuilder.TYPE_STRING, valLabel),
                BinaryAttr(nsUri, attrName, valServiceName, BinaryXmlBuilder.TYPE_STRING, valServiceName),
                BinaryAttr(nsUri, attrPermission, valBindWallpaper, BinaryXmlBuilder.TYPE_STRING, valBindWallpaper),
                BinaryAttr(nsUri, attrExported, -1, BinaryXmlBuilder.TYPE_INT_BOOLEAN, -1)
            )
        )

        builder.startElement(tagIntentFilter, emptyList())
        builder.startElement(
            nameIdx = tagAction,
            attrs = listOf(BinaryAttr(nsUri, attrName, valWallpaperServiceAction, BinaryXmlBuilder.TYPE_STRING, valWallpaperServiceAction))
        )
        builder.endElement(tagAction)
        builder.startElement(
            nameIdx = tagCategory,
            attrs = listOf(BinaryAttr(nsUri, attrName, valWatchFaceCategory, BinaryXmlBuilder.TYPE_STRING, valWatchFaceCategory))
        )
        builder.endElement(tagCategory)
        builder.endElement(tagIntentFilter)

        builder.startElement(
            nameIdx = tagMetaData,
            attrs = listOf(
                BinaryAttr(nsUri, attrName, valPreviewMeta, BinaryXmlBuilder.TYPE_STRING, valPreviewMeta),
                BinaryAttr(nsUri, attrResource, -1, BinaryXmlBuilder.TYPE_REFERENCE, RES_DRAWABLE_PREVIEW)
            )
        )
        builder.endElement(tagMetaData)

        builder.startElement(
            nameIdx = tagMetaData,
            attrs = listOf(
                BinaryAttr(nsUri, attrName, valPreviewCircMeta, BinaryXmlBuilder.TYPE_STRING, valPreviewCircMeta),
                BinaryAttr(nsUri, attrResource, -1, BinaryXmlBuilder.TYPE_REFERENCE, RES_DRAWABLE_PREVIEW)
            )
        )
        builder.endElement(tagMetaData)

        builder.endElement(tagService)
        builder.endElement(tagApplication)
        builder.endElement(tagManifest)
        builder.endNamespace(nsPrefix, nsUri)

        return builder.toByteArray()
    }

    /**
     * Builds the Binary res/xml/watch_face_info.xml (ResXMLTree).
     */
    private fun buildBinaryWatchFaceInfoXml(flavorsSupported: Boolean): ByteArray {
        val builder = BinaryXmlBuilder()
        val tagRoot = builder.addString("WatchFaceInfo")
        val tagPreview = builder.addString("Preview")
        val tagCategory = builder.addString("Category")
        val tagRetail = builder.addString("AvailableInRetail")
        val tagMulti = builder.addString("MultipleInstancesAllowed")
        val tagEditable = builder.addString("Editable")
        val tagFlavors = builder.addString("FlavorsSupported")
        val attrVal = builder.addString("value")
        val valCatEmpty = builder.addString("CATEGORY_EMPTY")

        builder.startElement(tagRoot, emptyList())

        builder.startElement(
            tagPreview,
            listOf(BinaryAttr(-1, attrVal, -1, BinaryXmlBuilder.TYPE_REFERENCE, RES_DRAWABLE_PREVIEW))
        )
        builder.endElement(tagPreview)

        builder.startElement(
            tagCategory,
            listOf(BinaryAttr(-1, attrVal, valCatEmpty, BinaryXmlBuilder.TYPE_STRING, valCatEmpty))
        )
        builder.endElement(tagCategory)

        builder.startElement(
            tagRetail,
            listOf(BinaryAttr(-1, attrVal, -1, BinaryXmlBuilder.TYPE_INT_BOOLEAN, -1))
        )
        builder.endElement(tagRetail)

        builder.startElement(
            tagMulti,
            listOf(BinaryAttr(-1, attrVal, -1, BinaryXmlBuilder.TYPE_INT_BOOLEAN, -1))
        )
        builder.endElement(tagMulti)

        builder.startElement(
            tagEditable,
            listOf(BinaryAttr(-1, attrVal, -1, BinaryXmlBuilder.TYPE_INT_BOOLEAN, -1))
        )
        builder.endElement(tagEditable)

        builder.startElement(
            tagFlavors,
            listOf(BinaryAttr(-1, attrVal, -1, BinaryXmlBuilder.TYPE_INT_BOOLEAN, if (flavorsSupported) -1 else 0))
        )
        builder.endElement(tagFlavors)

        builder.endElement(tagRoot)
        return builder.toByteArray()
    }

    /**
     * Builds a valid Android resources.arsc (ResTable) mapping:
     * - 0x7f010000: drawable/preview -> "res/drawable/preview.png"
     * - 0x7f020000: xml/watch_face_info -> "res/xml/watch_face_info.xml"
     * - 0x7f030000: raw/watchface -> "res/raw/watchface.xml"
     * - 0x7f040000: string/watch_face_name -> watchFaceName
     */
    private fun buildResourcesArsc(packageName: String, watchFaceName: String): ByteArray {
        val globalStrings = listOf(
            "res/drawable/preview.png",
            "res/xml/watch_face_info.xml",
            "res/raw/watchface.xml",
            watchFaceName
        )
        val typeStrings = listOf("drawable", "xml", "raw", "string")
        val keyStrings = listOf("preview", "watch_face_info", "watchface", "watch_face_name")

        val globalPoolChunk = buildStringPoolChunk(globalStrings)
        val typePoolChunk = buildStringPoolChunk(typeStrings)
        val keyPoolChunk = buildStringPoolChunk(keyStrings)

        // Build TypeSpec + Type chunks for each of the 4 types (id 1..4)
        val typeChunks = ByteArrayOutputStream()
        for (typeIdx in 0 until 4) {
            val typeId = typeIdx + 1 // 1-based
            // ResTable_typeSpec (type 0x0202, headerSize 16, size 20)
            val specBuf = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
            specBuf.putShort(0x0202.toShort())
            specBuf.putShort(16.toShort())
            specBuf.putInt(20)
            specBuf.put(typeId.toByte())
            specBuf.put(0.toByte())
            specBuf.putShort(0.toShort())
            specBuf.putInt(1) // entryCount = 1
            specBuf.putInt(0) // flags for entry 0
            typeChunks.write(specBuf.array())

            // ResTable_type (type 0x0201)
            // headerSize = 84 (20 + 64-byte ResTable_config)
            // 1 entry offset (4 bytes) + ResTable_entry (8 bytes) + Res_value (8 bytes) = 20 bytes
            val typeHeaderSize = 84
            val typeChunkSize = typeHeaderSize + 4 + 16
            val typeBuf = ByteBuffer.allocate(typeChunkSize).order(ByteOrder.LITTLE_ENDIAN)
            typeBuf.putShort(0x0201.toShort())
            typeBuf.putShort(typeHeaderSize.toShort())
            typeBuf.putInt(typeChunkSize)
            typeBuf.put(typeId.toByte())
            typeBuf.put(0.toByte())
            typeBuf.putShort(0.toShort())
            typeBuf.putInt(1) // entryCount = 1
            typeBuf.putInt(typeHeaderSize + 4) // entriesStart

            // ResTable_config (64 bytes, first 4 bytes = size = 64)
            typeBuf.putInt(64)
            typeBuf.position(typeHeaderSize)

            // Entry offset table (1 entry at offset 0)
            typeBuf.putInt(0)

            // ResTable_entry (8 bytes)
            typeBuf.putShort(8.toShort()) // size
            typeBuf.putShort(0.toShort()) // flags
            typeBuf.putInt(typeIdx)       // key index in keyStringPool

            // Res_value (8 bytes)
            typeBuf.putShort(8.toShort()) // size
            typeBuf.put(0.toByte())       // res0
            typeBuf.put(0x03.toByte())    // dataType = TYPE_STRING (points into global string pool)
            typeBuf.putInt(typeIdx)       // index in globalStrings

            typeChunks.write(typeBuf.array())
        }

        val typeChunksBytes = typeChunks.toByteArray()

        // ResTable_package header (type 0x0200, headerSize 288)
        val pkgHeaderSize = 288
        val pkgChunkSize = pkgHeaderSize + typePoolChunk.size + keyPoolChunk.size + typeChunksBytes.size
        val pkgBuf = ByteBuffer.allocate(pkgChunkSize).order(ByteOrder.LITTLE_ENDIAN)
        pkgBuf.putShort(0x0200.toShort())
        pkgBuf.putShort(pkgHeaderSize.toShort())
        pkgBuf.putInt(pkgChunkSize)
        pkgBuf.putInt(0x7f) // package ID = 0x7f

        // Package name as 128 char16_t (256 bytes)
        val pkgChars = packageName.take(127)
        for (i in 0 until 128) {
            val ch = if (i < pkgChars.length) pkgChars[i].code.toShort() else 0.toShort()
            pkgBuf.putShort(ch)
        }

        val typeStringsOffset = pkgHeaderSize
        val keyStringsOffset = pkgHeaderSize + typePoolChunk.size
        pkgBuf.putInt(typeStringsOffset) // typeStrings offset
        pkgBuf.putInt(typeStrings.size)  // lastPublicType
        pkgBuf.putInt(keyStringsOffset)  // keyStrings offset
        pkgBuf.putInt(keyStrings.size)   // lastPublicKey
        pkgBuf.putInt(0)                 // typeIdOffset

        pkgBuf.put(typePoolChunk)
        pkgBuf.put(keyPoolChunk)
        pkgBuf.put(typeChunksBytes)
        val pkgBytes = pkgBuf.array()

        // Root ResTable_header (type 0x0002, headerSize 12)
        val totalSize = 12 + globalPoolChunk.size + pkgBytes.size
        val rootBuf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        rootBuf.putShort(0x0002.toShort())
        rootBuf.putShort(12.toShort())
        rootBuf.putInt(totalSize)
        rootBuf.putInt(1) // packageCount = 1
        rootBuf.put(globalPoolChunk)
        rootBuf.put(pkgBytes)

        return rootBuf.array()
    }

    /**
     * Generates JAR / APK v1 signature files (MANIFEST.MF, CERT.SF, CERT.RSA).
     */
    private fun signApkEntries(entries: Map<String, ByteArray>): Triple<ByteArray, ByteArray, ByteArray> {
        val sha256 = MessageDigest.getInstance("SHA-256")

        val mfSb = StringBuilder()
        mfSb.append("Manifest-Version: 1.0\r\n")
        mfSb.append("Created-By: WatchFaceFormatStudio 4.0\r\n\r\n")

        val sectionHashes = linkedMapOf<String, String>()
        for ((name, bytes) in entries) {
            val digest = Base64.encodeToString(sha256.digest(bytes), Base64.NO_WRAP)
            val section = "Name: $name\r\nSHA-256-Digest: $digest\r\n\r\n"
            mfSb.append(section)
            sectionHashes[name] = Base64.encodeToString(
                sha256.digest(section.toByteArray(Charsets.UTF_8)),
                Base64.NO_WRAP
            )
        }

        val manifestMfBytes = mfSb.toString().toByteArray(Charsets.UTF_8)
        val wholeManifestDigest = Base64.encodeToString(sha256.digest(manifestMfBytes), Base64.NO_WRAP)

        val sfSb = StringBuilder()
        sfSb.append("Signature-Version: 1.0\r\n")
        sfSb.append("Created-By: WatchFaceFormatStudio 4.0\r\n")
        sfSb.append("SHA-256-Digest-Manifest: $wholeManifestDigest\r\n\r\n")
        for ((name, secDigest) in sectionHashes) {
            sfSb.append("Name: $name\r\nSHA-256-Digest: $secDigest\r\n\r\n")
        }

        val certSfBytes = sfSb.toString().toByteArray(Charsets.UTF_8)

        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(cachedKeyPair.private)
        sig.update(certSfBytes)
        val signatureBytes = sig.sign()

        return Triple(manifestMfBytes, certSfBytes, signatureBytes)
    }
}

private data class BinaryAttr(
    val nsIdx: Int,
    val nameIdx: Int,
    val rawValIdx: Int,
    val dataType: Int,
    val data: Int
)

private class BinaryXmlBuilder {
    companion object {
        const val TYPE_REFERENCE = 0x01
        const val TYPE_STRING = 0x03
        const val TYPE_INT_DEC = 0x10
        const val TYPE_INT_BOOLEAN = 0x12
    }

    private val attrStrings = mutableListOf<Pair<String, Int>>()
    private val generalStrings = mutableListOf<String>()
    private val bodyChunks = ByteArrayOutputStream()

    fun addAttrString(name: String, resId: Int): Int {
        val existing = attrStrings.indexOfFirst { it.first == name }
        if (existing >= 0) return existing
        attrStrings.add(name to resId)
        return attrStrings.lastIndex
    }

    fun addString(value: String): Int {
        val attrIdx = attrStrings.indexOfFirst { it.first == value }
        if (attrIdx >= 0) return attrIdx
        val genIdx = generalStrings.indexOf(value)
        if (genIdx >= 0) return attrStrings.size + genIdx
        generalStrings.add(value)
        return attrStrings.size + generalStrings.lastIndex
    }

    fun startNamespace(prefixIdx: Int, uriIdx: Int) {
        val buf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(0x0100.toShort())
        buf.putShort(16.toShort())
        buf.putInt(24)
        buf.putInt(1)
        buf.putInt(-1)
        buf.putInt(prefixIdx)
        buf.putInt(uriIdx)
        bodyChunks.write(buf.array())
    }

    fun endNamespace(prefixIdx: Int, uriIdx: Int) {
        val buf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(0x0101.toShort())
        buf.putShort(16.toShort())
        buf.putInt(24)
        buf.putInt(1)
        buf.putInt(-1)
        buf.putInt(prefixIdx)
        buf.putInt(uriIdx)
        bodyChunks.write(buf.array())
    }

    fun startElement(nameIdx: Int, attrs: List<BinaryAttr>) {
        val chunkSize = 36 + attrs.size * 20
        val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(0x0102.toShort())
        buf.putShort(16.toShort())
        buf.putInt(chunkSize)
        buf.putInt(1)  // lineNumber
        buf.putInt(-1) // comment
        buf.putInt(-1) // ns
        buf.putInt(nameIdx)
        buf.putShort(20.toShort()) // attributeStart
        buf.putShort(20.toShort()) // attributeSize
        buf.putShort(attrs.size.toShort())
        buf.putShort(0.toShort())  // idIndex
        buf.putShort(0.toShort())  // classIndex
        buf.putShort(0.toShort())  // styleIndex

        for (attr in attrs) {
            buf.putInt(attr.nsIdx)
            buf.putInt(attr.nameIdx)
            buf.putInt(attr.rawValIdx)
            buf.putShort(8.toShort())
            buf.put(0.toByte())
            buf.put(attr.dataType.toByte())
            buf.putInt(attr.data)
        }
        bodyChunks.write(buf.array())
    }

    fun endElement(nameIdx: Int) {
        val buf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(0x0103.toShort())
        buf.putShort(16.toShort())
        buf.putInt(24)
        buf.putInt(1)
        buf.putInt(-1)
        buf.putInt(-1)
        buf.putInt(nameIdx)
        bodyChunks.write(buf.array())
    }

    fun toByteArray(): ByteArray {
        val allStrings = attrStrings.map { it.first } + generalStrings
        val stringPoolBytes = buildStringPoolChunk(allStrings)

        val resMapBytes = if (attrStrings.isNotEmpty()) {
            val mapSize = 8 + attrStrings.size * 4
            val mapBuf = ByteBuffer.allocate(mapSize).order(ByteOrder.LITTLE_ENDIAN)
            mapBuf.putShort(0x0180.toShort())
            mapBuf.putShort(8.toShort())
            mapBuf.putInt(mapSize)
            for ((_, resId) in attrStrings) {
                mapBuf.putInt(resId)
            }
            mapBuf.array()
        } else {
            ByteArray(0)
        }

        val bodyBytes = bodyChunks.toByteArray()
        val totalSize = 8 + stringPoolBytes.size + resMapBytes.size + bodyBytes.size
        val out = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(0x0003.toShort()) // RES_XML_TYPE
        out.putShort(8.toShort())
        out.putInt(totalSize)
        out.put(stringPoolBytes)
        out.put(resMapBytes)
        out.put(bodyBytes)
        return out.array()
    }
}

private fun buildStringPoolChunk(strings: List<String>): ByteArray {
    val stringDataOut = ByteArrayOutputStream()
    val offsets = IntArray(strings.size)

    strings.forEachIndexed { index, s ->
        offsets[index] = stringDataOut.size()
        val len = s.length
        // UTF-16LE length (2 bytes) + chars (len * 2 bytes) + null terminator (2 bytes)
        val strBuf = ByteBuffer.allocate(2 + len * 2 + 2).order(ByteOrder.LITTLE_ENDIAN)
        strBuf.putShort(len.toShort())
        for (ch in s) {
            strBuf.putShort(ch.code.toShort())
        }
        strBuf.putShort(0.toShort())
        stringDataOut.write(strBuf.array())
    }

    // Pad string data to 4-byte boundary
    while (stringDataOut.size() % 4 != 0) {
        stringDataOut.write(0)
    }

    val stringDataBytes = stringDataOut.toByteArray()
    val headerSize = 28
    val stringsStart = headerSize + strings.size * 4
    val chunkSize = stringsStart + stringDataBytes.size

    val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
    buf.putShort(0x0001.toShort()) // RES_STRING_POOL_TYPE
    buf.putShort(headerSize.toShort())
    buf.putInt(chunkSize)
    buf.putInt(strings.size)
    buf.putInt(0) // styleCount
    buf.putInt(0) // flags = 0 (UTF-16LE)
    buf.putInt(stringsStart)
    buf.putInt(0) // stylesStart
    for (offset in offsets) {
        buf.putInt(offset)
    }
    buf.put(stringDataBytes)
    return buf.array()
}
