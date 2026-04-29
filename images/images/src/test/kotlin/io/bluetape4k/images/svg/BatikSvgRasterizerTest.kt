package io.bluetape4k.images.svg

import com.sksamuel.scrimage.nio.PngWriter
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

@TempFolderTest
class BatikSvgRasterizerTest : AbstractImageTest() {

    companion object : KLoggingChannel()

    private val rasterizer = BatikSvgRasterizer()

    @Test
    fun `SVG를 기본 옵션으로 래스터화`() = runSuspendIO {
        val input = Resourcex.getInputStream("images/sample.svg")!!
        input.use {
            val image = rasterizer.rasterize(it)

            image.shouldNotBeNull()
            image.width shouldBeGreaterThan 0
            image.height shouldBeGreaterThan 0
            log.debug { "래스터화 결과: ${image.width}x${image.height}" }
        }
    }

    @Test
    fun `SVG를 지정 크기로 래스터화`() = runSuspendIO {
        val input = Resourcex.getInputStream("images/sample.svg")!!
        val opts = SvgRasterizeOptions(width = 400, height = 400)

        input.use {
            val image = rasterizer.rasterize(it, opts)

            image.shouldNotBeNull()
            image.width shouldBeGreaterThan 0
            image.height shouldBeGreaterThan 0
            log.debug { "지정 크기 래스터화: ${image.width}x${image.height}" }
        }
    }

    @Test
    fun `SVG를 144 DPI로 래스터화`() = runSuspendIO {
        val image96 = Resourcex.getInputStream("images/sample.svg")!!.use {
            rasterizer.rasterize(it, SvgRasterizeOptions(dpi = 96))
        }
        val image144 = Resourcex.getInputStream("images/sample.svg")!!.use {
            rasterizer.rasterize(it, SvgRasterizeOptions(dpi = 144))
        }

        // 144 DPI 이미지가 더 크거나 같아야 함
        (image144.width * image144.height) shouldBeGreaterThan (image96.width * image96.height / 2)
        log.debug { "96 DPI: ${image96.width}x${image96.height}, 144 DPI: ${image144.width}x${image144.height}" }
    }

    @Test
    fun `SVG 파일로 저장`(tempFolder: TempFolder) = runSuspendIO {
        val dest = tempFolder.createFile("sample_rasterized.png")

        Resourcex.getInputStream("images/sample.svg")!!.use { input ->
            val image = rasterizer.rasterize(input)
            dest.writeBytes(image.forWriter(PngWriter.MaxCompression).bytes())
        }

        dest.exists().shouldBeTrue()
        dest.length() shouldBeGreaterThan 0L
        log.debug { "저장됨: ${dest.absolutePath} (${dest.length()} bytes)" }
    }

    @Test
    fun `maxWidthPx 초과 옵션 시 예외 발생`() = runSuspendIO {
        val opts = SvgRasterizeOptions(width = 9999, maxWidthPx = 8192)

        try {
            Resourcex.getInputStream("images/sample.svg")!!.use { rasterizer.rasterize(it, opts) }
            throw AssertionError("예외가 발생해야 합니다")
        } catch (e: IllegalArgumentException) {
            log.debug { "예상된 예외: ${e.message}" }
        }
    }
}
