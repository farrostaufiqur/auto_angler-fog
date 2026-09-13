package tf.fog.autoangler.game

import android.graphics.Bitmap
import android.graphics.Rect
import tf.fog.autoangler.util.Config
import kotlin.math.max

object AutoCalibrator {

    // Posisi tombol (% layar) ada di Config.kt / bot_config.txt — bisa di-tuning tanpa rebuild.
    fun calibrate(screenW: Int, screenH: Int): ButtonLayout = ButtonLayout(
        castX = (screenW * Config.castXPct).toInt(), castY = (screenH * Config.castYPct).toInt(),
        reelX = (screenW * Config.castXPct).toInt(), reelY = (screenH * Config.castYPct).toInt(),
        leftArrowX = (screenW * Config.leftArrowXPct).toInt(), leftArrowY = (screenH * Config.leftArrowYPct).toInt(),
        rightArrowX = (screenW * Config.rightArrowXPct).toInt(), rightArrowY = (screenH * Config.rightArrowYPct).toInt(),
        barLeft = (screenW * Config.barTrackLeftPct).toInt(),
        barRight = (screenW * Config.barTrackRightPct).toInt(),
        screenW = screenW, screenH = screenH
    )

    /**
     * Saat FISHING pertama terdeteksi: cari BARIS bar (tempat kuning DAN hijau muncul
     * berdekatan dalam x-span SEMPIT). Kembalikan pita ROI ketat + rentang x track bar.
     * Return null kalau span kuning+hijau terlalu lebar (kena scenery, bukan bar).
     */
    fun findFishingBarRoi(frame: Bitmap, screenW: Int, screenH: Int): Rect? {
        val maxY = (frame.height * 0.16).toInt().coerceAtMost(frame.height - 1)
        val w = frame.width
        val row = IntArray(w)
        val maxSpan = (screenW * Config.roiFindMaxSpanPct).toInt()

        var bestRow = -1
        var bestSpanL = 0
        var bestSpanR = 0
        var bestScore = Int.MAX_VALUE

        for (y in 0 until maxY) {
            frame.getPixels(row, 0, w, 0, y, w, 1)
            var yL = -1; var yR = -1
            var gL = -1; var gR = -1
            var x = 0
            while (x < w) {
                val c = row[x]
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                // heuristik cepat (bukan HSV penuh) — cukup untuk cari baris kandidat
                if (r > 180 && g > 150 && b < 140) { if (yL < 0) yL = x; yR = x }
                if (g > 140 && g >= r + 30 && g >= b) { if (gL < 0) gL = x; gR = x }
                x++
            }
            if (yL < 0 || gL < 0) continue
            val spanL = minOf(yL, gL)
            val spanR = maxOf(yR, gR)
            val span = spanR - spanL
            if (span > maxSpan) continue          // terlalu lebar → scenery, bukan bar
            // skor: makin sempit makin bagus
            if (span < bestScore) {
                bestScore = span; bestRow = y; bestSpanL = spanL; bestSpanR = spanR
            }
        }

        if (bestRow < 0) return null

        val band = max(6, (screenH * Config.roiBandPct).toInt())
        val top = (bestRow - band).coerceAtLeast(0)
        val bottom = (bestRow + band).coerceAtMost(screenH - 1)
        val margin = (screenW * 0.04).toInt()
        val trackL = (bestSpanL - margin).coerceAtLeast(0)
        val trackR = (bestSpanR + margin).coerceAtMost(screenW - 1)
        // Rect: left/right dipakai sebagai track bar (VisionProcessor batasi scan ke sini).
        return Rect(trackL, top, trackR, bottom)
    }
}
