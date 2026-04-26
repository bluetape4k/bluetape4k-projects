package io.bluetape4k.images.filters.dsl

import java.awt.Color
import kotlin.math.ln
import kotlin.math.pow

/**
 * RGB ↔ HSV / YCbCr / Kelvin 변환 유틸리티.
 *
 * HSV: H∈[0,360), S∈[0,1], V∈[0,1]
 * YCbCr: BT.601 표준
 * Kelvin→RGB: Tanner Helland 알고리즘, 범위 [KELVIN_MIN, KELVIN_MAX]에서 클램프
 *
 * ```kotlin
 * val (h, s, v) = ColorSpaceConverter.rgbToHsv(255, 128, 0)
 * val (r, g, b) = ColorSpaceConverter.hsvToRgb(30f, 1f, 1f)
 * ```
 */
object ColorSpaceConverter {

    /** Tanner Helland 알고리즘 적용 최소 켈빈 값. */
    const val KELVIN_MIN: Int = 1000

    /** Tanner Helland 알고리즘 적용 최대 켈빈 값. */
    const val KELVIN_MAX: Int = 40000

    /**
     * RGB 색상을 HSV 색공간으로 변환합니다.
     *
     * @param r Red 채널 값 (0..255)
     * @param g Green 채널 값 (0..255)
     * @param b Blue 채널 값 (0..255)
     * @return Triple(H∈[0,360), S∈[0,1], V∈[0,1])
     */
    fun rgbToHsv(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val hsb = Color.RGBtoHSB(r, g, b, null)
        return Triple(hsb[0] * 360f, hsb[1], hsb[2])
    }

    /**
     * HSV 색공간 값을 RGB로 변환합니다.
     *
     * @param h Hue 값 (0..360)
     * @param s Saturation 값 (0..1)
     * @param v Value 값 (0..1)
     * @return Triple(R, G, B) 각각 0..255 범위
     */
    fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Int, Int, Int> {
        val argb = Color.HSBtoRGB(h / 360f, s, v)
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return Triple(r, g, b)
    }

    /**
     * RGB 색상을 YCbCr 색공간(BT.601)으로 변환합니다.
     *
     * @param r Red 채널 값 (0..255)
     * @param g Green 채널 값 (0..255)
     * @param b Blue 채널 값 (0..255)
     * @return Triple(Y, Cb, Cr) BT.601 기준
     */
    fun rgbToYCbCr(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rf = r.toFloat()
        val gf = g.toFloat()
        val bf = b.toFloat()
        val y = 0.257f * rf + 0.504f * gf + 0.098f * bf + 16f
        val cb = -0.148f * rf - 0.291f * gf + 0.439f * bf + 128f
        val cr = 0.439f * rf - 0.368f * gf - 0.071f * bf + 128f
        return Triple(y, cb, cr)
    }

    /**
     * YCbCr 색공간(BT.601) 값을 RGB로 변환합니다.
     *
     * @param y Y(휘도) 값
     * @param cb Cb(청색 색차) 값
     * @param cr Cr(적색 색차) 값
     * @return Triple(R, G, B) 각각 0..255 범위로 클램프
     */
    fun yCbCrToRgb(y: Float, cb: Float, cr: Float): Triple<Int, Int, Int> {
        val y2 = y - 16f
        val cb2 = cb - 128f
        val cr2 = cr - 128f
        val r = (1.164f * y2 + 1.596f * cr2).toInt().coerceIn(0, 255)
        val g = (1.164f * y2 - 0.392f * cb2 - 0.813f * cr2).toInt().coerceIn(0, 255)
        val b = (1.164f * y2 + 2.017f * cb2).toInt().coerceIn(0, 255)
        return Triple(r, g, b)
    }

    /**
     * 색온도(켈빈)를 RGB 색상으로 변환합니다.
     * Tanner Helland 알고리즘을 사용하며 입력을 [KELVIN_MIN]..[KELVIN_MAX] 범위로 클램프합니다.
     *
     * @param kelvin 색온도 (켈빈 단위)
     * @return Triple(R, G, B) 각각 0..255 범위
     */
    fun kelvinToRgb(kelvin: Int): Triple<Int, Int, Int> {
        val out = IntArray(3)
        kelvinToRgbInto(kelvin, out)
        return Triple(out[0], out[1], out[2])
    }

