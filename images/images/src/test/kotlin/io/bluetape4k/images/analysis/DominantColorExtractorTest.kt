package io.bluetape4k.images.analysis

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldMatch
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class DominantColorExtractorTest {

    companion object: KLoggingChannel() {
        private const val HOMER_JPG = "images/homer.jpg"
        private const val CAFE_JPG = "images/cafe.jpg"
        private const val LANDSCAPE_JPG = "images/landscape.jpg"

        private fun loadImage(path: String): ImmutableImage =
            ImmutableImage.loader().fromStream(Resourcex.getInputStream(path)!!)

        /** 단색 JPEG 이미지를 프로그래밍으로 생성한다. */
        fun solidColorImage(r: Int, g: Int, b: Int, width: Int = 100, height: Int = 100): ImmutableImage {
            val buffered = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
            val color = java.awt.Color(r, g, b)
            val gfx = buffered.createGraphics()
            gfx.color = color
            gfx.fillRect(0, 0, width, height)
            gfx.dispose()
            val baos = ByteArrayOutputStream()
            ImageIO.write(buffered, "jpg", baos)
            return ImmutableImage.loader().fromBytes(baos.toByteArray())
        }

        /** 단색 PNG 이미지를 프로그래밍으로 생성한다 (손실 없음 — ignoreWhite 테스트용). */
        fun solidColorPngImage(r: Int, g: Int, b: Int, width: Int = 50, height: Int = 50): ImmutableImage {
            val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val color = java.awt.Color(r, g, b)
            val gfx = buffered.createGraphics()
            gfx.color = color
            gfx.fillRect(0, 0, width, height)
            gfx.dispose()
            val baos = ByteArrayOutputStream()
            ImageIO.write(buffered, "png", baos)
            return ImmutableImage.loader().fromBytes(baos.toByteArray())
        }
    }

    @Test
    fun `dominantColors returns correct number of colors`() {
        val image = loadImage(HOMER_JPG)
        val colors = image.dominantColors(5)
        log.debug { "homer dominantColors(5): $colors" }
        colors.shouldNotBeEmpty()
        colors.size shouldBeLessOrEqualTo 5
    }

    @Test
    fun `dominantColor returns single nullable color`() {
        val image = loadImage(HOMER_JPG)
        val color = image.dominantColor()
        log.debug { "homer dominantColor: $color" }
        color.shouldNotBeNull()
    }

    @Test
    fun `red solid image returns reddish dominant color`() {
        val image = solidColorImage(220, 30, 30)
        val color = image.dominantColor()
        log.debug { "red solid dominantColor: $color" }
        color.shouldNotBeNull()
        color.r shouldBeGreaterOrEqualTo color.g
        color.r shouldBeGreaterOrEqualTo color.b
    }

    @Test
    fun `blue solid image returns bluish dominant color`() {
        val image = solidColorImage(30, 30, 200)
        val color = image.dominantColor()
        log.debug { "blue solid dominantColor: $color" }
        color.shouldNotBeNull()
        color.b shouldBeGreaterOrEqualTo color.r
        color.b shouldBeGreaterOrEqualTo color.g
    }

    @Test
    fun `count=1 returns at most 1 color`() {
        val image = loadImage(HOMER_JPG)
        val colors = image.dominantColors(1)
        colors.size shouldBeLessOrEqualTo 1
    }

    @Test
    fun `colors are sorted by population descending`() {
        val image = loadImage(CAFE_JPG)
        val colors = image.dominantColors(5)
        log.debug { "cafe dominantColors(5): $colors" }
        colors.shouldNotBeEmpty()
        for (i in 0 until colors.size - 1) {
            colors[i].population shouldBeGreaterOrEqualTo colors[i + 1].population
        }
    }

    @Test
    fun `all color values are in 0-255 range`() {
        val image = loadImage(LANDSCAPE_JPG)
        val colors = image.dominantColors(8)
        colors.forEach { color ->
            color.r shouldBeGreaterOrEqualTo 0
            color.r shouldBeLessOrEqualTo 255
            color.g shouldBeGreaterOrEqualTo 0
            color.g shouldBeLessOrEqualTo 255
            color.b shouldBeGreaterOrEqualTo 0
            color.b shouldBeLessOrEqualTo 255
            color.population shouldBeGreaterOrEqualTo 0
        }
    }

    @Test
    fun `hex format is correct`() {
        val color = DominantColor(255, 128, 0, 100)
        color.hex shouldMatch "#[0-9a-f]{6}"
        color.hex shouldBeEqualTo "#ff8000"
    }

    @Test
    fun `fromRgb parses RGB integer correctly`() {
        val rgb = 0xFF8000 // orange: R=255 G=128 B=0
        val color = DominantColor.fromRgb(rgb, population = 42)
        color.r shouldBeEqualTo 0xFF
        color.g shouldBeEqualTo 0x80
        color.b shouldBeEqualTo 0x00
        color.population shouldBeEqualTo 42
    }

    @Test
    fun `toAwtColor returns correct color`() {
        val dominant = DominantColor(100, 150, 200, 50)
        val awt = dominant.toAwtColor()
        awt.red shouldBeEqualTo 100
        awt.green shouldBeEqualTo 150
        awt.blue shouldBeEqualTo 200
    }

    @Test
    fun `invalid quality throws`() {
        invoking {
            DominantColorExtractor.MedianCut(quality = 0)
        } shouldThrow IllegalArgumentException::class

        invoking {
            DominantColorExtractor.MedianCut(quality = 31)
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `invalid count throws`() {
        val image = loadImage(HOMER_JPG)
        invoking {
            image.dominantColors(count = 0)
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `ignoreWhite excludes near-white pixels`() {
        // PNG(손실 없음) 흰색 이미지 → ignoreWhite=true 이면 픽셀 없어 emptyList 반환
        val whiteImage = solidColorPngImage(255, 255, 255)
        val withIgnore = whiteImage.dominantColors(5, DominantColorExtractor.medianCut(ignoreWhite = true))
        val withoutIgnore = whiteImage.dominantColors(5, DominantColorExtractor.medianCut(ignoreWhite = false))
        log.debug { "white ignoreWhite=true: $withIgnore, ignoreWhite=false: $withoutIgnore" }
        withIgnore.shouldBeEmpty()
        withoutIgnore.shouldNotBeEmpty()
    }

    @Test
    fun `dominantColor on empty image returns null`() {
        // 완전 투명 이미지 (alpha=0) → 픽셀 없음 → null
        val transparent = ImmutableImage.create(10, 10)
        val color = transparent.dominantColor()
        log.debug { "transparent dominantColor: $color" }
        color.shouldBeNull()
    }

    @Test
    fun `medianCut quality=1 samples more pixels than quality=10`() {
        val image = loadImage(HOMER_JPG)
        val fast = image.dominantColors(3, DominantColorExtractor.medianCut(quality = 10))
        val precise = image.dominantColors(3, DominantColorExtractor.medianCut(quality = 1))
        log.debug { "fast: $fast, precise: $precise" }
        fast.shouldNotBeEmpty()
        precise.shouldNotBeEmpty()
        // quality=1은 모든 픽셀 샘플링 → 첫 번째 색상의 population이 더 큼
        precise.first().population shouldBeGreaterThan fast.first().population
    }

    @Test
    fun `DominantColor init rejects out-of-range values`() {
        invoking { DominantColor(-1, 0, 0, 1) } shouldThrow IllegalArgumentException::class
        invoking { DominantColor(0, 256, 0, 1) } shouldThrow IllegalArgumentException::class
        invoking { DominantColor(0, 0, 300, 1) } shouldThrow IllegalArgumentException::class
        invoking { DominantColor(0, 0, 0, -1) } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `cafe image extracts multiple distinct colors`() {
        val image = loadImage(CAFE_JPG)
        val colors = image.dominantColors(5)
        log.debug { "cafe colors: ${colors.map { it.hex }}" }
        colors shouldHaveSize colors.distinctBy { it.hex }.size // 중복 hex 없어야 함
    }
}
