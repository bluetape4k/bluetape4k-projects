package io.bluetape4k.images.golden

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Resourcex
import org.junit.jupiter.api.Test

/**
 * 이미지 resize 연산 결과를 골든 이미지와 비교하는 테스트.
 *
 * 골든 이미지가 없으면 [org.opentest4j.TestAbortedException]으로 자동 skip됩니다.
 * 골든 이미지 생성: `-Dbluetape4k.images.golden.update=true` 시스템 프로퍼티로 실행하세요.
 */
class GoldenResizeTest {

    companion object : KLoggingChannel() {
        private const val HOMER_JPG = "/images/homer.jpg"
    }

    private fun loadHomer(): ImmutableImage =
        immutableImageOf(Resourcex.getBytes(HOMER_JPG))

    /**
     * scaleTo(320, 240) 결과가 골든 이미지와 일치해야 합니다.
     */
    @Test
    fun `resize scaleTo 320x240 matches golden`() {
        val result = loadHomer().scaleTo(320, 240)
        GoldenImageAssert.assertSimilarToGolden(result, "resize-320x240", tolerance = 3)
    }

    /**
     * scaleTo(128, 128) 결과가 골든 이미지와 일치해야 합니다.
     */
    @Test
    fun `resize thumbnail 128x128 matches golden`() {
        val result = loadHomer().scaleTo(128, 128)
        GoldenImageAssert.assertSimilarToGolden(result, "resize-thumbnail-128", tolerance = 3)
    }

    /**
     * fit(400, 300) 결과가 골든 이미지와 일치해야 합니다.
     * scrimage fit()은 비율을 유지하며 주어진 범위 안에 이미지를 맞춥니다.
     */
    @Test
    fun `resize fit 400x300 matches golden`() {
        val result = loadHomer().fit(400, 300)
        GoldenImageAssert.assertSimilarToGolden(result, "resize-fit-400x300", tolerance = 3)
    }
}
