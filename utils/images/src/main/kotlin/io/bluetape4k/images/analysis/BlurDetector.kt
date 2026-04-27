package io.bluetape4k.images.analysis

import com.sksamuel.scrimage.ImmutableImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 이미지 블러 감지 결과.
 *
 * @property score Laplacian variance 점수. 클수록 선명하고 작을수록 흐림.
 * @property threshold 블러 판정 기준값.
 * @property isBlurry [score]가 [threshold]보다 작으면 true (흐린 이미지).
 */
data class BlurScore(
    val score: Double,
    val threshold: Double,
) {
    val isBlurry: Boolean get() = score < threshold

    companion object {
        /** blurScore() 기본 threshold 값 (~640x480 이미지 기준). 해상도에 따라 캘리브레이션 필요. */
        const val DEFAULT_THRESHOLD = 100.0
    }
}

/**
 * Laplacian variance 기반 블러 점수를 계산한다.
 *
 * Laplacian 커널 `[[0,1,0],[1,-4,1],[0,1,0]]`을 적용하고 variance를 계산한다.
 * score가 높을수록 선명하고, 낮을수록 흐린 이미지이다.
 *
 * **주의**: threshold=100.0은 ~640x480 기준. 고해상도 이미지는 더 큰 threshold 필요.
 * 같은 도메인 이미지 간 상대 비교용으로 사용 권장.
 * 큰 이미지 다수 처리 시 호출 전 다운샘플링 권장.
 *
 * @param threshold 블러 판정 기준값. 기본 [BlurScore.DEFAULT_THRESHOLD].
 * @throws IllegalArgumentException 이미지가 3x3보다 작을 경우.
 */
fun ImmutableImage.blurScore(threshold: Double = BlurScore.DEFAULT_THRESHOLD): BlurScore {
    require(width >= 3 && height >= 3) {
        "블러 감지를 위한 최소 이미지 크기는 3×3입니다. 현재: ${width}×${height}"
    }
    val score = computeLaplacianVariance(this)
    return BlurScore(score, threshold)
}

/**
 * 이미지가 흐린지 여부를 반환한다.
 *
 * @param threshold 블러 판정 기준값. 기본 [BlurScore.DEFAULT_THRESHOLD].
 */
fun ImmutableImage.isBlurry(threshold: Double = BlurScore.DEFAULT_THRESHOLD): Boolean =
    blurScore(threshold).isBlurry

/**
 * 블러 점수를 비동기로 계산한다.
 *
 * CPU-bound 연산이므로 [Dispatchers.Default]를 사용한다.
 */
suspend fun ImmutableImage.suspendBlurScore(threshold: Double = BlurScore.DEFAULT_THRESHOLD): BlurScore =
    withContext(Dispatchers.Default) { blurScore(threshold) }

/**
 * Laplacian variance를 계산하는 내부 함수.
 *
 * 1. grayscale 변환 (Rec. 601 luminance)
 * 2. Laplacian 3×3 convolution
 * 3. variance 계산 (boundary 1px 제외)
 */
internal fun computeLaplacianVariance(image: ImmutableImage): Double {
    val w = image.width
    val h = image.height
    val pixels = image.pixels()

    // grayscale 값 2D 배열 생성 (row-major)
    val gray = DoubleArray(w * h) { idx ->
        val p = pixels[idx]
        0.299 * p.red() + 0.587 * p.green() + 0.114 * p.blue()
    }

    // Laplacian convolution + Welford's online variance — boundary 1px 제외
    // mutableList<Double> 대신 single-pass로 heap 할당 없이 분산 계산
    var count = 0L
    var mean = 0.0
    var m2 = 0.0
    for (y in 1 until h - 1) {
        for (x in 1 until w - 1) {
            val center = gray[y * w + x]
            val v = gray[(y - 1) * w + x] + gray[(y + 1) * w + x] +
                    gray[y * w + (x - 1)] + gray[y * w + (x + 1)] - 4.0 * center
            count++
            val delta = v - mean
            mean += delta / count
            m2 += delta * (v - mean)
        }
    }
    return if (count > 0) m2 / count else 0.0
}
