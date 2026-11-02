package com.x0550.rubiccubesolver.data.analyzer

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.x0550.rubiccubesolver.domain.model.CubeColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import android.graphics.Color as AndroidColor

@Singleton
class CubeFrameAnalyzer @Inject constructor() : ImageAnalysis.Analyzer {

    private val _detectedColors = MutableStateFlow<List<CubeColor>>(List(9) { CubeColor.UNKNOWN })
    val detectedColors: StateFlow<List<CubeColor>> = _detectedColors.asStateFlow()

    /** On-screen guide box, in preview-view pixel coordinates. */
    private data class Guide(val viewW: Float, val viewH: Float, val left: Float, val top: Float, val size: Float)

    @Volatile
    private var guide: Guide? = null

    /** Color the center sticker is known to be for the face currently being scanned. */
    @Volatile
    private var expectedCenter: CubeColor? = null

    /** Latest per-sticker Lab samples, used to calibrate references on capture. */
    @Volatile
    private var lastSamples: List<FloatArray?> = List(9) { null }

    /** Reference Lab color per cube color; starts from typical camera values and adapts to real stickers. */
    private val references: MutableMap<CubeColor, FloatArray> = DEFAULT_REFERENCES
        .mapValues { it.value.copyOf() }
        .toMutableMap()

    private val history = ArrayDeque<List<CubeColor>>()

    /** Tell the analyzer where the 3x3 guide box is drawn on top of the (FILL_CENTER) preview. */
    fun setGuideRegion(viewWidth: Float, viewHeight: Float, left: Float, top: Float, size: Float) {
        if (viewWidth > 0f && viewHeight > 0f && size > 0f) {
            guide = Guide(viewWidth, viewHeight, left, top, size)
        }
    }

    fun setExpectedCenter(color: CubeColor?) {
        expectedCenter = color
        synchronized(history) { history.clear() }
    }

    /** Lock in the current center sticker as the true reference for [color]; call when the user captures a face. */
    fun calibrateFromCenter(color: CubeColor) {
        val center = lastSamples[4] ?: return
        synchronized(references) {
            if (isSafeCalibration(center, color)) references[color] = center.copyOf()
        }
    }

    fun resetCalibration() {
        synchronized(references) {
            DEFAULT_REFERENCES.forEach { (k, v) -> references[k] = v.copyOf() }
        }
        synchronized(history) { history.clear() }
    }

