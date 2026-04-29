package io.bluetape4k.images.golden

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.awt.Color
import java.awt.image.BufferedImage
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.TestAbortedException

/**
 * GoldenImageAssert 갱신 모드 / CI 가드 / 비교 실패 흐름 검증.
 *
 * UPDATE_MODE=false (기본) 상태에서:
 * - 골든 없음 → TestAbortedException (skip)
 * - ByteArray 오버로드도 동일하게 TestAbortedException (skip)
 *
 * 크기 불일치 시나리오는 GoldenImageAssertTest에서 이미 커버됨.
 *
 * 이 테스트 클래스는 주로 "갱신 모드가 꺼진 상태"에서의 정상 동작을 검증합니다.
 */
class GoldenUpdateModeTest {

    companion object : KLoggingChannel()

    private fun solidImage(w: Int, h: Int, color: Color): ImmutableImage {
        val buf = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = buf.createGraphics()
        g.color = color
        g.fillRect(0, 0, w, h)
        g.dispose()
        return ImmutableImage.fromAwt(buf)
    }

    /**
     * 업데이트 모드가 꺼진 상태에서 골든 이미지가 없으면 TestAbortedException이 발생합니다.
     */
    @Test
    fun `업데이트 모드 OFF에서 골든 없으면 TestAbortedException이 발생한다`() {
        val image = solidImage(16, 16, Color.RED)
        assertThrows<TestAbortedException> {
            GoldenImageAssert.assertSimilarToGolden(image, "nonexistent-update-key-abc")
        }
    }

    /**
     * ByteArray 오버로드도 골든 이미지가 없으면 TestAbortedException이 발생합니다.
     */
    @Test
    fun `ByteArray 오버로드에서 골든 없으면 TestAbortedException이 발생한다`() {
        val image = solidImage(16, 16, Color.BLUE)
        val bytes = image.forWriter(com.sksamuel.scrimage.nio.PngWriter.MaxCompression).bytes()
        assertThrows<TestAbortedException> {
            GoldenImageAssert.assertSimilarToGolden(bytes, "nonexistent-bytes-key-def")
        }
    }

    /**
     * 시스템 프로퍼티 "bluetape4k.images.golden.update" 기본값이 "false"임을 확인합니다.
     */
    @Test
    fun `시스템 프로퍼티 golden update 기본값은 false이다`() {
        val value = System.getProperty("bluetape4k.images.golden.update", "false")
        value shouldBeEqualTo "false"
    }
}
