package tf.fog.autoangler.vision

import android.graphics.Bitmap
import tf.fog.autoangler.util.Config
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class VisionResult(
    val yellowX: Int = 0,
    val greenLeft: Int = 0,
    val greenRight: Int = 0,
    val valid: Boolean = false
)

/**
 * Deteksi kursor kuning + zona hijau pada fishing bar, tahan-noise:
 *
 * - Zona hijau = RUN kontigu piksel hijau terbaik (bukan min/max SEMUA piksel hijau) —
 *   piksel hijau scenery yang TERPISAH dari run utama diabaikan, jadi `greenRight` tidak
 *   lagi ketarik ke ujung layar oleh pulau/tebing hijau di kanan.
 * - Kursor = gugus kuning tersempit & terpadat (bukan centroid semua piksel kuning).
 * - Pencarian dibatasi ke rentang "track" bar + jendela di sekitar hasil frame sebelumnya.
 * - Hasil di-clamp temporal (tak boleh lompat jauh antar-frame).
 * - Kalau tak masuk akal → `valid=false` (bot diam 1 frame, bukan dorong ngawur).
 *
 * `process(roi)` kompat lama (tanpa prev/track) tetap ada untuk pemanggilan sederhana.
 */
object VisionProcessor {
    private val hsv = FloatArray(3)

    fun process(roi: Bitmap): VisionResult = process(roi, VisionResult(), null)

