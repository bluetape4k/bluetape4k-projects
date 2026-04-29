package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [AbstractFilterTest]에서 제공하는 픽셀 유사도 헬퍼 메서드의 단위 테스트입니다.
 */
class AbstractFilterTestHelpersTest: AbstractFilterTest() {

    companion object: KLoggingChannel()

    @Test
    fun `동일 이미지는 assertSimilarToImage 기본 tolerance로 통과한다`() {
        val image = loadResourceImage("debop.jpg")
        assertSimilarToImage(image, image)
    }

    @Test
    fun `동일 이미지는 tolerance=0으로 assertSimilarToImage 통과한다`() {
        val image = loadResourceImage("debop.jpg")
        assertSimilarToImage(image, image, tolerance = 0)
    }

    @Test
    fun `서로 다른 이미지는 assertNotSimilarToImage 통과한다`() {
        val original = loadResourceImage("debop.jpg")
        val watermarked = loadResourceImage("debop_watermark.jpg")
        assertNotSimilarToImage(original, watermarked)
    }

    @Test
    fun `동일 이미지에 assertNotSimilarToImage를 호출하면 실패한다`() {
        val image = loadResourceImage("debop.jpg")
        assertThrows<AssertionError> {
            assertNotSimilarToImage(image, image)
        }
    }

    @Test
    fun `크기가 다른 이미지는 assertSimilarToImage 호출 시 실패한다`() {
        val original = loadResourceImage("debop.jpg")
        val resized = ImmutableImage.create(original.width / 2, original.height / 2)
        assertThrows<AssertionError> {
            assertSimilarToImage(original, resized)
        }
    }

    @Test
    fun `크기가 다른 이미지는 assertNotSimilarToImage 호출 시 통과한다`() {
        val original = loadResourceImage("debop.jpg")
        val resized = ImmutableImage.create(original.width / 2, original.height / 2)
        // 크기가 달라서 명백히 다른 이미지이므로 통과해야 한다
        assertNotSimilarToImage(original, resized)
    }

    @Test
    fun `assertSimilarToResource는 동일 이미지 리소스에 대해 통과한다`() {
        val image = loadResourceImage("debop.jpg")
        assertSimilarToResource(image, "debop.jpg", tolerance = 0)
    }
}
