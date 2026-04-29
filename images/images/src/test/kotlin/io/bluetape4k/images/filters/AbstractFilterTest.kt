package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Resourcex
import kotlin.math.abs
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Assertions.fail

abstract class AbstractFilterTest: AbstractImageTest() {

    companion object: KLoggingChannel() {
        const val FILTERS_DIR = "/images/filters/"
    }

    protected fun loadResourceImageBytes(imageName: String): ByteArray {
        return Resourcex.getBytes("$FILTERS_DIR/$imageName")
    }

    protected fun loadResourceImage(imageName: String): ImmutableImage {
        return immutableImageOf(loadResourceImageBytes(imageName))
    }

    /**
     * 두 이미지가 픽셀 단위로 유사한지 검증합니다.
     *
     * width, height가 같아야 하고, 모든 픽셀의 R/G/B 채널 절대 차이가 [tolerance] 이하여야 합니다.
     * 위반 픽셀이 발견되면 첫 번째 위반 좌표와 채널 값을 포함한 메시지로 실패합니다.
     *
     * @param actual 실제 이미지
     * @param expected 기대 이미지
     * @param tolerance 허용할 최대 채널 절대 차이 (기본값 2)
     */
    protected fun assertSimilarToImage(
        actual: ImmutableImage,
        expected: ImmutableImage,
        tolerance: Int = 2,
    ) {
        if (actual.width != expected.width || actual.height != expected.height) {
            fail<Unit>(
                "이미지 크기 불일치: actual=(${actual.width}x${actual.height}) " +
                    "expected=(${expected.width}x${expected.height})"
            )
        }

        val actualPixels = actual.pixels()
        val expectedPixels = expected.pixels()

        for (i in actualPixels.indices) {
            val a = actualPixels[i]
            val e = expectedPixels[i]
            val dr = abs(a.red() - e.red())
            val dg = abs(a.green() - e.green())
            val db = abs(a.blue() - e.blue())
            if (dr > tolerance || dg > tolerance || db > tolerance) {
                val x = i % actual.width
                val y = i / actual.width
                fail<Unit>(
                    "픽셀 ($x, $y) 에서 허용 오차($tolerance) 초과: " +
                        "actual=(${a.red()},${a.green()},${a.blue()}) " +
                        "expected=(${e.red()},${e.green()},${e.blue()}) " +
                        "delta=(dr=$dr, dg=$dg, db=$db)"
                )
            }
        }
    }

    /**
     * 두 이미지가 픽셀 단위로 충분히 다른지 검증합니다.
     *
     * 동일 크기인 경우, 어느 픽셀이든 R/G/B 채널 중 하나라도 [threshold] 초과이면 통과합니다.
     * 모든 픽셀이 [threshold] 이하이면 두 이미지가 너무 비슷한 것으로 간주하여 실패합니다.
     *
     * @param a 비교할 첫 번째 이미지
     * @param b 비교할 두 번째 이미지
     * @param threshold 차이가 있다고 판단하는 최소 채널 차이 (기본값 5)
     */
    protected fun assertNotSimilarToImage(
        a: ImmutableImage,
        b: ImmutableImage,
        threshold: Int = 5,
    ) {
        if (a.width != b.width || a.height != b.height) {
            // 크기가 다르면 명백히 다른 이미지이므로 통과
            return
        }

        val aPixels = a.pixels()
        val bPixels = b.pixels()

        for (i in aPixels.indices) {
            val pa = aPixels[i]
            val pb = bPixels[i]
            val dr = abs(pa.red() - pb.red())
            val dg = abs(pa.green() - pb.green())
            val db = abs(pa.blue() - pb.blue())
            if (dr > threshold || dg > threshold || db > threshold) {
                // 차이가 있는 픽셀 발견 — 통과
                return
            }
        }

        fail<Unit>("두 이미지가 threshold($threshold) 이하로 너무 비슷합니다. 다른 이미지여야 합니다.")
    }

    /**
     * 실제 이미지가 리소스 경로의 기대 이미지와 픽셀 단위로 유사한지 검증합니다.
     *
     * [resourceName]으로 기대 이미지를 로드한 후 [assertSimilarToImage]를 호출합니다.
     * JPEG 재인코딩 없이 직접 비교합니다.
     *
     * @param actual 검증할 실제 이미지
     * @param resourceName 비교 기준이 될 리소스 이미지 파일명
     * @param tolerance 허용할 최대 채널 절대 차이 (기본값 3)
     */
    protected fun assertSimilarToResource(
        actual: ImmutableImage,
        resourceName: String,
        tolerance: Int = 3,
    ) {
        val expected = loadResourceImage(resourceName)
        assertSimilarToImage(actual, expected, tolerance)
    }
}