    fun process(roi: Bitmap, prev: VisionResult, track: IntRange?): VisionResult {
        val w = roi.width
        val h = roi.height
        if (w < 8 || h < 2) return VisionResult()

        // Rentang x pencarian: track bar (kalau ada) dipotong ke lebar ROI.
        var x0 = track?.first?.coerceIn(0, w - 2) ?: (w * Config.barTrackLeftPct).toInt()
        var x1 = track?.last?.coerceIn(x0 + 1, w - 1) ?: (w * Config.barTrackRightPct).toInt()
        if (x1 - x0 < 8) { x0 = 0; x1 = w - 1 }
        // Abaikan ujung track (ring lingkaran Fish Stamina / Fishing Line = noise kuning/cyan).
        val endMargin = ((x1 - x0) * Config.trackEndMarginPct).toInt()
        x0 = (x0 + endMargin).coerceAtMost(w - 2)
        x1 = (x1 - endMargin).coerceAtLeast(x0 + 1)
        val trackW = x1 - x0

        // Pita scan vertikal tipis di tengah ROI (bar itu tipis).
        val cy = h / 2
        val bandHalf = max(1, h / 4)
        val y0 = (cy - bandHalf).coerceAtLeast(0)
        val y1 = (cy + bandHalf).coerceAtMost(h - 1)

        // Per-kolom: apakah kolom ini mengandung >=1 piksel kuning / hijau di pita scan.
        val greenCol = BooleanArray(w)
        val yellowCol = IntArray(w)          // jumlah piksel kuning per kolom (kepadatan)
        val rowPixels = IntArray(w)
        for (y in y0..y1) {
            roi.getPixels(rowPixels, 0, w, 0, y, w, 1)
            var x = x0
            while (x <= x1) {
                val c = rowPixels[x]
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                rgbToHsv(r, g, b, hsv)
                val hue = hsv[0]; val sat = hsv[1]; val v = hsv[2]
                if (isYellow(hue, sat, v)) yellowCol[x]++
                else if (isGreen(hue, sat, v)) greenCol[x] = true
                x++
            }
        }

        // ── Zona hijau: kumpulkan RUN kontigu (toleransi celah), pilih run terbaik ──────
        val runs = ArrayList<IntArray>()   // [start, end]
        run {
            var i = x0
            while (i <= x1) {
                if (!greenCol[i]) { i++; continue }
                var end = i
                var gap = 0
                var j = i + 1
                while (j <= x1 && gap <= Config.greenRunGapPx) {
                    if (greenCol[j]) { end = j; gap = 0 } else gap++
                    j++
                }
                runs.add(intArrayOf(i, end))
                i = end + 1
            }
        }
        val zoneMax = max(Config.zoneMinPx + 1, (Config.zoneMaxPct * trackW).toInt())
        val prevCenter = if (prev.valid) (prev.greenLeft + prev.greenRight) / 2 else -1
        val windowPx = (Config.zoneTrackWindowPct * trackW).toInt()

        var bestRun: IntArray? = null
        var bestScore = Int.MAX_VALUE
        for (rn in runs) {
            val wRun = rn[1] - rn[0]
            if (wRun < Config.zoneMinPx || wRun > zoneMax) continue
            // menyentuh tepi scan → curiga bleeding ke scenery/lingkaran ujung
            if (rn[0] <= x0 + 1 || rn[1] >= x1 - 1) continue
            val center = (rn[0] + rn[1]) / 2
            if (prevCenter >= 0 && windowPx > 0 && abs(center - prevCenter) > windowPx) continue
            val score = if (prevCenter >= 0) abs(center - prevCenter) else -(wRun) // no prev: run terlebar
            if (score < bestScore) { bestScore = score; bestRun = rn }
        }
        // fallback: kalau semua kena filter jendela tapi ada run wajar, ambil yang terlebar
        if (bestRun == null) {
            var widest = -1
            for (rn in runs) {
                val wRun = rn[1] - rn[0]
                if (wRun < Config.zoneMinPx || wRun > zoneMax) continue
                if (rn[0] <= x0 + 1 || rn[1] >= x1 - 1) continue
                if (wRun > widest) { widest = wRun; bestRun = rn }
            }
        }
        val gr = bestRun ?: return VisionResult()
        var greenL = gr[0]
        var greenR = gr[1]

        // ── Kursor kuning: kolom kuning PADAT (garis vertikal), HANYA di sekitar zona ─────
        // Kursor = garis kuning vertikal yang mengisi hampir seluruh tinggi pita-scan.
        // Noise (arc ring Fish Stamina, gradasi isi bar) cuma kuning di beberapa baris →
        // kepadatan kolom rendah → ditolak. Plus pencarian dibatasi ke sekitar zona hijau
        // digabung dgn sekitar kursor frame lalu → ring ujung & kuning scenery tak masuk.
        val bandRows = y1 - y0 + 1
        val searchHalf = (Config.cursorSearchPct * trackW).toInt().coerceAtLeast(40)
        val jumpW = (Config.cursorJumpMaxPct * trackW).toInt()
        val prevYx = if (prev.valid) prev.yellowX else -1
        var yx0 = greenL - searchHalf
        var yx1 = greenR + searchHalf
        if (prevYx >= 0 && jumpW > 0) {
            yx0 = min(yx0, prevYx - jumpW - 8)
            yx1 = max(yx1, prevYx + jumpW + 8)
        }
        yx0 = yx0.coerceAtLeast(x0)
        yx1 = yx1.coerceAtMost(x1)
        val densHi = max(2, (bandRows * Config.cursorMinDensityFrac).toInt())
        val densLo = max(1, (bandRows * Config.cursorMinDensityFrac * 0.5).toInt())

        fun collectClusters(minCount: Int): ArrayList<IntArray> {
            val out = ArrayList<IntArray>()
            var i = yx0
            while (i <= yx1) {
                if (yellowCol[i] < minCount) { i++; continue }
                var end = i
                var j = i + 1
                while (j <= yx1 && yellowCol[j] >= minCount && (j - end) <= 2) { end = j; j++ }
                if (end - i <= Config.cursorMaxWidthPx) out.add(intArrayOf(i, end))
                i = end + 1
            }
            return out
        }
        var clusters = collectClusters(densHi)
        if (clusters.isEmpty()) clusters = collectClusters(densLo)   // longgar (kursor tepi zona / motion blur)
        if (clusters.isEmpty()) return VisionResult()

        val prevY = if (prev.valid) prev.yellowX else -1
        var bestC: IntArray = clusters[0]
        var bestC2 = Int.MAX_VALUE
        for (cl in clusters) {
            val center = (cl[0] + cl[1]) / 2
            val score = when {
                prevY >= 0 -> abs(center - prevY)
                else -> abs(center - (greenL + greenR) / 2)  // no prev: paling dekat zona
            }
            if (score < bestC2) { bestC2 = score; bestC = cl }
        }
        var yellowX = (bestC[0] + bestC[1]) / 2

        // ── Clamp temporal ───────────────────────────────────────────────────────────
        if (prev.valid && jumpW > 0) {
            yellowX = yellowX.coerceIn(prev.yellowX - jumpW, prev.yellowX + jumpW)
            greenL = greenL.coerceIn(prev.greenLeft - jumpW, prev.greenLeft + jumpW)
            greenR = greenR.coerceIn(prev.greenRight - jumpW, prev.greenRight + jumpW)
        }
        if (greenR - greenL < Config.zoneMinPx) return VisionResult()

        return VisionResult(
            yellowX = yellowX.coerceIn(0, w - 1),
            greenLeft = min(greenL, greenR),
            greenRight = max(greenL, greenR),
            valid = true
        )
    }

    private fun isYellow(h: Float, s: Float, v: Float) =
        h >= Config.yellowHMin && h <= Config.yellowHMax &&
            s >= Config.yellowSMin && v >= Config.yellowVMin

    private fun isGreen(h: Float, s: Float, v: Float) =
        h >= Config.greenHMin && h <= Config.greenHMax &&
            s >= Config.greenSMin && v >= Config.greenVMin

    private fun rgbToHsv(r: Int, g: Int, b: Int, out: FloatArray) {
        val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
        val mx = maxOf(rf, gf, bf); val mn = minOf(rf, gf, bf)
        val d = mx - mn
        out[2] = mx
        out[1] = if (mx > 0f) d / mx else 0f
        if (d == 0f) { out[0] = 0f; return }
        var hue = when (mx) {
            rf -> 60f * (((gf - bf) / d) % 6f)
            gf -> 60f * (((bf - rf) / d) + 2f)
            else -> 60f * (((rf - gf) / d) + 4f)
        }
        if (hue < 0f) hue += 360f
        out[0] = hue
    }
}