    /**
     * Only accept a sample as the new reference for [color] if it is still clearly that color: it must not have
     * drifted far from the default, and must remain closer to [color] than to every other reference by a margin.
     * Without this, e.g. a dim bluish white could become the white reference and then swallow blue stickers.
     */
    private fun isSafeCalibration(sample: FloatArray, color: CubeColor): Boolean {
        if (distance(sample, DEFAULT_REFERENCES.getValue(color), color) > MAX_CALIBRATION_DRIFT) return false
        val own = distance(sample, references.getValue(color), color)
        return references.none { (other, ref) ->
            other != color && distance(sample, ref, other) < own + CALIBRATION_MARGIN
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
        try {
            val g = guide ?: return
            val bitmap = imageProxy.toBitmap()
            val samples = sampleStickers(bitmap, imageProxy.imageInfo.rotationDegrees, g)
            lastSamples = samples

            // Live-adapt the expected center's reference while it clearly looks like that color.
            val expected = expectedCenter
            val center = samples[4]
            if (expected != null && center != null) {
                synchronized(references) {
                    if (isSafeCalibration(center, expected)) {
                        val ref = references.getValue(expected)
                        for (i in 0..2) ref[i] += (center[i] - ref[i]) * LIVE_CALIBRATION_RATE
                    }
                }
            }

            val colors = samples.map { lab -> if (lab == null) CubeColor.UNKNOWN else classify(lab) }
            _detectedColors.value = smooth(colors)
        } finally {
            imageProxy.close()
        }
    }

    /** Returns a robust (median) Lab color per sticker, or null if the sticker could not be sampled. */
    private fun sampleStickers(bitmap: Bitmap, rotation: Int, g: Guide): List<FloatArray?> {
        val geometry = samplingGeometry(bitmap, rotation, g)
        return List(9) { index -> sampleSticker(bitmap, geometry, index) }
    }

    private data class BitmapSize(val width: Int, val height: Int)

    private data class UprightSize(val width: Int, val height: Int)

    private data class PreviewMapping(val scale: Float, val offsetX: Float, val offsetY: Float)

    private data class StickerGrid(val left: Float, val top: Float, val cellSize: Float, val halfSampleSize: Float)

    private data class SamplingGeometry(
        val bitmapSize: BitmapSize,
        val uprightSize: UprightSize,
        val previewMapping: PreviewMapping,
        val stickerGrid: StickerGrid,
        val rotation: Int
    )

    private data class BitmapPoint(val x: Int, val y: Int)

    private data class SampleCenter(val x: Float, val y: Float)

    private fun samplingGeometry(bitmap: Bitmap, rotation: Int, guide: Guide): SamplingGeometry {
        val bitmapSize = BitmapSize(bitmap.width, bitmap.height)
        val rotated = rotation == 90 || rotation == 270
        val uprightSize = if (rotated) UprightSize(bitmap.height, bitmap.width)
        else UprightSize(bitmap.width, bitmap.height)
        val scale = max(guide.viewW / uprightSize.width, guide.viewH / uprightSize.height)
        val mapping = PreviewMapping(
            scale = scale,
            offsetX = (guide.viewW - uprightSize.width * scale) / 2f,
            offsetY = (guide.viewH - uprightSize.height * scale) / 2f
        )
        val cellSize = guide.size / 3f
        val grid = StickerGrid(
            left = guide.left,
            top = guide.top,
            cellSize = cellSize,
            halfSampleSize = cellSize * INNER_FRACTION / 2f
        )
        return SamplingGeometry(bitmapSize, uprightSize, mapping, grid, rotation)
    }

    private fun sampleSticker(bitmap: Bitmap, geometry: SamplingGeometry, index: Int): FloatArray? {
        val center = stickerCenter(geometry.stickerGrid, index)
        val lightness = ArrayList<Float>(SAMPLES * SAMPLES)
        val aValues = ArrayList<Float>(SAMPLES * SAMPLES)
        val bValues = ArrayList<Float>(SAMPLES * SAMPLES)

        for (row in 0 until SAMPLES) {
            for (column in 0 until SAMPLES) {
                val sample = samplePixel(bitmap, geometry, center, row, column)
                if (sample != null && sample[0] >= MIN_LIGHTNESS) {
                    lightness += sample[0]
                    aValues += sample[1]
                    bValues += sample[2]
                }
            }
        }

        return if (lightness.size < MIN_VALID_SAMPLES) null
        else floatArrayOf(lightness.median(), aValues.median(), bValues.median())
    }

    private fun stickerCenter(grid: StickerGrid, index: Int) = SampleCenter(
        grid.left + index % 3 * grid.cellSize + grid.cellSize / 2f,
        grid.top + index / 3 * grid.cellSize + grid.cellSize / 2f
    )

    private fun samplePixel(
        bitmap: Bitmap,
        geometry: SamplingGeometry,
        center: SampleCenter,
        row: Int,
        column: Int
    ): FloatArray? {
        val grid = geometry.stickerGrid
        val x = center.x - grid.halfSampleSize + 2f * grid.halfSampleSize * row / (SAMPLES - 1)
        val y = center.y - grid.halfSampleSize + 2f * grid.halfSampleSize * column / (SAMPLES - 1)
        val mapping = geometry.previewMapping
        val uprightX = ((x - mapping.offsetX) / mapping.scale).toInt()
        val uprightY = ((y - mapping.offsetY) / mapping.scale).toInt()
        val point = bitmapPoint(geometry, uprightX, uprightY) ?: return null
        return rgbToLab(bitmap.getPixel(point.x, point.y))
    }

    private fun bitmapPoint(geometry: SamplingGeometry, x: Int, y: Int): BitmapPoint? {
        val upright = geometry.uprightSize
        if (x !in 0 until upright.width || y !in 0 until upright.height) return null
        val bitmap = geometry.bitmapSize
        val point = when (geometry.rotation) {
            90 -> BitmapPoint(y, bitmap.height - 1 - x)
            180 -> BitmapPoint(bitmap.width - 1 - x, bitmap.height - 1 - y)
            270 -> BitmapPoint(bitmap.width - 1 - y, x)
            else -> BitmapPoint(x, y)
        }
        return point.takeIf { it.x in 0 until bitmap.width && it.y in 0 until bitmap.height }
    }

    private fun classify(lab: FloatArray): CubeColor {
        val refs = synchronized(references) { references.mapValues { it.value.copyOf() } }
        return refs.minByOrNull { distance(lab, it.value, it.key) }?.key ?: CubeColor.UNKNOWN
    }

    /**
     * LCh-style distance: hue-angle difference (scaled by chroma so it vanishes for greys) dominates,
     * lightness is down-weighted so shadows/glare don't flip orange into red. Against WHITE, lightness
     * weighs more, since brightness is what separates white from a desaturated blue.
     */
    private fun distance(a: FloatArray, b: FloatArray, refColor: CubeColor): Float {
        val ca = sqrt(a[1] * a[1] + a[2] * a[2])
        val cb = sqrt(b[1] * b[1] + b[2] * b[2])
        var dh = abs(atan2(a[2], a[1]) - atan2(b[2], b[1]))
        if (dh > PI.toFloat()) dh = 2f * PI.toFloat() - dh
        val hueTerm = 2f * sqrt(ca * cb) * sin(dh / 2f) * HUE_WEIGHT
        val dl = (a[0] - b[0]) * if (refColor == CubeColor.WHITE) WHITE_L_WEIGHT else L_WEIGHT
        val dc = (ca - cb) * CHROMA_WEIGHT
        return sqrt(dl * dl + dc * dc + hueTerm * hueTerm)
    }

    /** Vote over the last few frames per sticker to remove flicker. */
    private fun smooth(frame: List<CubeColor>): List<CubeColor> = synchronized(history) {
        history.addLast(frame)
        while (history.size > HISTORY_SIZE) history.removeFirst()
        List(9) { idx ->
            history.groupingBy { it[idx] }.eachCount().maxByOrNull { it.value }?.key ?: CubeColor.UNKNOWN
        }
    }

    private fun List<Float>.median(): Float {
        val s = sorted()
        return s[s.size / 2]
    }

    private companion object {
        const val HISTORY_SIZE = 5
        const val SAMPLES = 7
        const val INNER_FRACTION = 0.5f
        const val MIN_LIGHTNESS = 12f
        const val MIN_VALID_SAMPLES = 8
        const val L_WEIGHT = 0.35f
        const val WHITE_L_WEIGHT = 0.8f
        const val CHROMA_WEIGHT = 1.0f
        const val HUE_WEIGHT = 1.6f
        const val LIVE_CALIBRATION_RATE = 0.1f
        const val MAX_CALIBRATION_DRIFT = 30f
        const val CALIBRATION_MARGIN = 8f

        fun rgbToLab(pixel: Int): FloatArray {
            fun lin(c: Int): Double {
                val v = c / 255.0
                return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
            }
            val r = lin(AndroidColor.red(pixel))
            val g = lin(AndroidColor.green(pixel))
            val b = lin(AndroidColor.blue(pixel))
            val x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047
            val y = r * 0.2126 + g * 0.7152 + b * 0.0722
            val z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883
            fun f(t: Double) = if (t > 0.008856) cbrt(t) else 7.787 * t + 16.0 / 116.0
            val fx = f(x); val fy = f(y); val fz = f(z)
            return floatArrayOf(
                (116 * fy - 16).toFloat(),
                (500 * (fx - fy)).toFloat(),
                (200 * (fy - fz)).toFloat()
            )
        }

        /** Typical phone-camera captures of standard sticker colors. */
        val DEFAULT_REFERENCES: Map<CubeColor, FloatArray> = mapOf(
            CubeColor.WHITE to rgbToLab(AndroidColor.rgb(215, 220, 220)),
            CubeColor.YELLOW to rgbToLab(AndroidColor.rgb(225, 215, 45)),
            CubeColor.GREEN to rgbToLab(AndroidColor.rgb(30, 165, 80)),
            CubeColor.BLUE to rgbToLab(AndroidColor.rgb(25, 85, 185)),
            CubeColor.RED to rgbToLab(AndroidColor.rgb(175, 25, 35)),
            CubeColor.ORANGE to rgbToLab(AndroidColor.rgb(240, 95, 40))
        )
    }
}