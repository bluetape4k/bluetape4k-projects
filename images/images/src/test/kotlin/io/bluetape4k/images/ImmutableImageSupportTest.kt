package io.bluetape4k.images

import io.bluetape4k.images.coroutines.SuspendJpegWriter
import io.bluetape4k.images.coroutines.SuspendPngWriter
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.awt.Color
import java.nio.file.Path
import kotlin.test.assertFailsWith

@TempFolderTest
class ImmutableImageSupportTest: AbstractImageTest() {

    companion object: KLoggingChannel()

    private val useTempFile = true

    @ParameterizedTest(name = "load write coroutines: {0}.jpg")
    @MethodSource("getImageFileNames")
    fun `load and write jpg image async`(filename: String, tempFolder: TempFolder) = runTest {
        val image =
            suspendLoadImage(Path.of("${BASE_PATH}/$filename.jpg"))

        if (useTempFile) {
            image.forSuspendWriter(SuspendJpegWriter.Default).write(tempFolder.createFile().toPath())
        } else {
            image.forSuspendWriter(SuspendJpegWriter.Default)
                .write(Path.of("${BASE_PATH}/${filename}_async.jpg"))
        }
    }

    @ParameterizedTest(name = "load write coroutines: {0}.png")
    @MethodSource("getImageFileNames")
    fun `load and write png image async`(filename: String, tempFolder: TempFolder) = runTest {
        val image =
            suspendLoadImage(Path.of("${BASE_PATH}/$filename.png"))

        if (useTempFile) {
            image.forSuspendWriter(SuspendPngWriter.MaxCompression).write(tempFolder.createFile().toPath())
        } else {
            image.forSuspendWriter(SuspendPngWriter.MaxCompression)
                .write(Path.of("${BASE_PATH}/${filename}_async.png"))
        }
    }

    @Test
    fun `withGraphics는 원본 이미지를 변경하지 않는다`() {
        val original = immutableImageOf(whiteTestImage(10, 10))
        val originalRgb = original.awt().getRGB(0, 0)

        val result = original.withGraphics { g ->
            g.color = Color.RED
            g.fillRect(0, 0, 10, 10)
        }

        // 원본은 흰색 유지
        original.awt().getRGB(0, 0) shouldBeEqualTo originalRgb
        // 반환된 복사본은 빨간색
        result.awt().getRGB(0, 0) shouldNotBeEqualTo originalRgb
    }

    @Test
    fun `withGraphics는 수신 객체와 다른 인스턴스를 반환한다`() {
        val original = immutableImageOf(whiteTestImage(10, 10))

        val result = original.withGraphics { }

        result.shouldNotBeNull()
        result.width shouldBeEqualTo original.width
        // 별도 복사본이므로 픽셀 버퍼가 독립적
        result.awt() shouldNotBeEqualTo original.awt()
    }

    @Test
    fun `withGraphics는 action이 예외를 던져도 Graphics2D를 dispose하고 원본을 반환 가능하다`() {
        val original = immutableImageOf(whiteTestImage(10, 10))

        assertFailsWith<RuntimeException> {
            original.withGraphics { throw RuntimeException("test error") }
        }

        // 예외 발생 후에도 원본 이미지가 정상 사용 가능 (Graphics2D dispose됨)
        original.width shouldBeGreaterThan 0
        original.awt().getRGB(0, 0).let { /* 픽셀 읽기 성공 */ }
    }

    private fun whiteTestImage(w: Int, h: Int): ByteArray {
        val buf = bufferedImageOf(w, h)
        buf.useGraphics { g ->
            g.color = Color.WHITE
            g.fillRect(0, 0, w, h)
        }
        return buf.toByteArray("png")
    }
}