    /**
     * RGB를 HSV로 변환한 결과를 [out] 배열에 직접 씁니다. (박싱 회피용)
     *
     * @param r Red 채널 값 (0..255)
     * @param g Green 채널 값 (0..255)
     * @param b Blue 채널 값 (0..255)
     * @param out 결과를 저장할 FloatArray (크기 >= 3). out[0]=H, out[1]=S, out[2]=V
     */
    @JvmSynthetic
    internal fun rgbToHsvInto(r: Int, g: Int, b: Int, out: FloatArray) {
        val hsb = Color.RGBtoHSB(r, g, b, null)
        out[0] = hsb[0] * 360f
        out[1] = hsb[1]
        out[2] = hsb[2]
    }

    /**
     * HSV를 RGB로 변환한 결과를 [out] 배열에 직접 씁니다. (박싱 회피용)
     *
     * @param h Hue 값 (0..360)
     * @param s Saturation 값 (0..1)
     * @param v Value 값 (0..1)
     * @param out 결과를 저장할 IntArray (크기 >= 3). out[0]=R, out[1]=G, out[2]=B
     */
    @JvmSynthetic
    internal fun hsvToRgbInto(h: Float, s: Float, v: Float, out: IntArray) {
        val argb = Color.HSBtoRGB(h / 360f, s, v)
        out[0] = (argb shr 16) and 0xFF
        out[1] = (argb shr 8) and 0xFF
        out[2] = argb and 0xFF
    }

    /**
     * 색온도(켈빈)를 RGB로 변환한 결과를 [out] 배열에 직접 씁니다. (박싱 회피용)
     * 입력을 [KELVIN_MIN]..[KELVIN_MAX] 범위로 클램프합니다.
     *
     * @param kelvin 색온도 (켈빈 단위)
     * @param out 결과를 저장할 IntArray (크기 >= 3). out[0]=R, out[1]=G, out[2]=B
     */
    /**
     * RGB를 YCbCr(BT.601)로 변환한 결과를 [out] 배열에 직접 씁니다. (박싱 회피용)
     *
     * @param r Red 채널 값 (0..255)
     * @param g Green 채널 값 (0..255)
     * @param b Blue 채널 값 (0..255)
     * @param out 결과를 저장할 FloatArray (크기 >= 3). out[0]=Y, out[1]=Cb, out[2]=Cr
     */
    @JvmSynthetic
    internal fun rgbToYCbCrInto(r: Int, g: Int, b: Int, out: FloatArray) {
        val rf = r.toFloat(); val gf = g.toFloat(); val bf = b.toFloat()
        out[0] = 0.257f * rf + 0.504f * gf + 0.098f * bf + 16f
        out[1] = -0.148f * rf - 0.291f * gf + 0.439f * bf + 128f
        out[2] = 0.439f * rf - 0.368f * gf - 0.071f * bf + 128f
    }

    @JvmSynthetic
    internal fun kelvinToRgbInto(kelvin: Int, out: IntArray) {
        val k = kelvin.coerceIn(KELVIN_MIN, KELVIN_MAX)
        val temp = k / 100.0

        val r: Int = if (temp <= 66) {
            255
        } else {
            (329.698727446 * (temp - 60).pow(-0.1332047592)).toInt().coerceIn(0, 255)
        }

        val g: Int = if (temp <= 66) {
            (99.4708025861 * ln(temp) - 161.1195681661).toInt().coerceIn(0, 255)
        } else {
            (288.1221695283 * (temp - 60).pow(-0.0755148492)).toInt().coerceIn(0, 255)
        }

        val b: Int = when {
            temp >= 66 -> 255
            temp <= 19 -> 0
            else -> (138.5177312231 * ln(temp - 10) - 305.0447927307).toInt().coerceIn(0, 255)
        }

        out[0] = r
        out[1] = g
        out[2] = b
    }
}
