package io.bluetape4k.images.golden

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.webp.WebpWriter
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Resourcex
import org.junit.jupiter.api.Test

/**
 * 이미지 인코딩 결과를 골든 이미지와 비교하는 테스트.
 *
 * 골든 이미지가 없으면 [org.opentest4j.TestAbortedException]으로 자동 skip됩니다.
 * 골든 이미지 생성: `-Dbluetape4k.images.golden.update=true` 시스템 프로퍼티로 실행하세요.
 */
class GoldenEncoderTest {

    companion object : KLoggingChannel() {
        private const val HOMER_JPG = "/images/homer.jpg"
    }

    private fun loadHomer(): ImmutableImage =
        immutableImageOf(Resourcex.getBytes(HOMER_JPG))

    /**
     * JpegWriter(quality=80, progressive=false) 인코딩 결과가 골든 이미지와 일치해야 합니다.
     */
    @Test
    fun `encode jpeg quality 80 matches golden`() {
        val bytes = loadHomer().forWriter(JpegWriter(80, false)).bytes()
        GoldenImageAssert.assertSimilarToGolden(bytes, "encode-jpeg-q80", tolerance = 5)
    }

    /**
     * PngWriter.MaxCompression 인코딩 결과가 골든 이미지와 일치해야 합니다.
     */
    @Test
    fun `encode png max compression matches golden`() {
        val bytes = loadHomer().forWriter(PngWriter.MaxCompression).bytes()
        GoldenImageAssert.assertSimilarToGolden(bytes, "encode-png", tolerance = 1)
    }

    /**
     * WebpWriter.DEFAULT 인코딩 결과가 골든 이미지와 일치해야 합니다.
     */
    @Test
    fun `encode webp default matches golden`() {
        val bytes = loadHomer().forWriter(WebpWriter.DEFAULT).bytes()
        GoldenImageAssert.assertSimilarToGolden(bytes, "encode-webp", tolerance = 5)
    }
}
