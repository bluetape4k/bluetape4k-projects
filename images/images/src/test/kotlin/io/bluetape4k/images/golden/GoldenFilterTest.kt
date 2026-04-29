package io.bluetape4k.images.golden

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.BlurFilter
import com.sksamuel.scrimage.filter.BrightnessFilter
import com.sksamuel.scrimage.filter.GrayscaleFilter
import com.sksamuel.scrimage.filter.SepiaFilter
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Resourcex
import org.junit.jupiter.api.Test

/**
 * 이미지 필터 적용 결과를 골든 이미지와 비교하는 테스트.
 *
 * 골든 이미지가 없으면 [org.opentest4j.TestAbortedException]으로 자동 skip됩니다.
 * 골든 이미지 생성: `-Dbluetape4k.images.golden.update=true` 시스템 프로퍼티로 실행하세요.
 */
class GoldenFilterTest {

    companion object : KLoggingChannel() {
        private const val HOMER_JPG = "/images/homer.jpg"
        private const val TOLERANCE = 5
    }

    private fun loadHomer(): ImmutableImage =
        immutableImageOf(Resourcex.getBytes(HOMER_JPG))

    /**
     * GrayscaleFilter 적용 결과가 골든 이미지와 일치해야 합니다.
     */
    @Test
    fun `filter grayscale matches golden`() {
        val result = loadHomer().filter(GrayscaleFilter())
        GoldenImageAssert.assertSimilarToGolden(result, "filter-grayscale", tolerance = TOLERANCE)
    }

    /**
     * SepiaFilter 적용 결과가 골든 이미지와 일치해야 합니다.
     */
    @Test
    fun `filter sepia matches golden`() {
        val result = loadHomer().filter(SepiaFilter())
        GoldenImageAssert.assertSimilarToGolden(result, "filter-sepia", tolerance = TOLERANCE)
    }

    /**
     * BrightnessFilter(1.3f) 적용 결과가 골든 이미지와 일치해야 합니다.
     */
    @Test
    fun `filter brightness 1_3 matches golden`() {
        val result = loadHomer().filter(BrightnessFilter(1.3f))
        GoldenImageAssert.assertSimilarToGolden(result, "filter-brightness", tolerance = TOLERANCE)
    }

    /**
     * BlurFilter 적용 결과가 골든 이미지와 일치해야 합니다.
     */
    @Test
    fun `filter blur matches golden`() {
        val result = loadHomer().filter(BlurFilter())
        GoldenImageAssert.assertSimilarToGolden(result, "filter-blur", tolerance = TOLERANCE)
    }
}
