package com.example.watchface

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.generator.WatchFacePackageExporter
import com.example.model.HandMotion
import com.example.model.WatchFaceConfig
import com.example.previewer.PreviewSimulationState
import com.example.previewer.renderWatchFaceFrame
import java.util.Calendar

/**
 * Built-in Wear OS Watch Face & Android Live Wallpaper Service.
 * Renders the user's active custom watch face design directly onto the watch or phone surface.
 */
class WatchFaceWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return WatchFaceEngine()
    }

    inner class WatchFaceEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val drawScope = CanvasDrawScope()
        private var isVisibleFlag = false
        private var activeConfig: WatchFaceConfig =
            WatchFacePackageExporter.loadActiveWatchFaceForService(this@WatchFaceWallpaperService)

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (isVisibleFlag) {
                    val delayMs = if (activeConfig.analogHands.secondHandMotion == HandMotion.SWEEP) {
                        66L // ~15 FPS for smooth sweep
                    } else {
                        500L // 2 FPS for stepped tick / digital
                    }
                    handler.postDelayed(this, delayMs)
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisibleFlag = visible
            if (visible) {
                activeConfig = WatchFacePackageExporter.loadActiveWatchFaceForService(this@WatchFaceWallpaperService)
                handler.removeCallbacks(drawRunnable)
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            activeConfig = WatchFacePackageExporter.loadActiveWatchFaceForService(this@WatchFaceWallpaperService)
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            isVisibleFlag = false
            handler.removeCallbacks(drawRunnable)
        }

        override fun onDestroy() {
            super.onDestroy()
            isVisibleFlag = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            var androidCanvas: android.graphics.Canvas? = null
            try {
                androidCanvas = holder.lockCanvas()
                if (androidCanvas != null) {
                    androidCanvas.drawColor(Color.BLACK)
                    val w = androidCanvas.width.toFloat()
                    val h = androidCanvas.height.toFloat()
                    val watchSize = minOf(w, h) * 0.94f
                    val offsetX = (w - watchSize) / 2f
                    val offsetY = (h - watchSize) / 2f

                    val cal = Calendar.getInstance()
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    val second = cal.get(Calendar.SECOND) + (cal.get(Calendar.MILLISECOND) / 1000f)

                    val state = PreviewSimulationState(
                        isAmbient = false,
                        showGuidelines = false,
                        simulatedHour = hour,
                        simulatedMinute = minute,
                        simulatedSecond = second,
                        isLiveTime = true
                    )

                    androidCanvas.save()
                    androidCanvas.translate(offsetX, offsetY)

                    val composeCanvas = Canvas(androidCanvas)
                    drawScope.draw(
                        density = Density(watchSize / 450f, 1f),
                        layoutDirection = LayoutDirection.Ltr,
                        canvas = composeCanvas,
                        size = Size(watchSize, watchSize)
                    ) {
                        renderWatchFaceFrame(
                            context = this@WatchFaceWallpaperService,
                            config = activeConfig,
                            state = state,
                            hour = hour,
                            minute = minute,
                            second = second
                        )
                    }
                    androidCanvas.restore()
                }
            } catch (_: Exception) {
                // Ignore transient surface lock exceptions
            } finally {
                if (androidCanvas != null) {
                    try {
                        holder.unlockCanvasAndPost(androidCanvas)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }
}
