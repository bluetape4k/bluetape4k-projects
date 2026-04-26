package io.bluetape4k.images.similarity

import com.sksamuel.scrimage.ImmutableImage

/**
 * 히스토그램 색공간 선택.
 *
 * - [RGB]: 일반적인 사진/그림 비교에 적합. 채널 간 독립.
 * - [HSV]: 조명 변화에 더 강건. 색상(H)/채도(S)/명도(V) 분리 비교.
 */
enum class ColorSpace { RGB, HSV }

/**
 * 이미지의 정규화된 색상 히스토그램을 생성합니다.
 *
 * 채널별로 [binsPerChannel] 개 구간을 만들고, 각 채널의 합이 1.0이 되도록 정규화합니다.
 * 이미지 크기/종횡비가 달라도 정규화된 히스토그램은 동일한 분포를 표현하므로 비교 가능합니다.
 *
 * @param img 대상 이미지
 * @param colorSpace 색공간 ([ColorSpace.RGB] 또는 [ColorSpace.HSV])
 * @param binsPerChannel 채널별 bin 개수. 2..256 범위
 * @return Array<DoubleArray> — `[channel][bin]`. 각 채널 합 = 1.0. zero-histogram이면 그대로 0 유지(정규화 안 함).
 */
internal fun buildHistogram(
    img: ImmutableImage,
    colorSpace: ColorSpace,
    binsPerChannel: Int,
): Array<DoubleArray> {
    val channelCount = 3
    val hist = Array(channelCount) { DoubleArray(binsPerChannel) }

    for (pixel in img.pixels()) {
        val (c0, c1, c2) = when (colorSpace) {
            ColorSpace.RGB -> Triple(pixel.red() / 256.0, pixel.green() / 256.0, pixel.blue() / 256.0)
            ColorSpace.HSV -> {
                val (h, s, v) = hsvComponents(pixel)
                Triple(h.toDouble(), s.toDouble(), v.toDouble())
            }
        }
        hist[0][(c0 * binsPerChannel).toInt().coerceIn(0, binsPerChannel - 1)]++
        hist[1][(c1 * binsPerChannel).toInt().coerceIn(0, binsPerChannel - 1)]++
        hist[2][(c2 * binsPerChannel).toInt().coerceIn(0, binsPerChannel - 1)]++
    }

    // 채널별 정규화 (sum → 1.0). zero-histogram이면 그대로 유지.
    for (ch in 0 until channelCount) {
        val sum = hist[ch].sum()
        if (sum > 0.0) for (b in 0 until binsPerChannel) hist[ch][b] /= sum
    }
    return hist
}

/**
 * 컬러 히스토그램 기반 유사도 측정 전략.
 *
 * 크기·종횡비가 다른 이미지 비교 가능 — 정규화된 히스토그램(sum=1.0)으로 비교합니다.
 * 반환값은 모두 `[0.0, 1.0]`, **클수록 유사** (완전 동일 = 1.0).
 *
 * ## 제공 측정 방식
 *
 * | 전략 | 의미 | 특징 |
 * |---|---|---|
 * | [ChiSquare]     | Chi-Square 거리 → exp(-d/2) | 분포 차이에 민감 |
 * | [Bhattacharyya] | Bhattacharyya 계수          | 채널 평균, 직관적 [0,1] |
 * | [EarthMover]    | 1D Earth Mover's Distance   | 순서/이동량에 민감 |
 *
 * ## zero-histogram 처리
 *
 * 두 이미지 모두 단색/비어있어 히스토그램이 0인 경우 `1.0` 반환 (동일 이미지 간주).
 *
 * ## 사용 예
 *
 * ```kotlin
 * val a = immutableImageOf(File("a.jpg"))
 * val b = immutableImageOf(File("b.jpg"))
 *
 * a.histogramSimilarityTo(b)                                       // 기본: ChiSquare RGB 32 bins
 * a.histogramSimilarityTo(b, HistogramSimilarity.bhattacharyya())  // Bhattacharyya
 * a.histogramSimilarityTo(b, HistogramSimilarity.earthMover(ColorSpace.HSV, bins = 64))
 * ```
 */
sealed interface HistogramSimilarity {

    /**
     * 두 이미지의 히스토그램 기반 유사도를 측정합니다.
     *
     * @return `[0.0, 1.0]`. 클수록 유사. 완전 동일 = 1.0.
     */
    fun measure(a: ImmutableImage, b: ImmutableImage): Double

    /**
     * Chi-Square 거리 기반 유사도.
     *
     * `d = sum((p-q)² / (p+q+ε))` → `similarity = exp(-d/2)`
     *
     * 분포 차이에 민감하여 작은 색상 변화도 잘 잡아냅니다.
     *
     * @property colorSpace 색공간
     * @property binsPerChannel 채널별 bin 개수 (2..256)
     */
    data class ChiSquare(
        val colorSpace: ColorSpace = ColorSpace.RGB,
        val binsPerChannel: Int = 32,
    ): HistogramSimilarity {
        init {
            require(binsPerChannel in 2..256) { "binsPerChannel 범위: 2..256, 입력: $binsPerChannel" }
        }

        override fun measure(a: ImmutableImage, b: ImmutableImage): Double {
            val ha = buildHistogram(a, colorSpace, binsPerChannel)
            val hb = buildHistogram(b, colorSpace, binsPerChannel)
            if (ha.isZero() && hb.isZero()) return 1.0
            var d = 0.0
            val eps = 1e-10
            for (ch in ha.indices) {
                for (bin in 0 until binsPerChannel) {
                    val p = ha[ch][bin]
                    val q = hb[ch][bin]
                    d += (p - q) * (p - q) / (p + q + eps)
                }
            }
            return kotlin.math.exp(-d / 2.0)
        }
    }

