package tf.fog.autoangler.game

import android.graphics.Bitmap
import tf.fog.autoangler.util.Config

object StateDetector {

    fun detect(frame: Bitmap, layout: ButtonLayout): GameState {
        if (hasResultScreen(frame, layout)) return GameState.RESULT
        if (hasHookedBanner(frame)) return GameState.HOOKED
        return GameState.IDLE
    }

    /**
     * Layar hasil tangkapan = popup terang menutupi TENGAH layar (ring + gambar ikan)
     * DAN area tombol cast (dock kanan-bawah) juga tertutup/terang → tombol aksi hilang.
     * Dua syarat sekaligus supaya langit terang / efek portal biru tak memicu.
     * (Menggantikan cek "pink di strip atas" yang regresi di map sunset/sakura.)
     */
    private fun hasResultScreen(frame: Bitmap, layout: ButtonLayout): Boolean {
        val w = frame.width; val h = frame.height
        val hsv = FloatArray(3)

        // 1. grid tengah terang
        val cx0 = (w * 0.35).toInt(); val cx1 = (w * 0.65).toInt()
        val cy0 = (h * 0.30).toInt(); val cy1 = (h * 0.66).toInt()
        var bright = 0; var total = 0
        val rowC = IntArray(cx1 - cx0)
        var y = cy0
        while (y < cy1) {
            frame.getPixels(rowC, 0, rowC.size, cx0, y, rowC.size, 1)
            var i = 0
            while (i < rowC.size) {
                android.graphics.Color.colorToHSV(rowC[i], hsv)
                if (hsv[2] >= Config.resultCenterVMin) bright++
                total++
                i += 3
            }
            y += 3
        }
        if (total == 0 || bright.toDouble() / total < Config.resultCenterFrac) return false

        // 2. area dock cast juga terang (panel hasil menutupinya) → tombol aksi hilang
        val dx0 = (layout.castX - w * 0.06).toInt().coerceAtLeast(0)
        val dx1 = (layout.castX + w * 0.06).toInt().coerceAtMost(w - 1)
        val dy0 = (layout.castY - h * 0.05).toInt().coerceAtLeast(0)
        val dy1 = (layout.castY + h * 0.05).toInt().coerceAtMost(h - 1)
        var dBright = 0; var dTotal = 0
        val rowD = IntArray(dx1 - dx0)
        var yy = dy0
        while (yy < dy1) {
            frame.getPixels(rowD, 0, rowD.size, dx0, yy, rowD.size, 1)
            var i = 0
            while (i < rowD.size) {
                android.graphics.Color.colorToHSV(rowD[i], hsv)
                if (hsv[2] >= Config.resultDockVMin) dBright++
                dTotal++
                i += 2
            }
            yy += 2
        }
        return dTotal > 0 && dBright.toDouble() / dTotal >= 0.55
    }

    /**
     * Banner "Fish on the hook! Press the button to reel it!" — overlay lebar & pendek
     * di pita `hookedYPct ± hookedBandPct`. Deteksi: ada baris di mana bagian tengah
     * (x 25-75%) "kena overlay" (kontras rendah + lebih redup/rata dari gameplay), DAN
     * pita di atas & di bawah band TIDAK begitu → band lokal, bukan langit/karakter.
     * Y tunable di bot_config.txt (posisi banner beda antar game/map).
     */
    private fun hasHookedBanner(frame: Bitmap): Boolean {
        val w = frame.width; val h = frame.height
        val x0 = (w * 0.25).toInt(); val x1 = (w * 0.75).toInt()
        val span = x1 - x0
        if (span < 16) return false
        val row = IntArray(span)

        fun overlayRatio(yFrac: Double): Double {
            val y = (h * yFrac).toInt().coerceIn(0, h - 1)
            frame.getPixels(row, 0, span, x0, y, span, 1)
            // "kena overlay" = brightness di rentang mid + variasi antar-piksel rendah
            var darkish = 0
            var i = 0
            while (i < span) {
                val c = row[i]
                val br = ((c ushr 16 and 0xFF) + (c ushr 8 and 0xFF) + (c and 0xFF)) / 3
                if (br in 30..160) darkish++
                i += 2
            }
            return darkish.toDouble() / (span / 2)
        }

        val yc = Config.hookedYPct
        val band = Config.hookedBandPct
        var hit = 0
        var yy = yc - band
        while (yy <= yc + band) {
            if (overlayRatio(yy) >= 0.72) hit++
            yy += band / 2
        }
        if (hit < 2) return false
        // kontrol: band harus LOKAL — jauh di atas DAN di bawahnya harus jelas bukan overlay
        // (dulu OR: cukup satu sisi cerah → siluet karakter / bibir langit lolos).
        val above = overlayRatio((yc - band * 3).coerceAtLeast(0.02))
        val below = overlayRatio((yc + band * 3).coerceAtMost(0.95))
        return above < 0.72 && below < 0.72
    }
}
