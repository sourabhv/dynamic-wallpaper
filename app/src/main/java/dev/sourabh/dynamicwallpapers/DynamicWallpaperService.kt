package dev.sourabh.dynamicwallpapers

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import java.util.*

class DynamicWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return JourneyWallpaperEngine(this)
    }

    inner class JourneyWallpaperEngine(private val context: Context) : WallpaperService.Engine() {

        private var visible = false
        private val updateDelayMs = 5000L;
        private var width = 0
        private var height = 0
        private val base = this.getBitmap("journey_base.png");
        private val l1 = this.getBitmap("journey_night_1.png");
        private val l2 = this.getBitmap("journey_night_2.png");

        private var holder: SurfaceHolder? = null
        private val handler = Handler(Looper.getMainLooper());
        private val updateWallpaper = Runnable {
            this.draw()
        }


        private fun getBitmap(path: String): Bitmap {
            return BitmapFactory.decodeStream(context.assets.open(path))
        }

        private fun draw() {
            if (visible) {
                this.holder?.let { hldr ->
                    hldr.lockCanvas()?.let { canvas ->
                        val dstRect = Rect(0, 0, width, height)
                        val alphas = this.getOpacitiesByTime()

                        canvas.drawRect(dstRect, Paint().apply {
                            isAntiAlias = true
                            color = Color.BLACK
                        })

                        Log.d("WALLPAPER", "${alphas[0]}, ${alphas[1]}, ${alphas[2]}")
                        canvas.drawBitmap(base, null, dstRect, Paint().apply { alpha = alphas[0] })
                        canvas.drawBitmap(l1, null, dstRect, Paint().apply { alpha = alphas[1] })
                        canvas.drawBitmap(l2, null, dstRect, Paint().apply { alpha = alphas[2] })
                        hldr.unlockCanvasAndPost(canvas)
                    }
                }
                handler.removeCallbacks(updateWallpaper)
                handler.postDelayed(updateWallpaper, updateDelayMs)
            }
        }

        private fun interpolate(value: Float, from: Array<Float>, to: Array<Float>): Float {
            if (from.size < 2 || to.size < 2 || from.size != to.size) {
                throw RuntimeException("from and to ranges should have same size and at least 2 length")
            }

            // out of bounds clamp
            if (value <= from[0]) return to[0]
            if (value >= from[from.lastIndex]) return to[to.lastIndex]

            val range = from.indexOfFirst { it >= value } - 1;
            return (((value - from[range]) * (to[range + 1] - to[range])) / (from[range + 1] - from[range])) + to[range]
        }

        private fun getOpacitiesByTime(): Array<Int> {
            val cal = Calendar.getInstance()
            val hr = cal.get(Calendar.HOUR_OF_DAY);
            Log.d("WALLPAPER", "Hour of day, $hr")

            val o1 = this.interpolate(
                hr.toFloat(),
                arrayOf(0F,   3F,   6F,   9F,  12F, 15F, 18F, 21F, 23F),
                arrayOf(.85F, .9F, .85F, .7F, .3F,  0F, .3F, .5F, .85F)
            )
            Log.d("WALLPAPER", "O1, $o1")
            val o2 = this.interpolate(
                hr.toFloat(),
                arrayOf(0F, 3F, 6F, 9F, 12F, 15F, 18F, 21F, 23F),
                arrayOf(.3F, .35F, .3F, .2F, .1F, 0F, .1F, .2F, .3F)
            )
            Log.d("WALLPAPER", "O2, $o2")
            return arrayOf(255, (o1 * 255).toInt(), (o2 * 255).toInt());
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            this.holder = surfaceHolder;
            if (visible) {
                handler.post(updateWallpaper)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            this.visible = visible
            if (visible) {
                handler.post(updateWallpaper)
            } else {
                handler.removeCallbacks(updateWallpaper)
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width
            this.height = height
            if (visible) {
                handler.removeCallbacks(updateWallpaper)
                handler.post(updateWallpaper)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            this.visible = false;
            handler.removeCallbacks(updateWallpaper);
        }
    }
}