    /**
     * Bhattacharyya 계수 기반 유사도.
     *
     * `similarity = sum(sqrt(p*q))` ∈ `[0,1]`. 완전 동일 = 1.0.
     * 채널별 계수의 평균을 반환하여 [0,1] 범위를 유지합니다.
     *
     * @property colorSpace 색공간
     * @property binsPerChannel 채널별 bin 개수 (2..256)
     */
    data class Bhattacharyya(
        val colorSpace: ColorSpace = ColorSpace.RGB,
        val binsPerChannel: Int = 32,
    ): HistogramSimilarity {
        init {
            require(binsPerChannel in 2..256) { "binsPerChannel 범위: 2..256, 입력: $binsPerChannel" }
        }

        override fun measure(a: ImmutableImage, b: ImmutableImage): Double {
            val ha = buildHistogram(a, colorSpace, binsPerChannel)
            val hb = buildHistogram(b, colorSpace, binsPerChannel)
            if (ha.isZero() && hb.isZero()) return 1.0
            var coeff = 0.0
            for (ch in ha.indices) {
                for (bin in 0 until binsPerChannel) {
                    coeff += kotlin.math.sqrt(ha[ch][bin] * hb[ch][bin])
                }
            }
            return coeff / ha.size  // 채널 수로 나눠 [0,1] 유지
        }
    }

    /**
     * 1D Earth Mover's Distance (CDF 차이의 합) 기반 유사도.
     *
     * `similarity = 1 - emd/dMax`, `dMax = channels * (binsPerChannel - 1)`.
     *
     * 순서에 민감 — H 채널 wrap-around 미지원. RGB 또는 HSV의 S/V 채널 사용을 권장.
     *
     * @property colorSpace 색공간
     * @property binsPerChannel 채널별 bin 개수 (2..256)
     */
    data class EarthMover(
        val colorSpace: ColorSpace = ColorSpace.RGB,
        val binsPerChannel: Int = 32,
    ): HistogramSimilarity {
        init {
            require(binsPerChannel in 2..256) { "binsPerChannel 범위: 2..256, 입력: $binsPerChannel" }
        }

        override fun measure(a: ImmutableImage, b: ImmutableImage): Double {
            val ha = buildHistogram(a, colorSpace, binsPerChannel)
            val hb = buildHistogram(b, colorSpace, binsPerChannel)
            if (ha.isZero() && hb.isZero()) return 1.0
            var emd = 0.0
            for (ch in ha.indices) {
                var flow = 0.0
                for (bin in 0 until binsPerChannel) {
                    flow += ha[ch][bin] - hb[ch][bin]
                    emd += kotlin.math.abs(flow)
                }
            }
            val dMax = ha.size.toDouble() * (binsPerChannel - 1)
            return 1.0 - (emd / dMax).coerceIn(0.0, 1.0)
        }
    }

    companion object {
        /** Chi-Square 측정 전략 단축 생성자. */
        fun chiSquare(colorSpace: ColorSpace = ColorSpace.RGB, bins: Int = 32): HistogramSimilarity =
            ChiSquare(colorSpace, bins)

        /** Bhattacharyya 측정 전략 단축 생성자. */
        fun bhattacharyya(colorSpace: ColorSpace = ColorSpace.RGB, bins: Int = 32): HistogramSimilarity =
            Bhattacharyya(colorSpace, bins)

        /** Earth Mover's Distance 측정 전략 단축 생성자. */
        fun earthMover(colorSpace: ColorSpace = ColorSpace.RGB, bins: Int = 32): HistogramSimilarity =
            EarthMover(colorSpace, bins)
    }
}

/** 히스토그램이 모두 0인지 확인 (zero-histogram 판정). */
private fun Array<DoubleArray>.isZero(): Boolean = all { ch -> ch.all { it == 0.0 } }

/**
 * Chi-Square 히스토그램 유사도를 바로 계산합니다. [HistogramSimilarity.chiSquare] 단축형.
 *
 * 크기·종횡비가 달라도 비교 가능하며 반환값은 `[0.0, 1.0]`, 클수록 유사.
 *
 * @param other 비교 대상 이미지
 * @param measure 측정 전략. 기본 [HistogramSimilarity.ChiSquare] (RGB, 32 bins).
 * @return `[0.0, 1.0]` 유사도. 완전 동일 = 1.0.
 */
fun ImmutableImage.histogramSimilarityTo(
    other: ImmutableImage,
    measure: HistogramSimilarity = HistogramSimilarity.chiSquare(),
): Double = measure.measure(this, other)
