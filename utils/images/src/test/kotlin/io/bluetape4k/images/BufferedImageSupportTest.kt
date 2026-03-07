package io.bluetape4k.images

import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@TempFolderTest
class BufferedImageSupportTest: AbstractImageTest() {

    companion object: KLoggingChannel()

    @Test
    fun `bufferedImageOf로 빈 이미지를 생성한다`() {
        val image = bufferedImageOf(200, 100)

        image.width shouldBeEqualTo 200
        image.height shouldBeEqualTo 100
    }

    @Test
    fun `InputStream에서 BufferedImage를 생성한다`() {
        getImage(CAFE_JPG).use { input ->
            val image = bufferedImageOf(input)

            image.width shouldBeGreaterThan 0
            image.height shouldBeGreaterThan 0
        }
    }

    @Test
    fun `ByteArray에서 BufferedImage를 생성한다`() {
        getImage(CAFE_JPG).use { input ->
            val bytes = input.readBytes()
            val image = bufferedImageOf(bytes)

            image.width shouldBeGreaterThan 0
            image.height shouldBeGreaterThan 0
        }
    }

    @Test
    fun `File에서 BufferedImage를 생성한다`(tempFolder: TempFolder) {
        val file = tempFolder.createFile()
        getImage(CAFE_JPG).use { input ->
            val original = ImageIO.read(input)
            ImageIO.write(original, "jpg", file)
        }

        val image = bufferedImageOf(file)
        image.width shouldBeGreaterThan 0
        image.height shouldBeGreaterThan 0
    }

    @Test
    fun `BufferedImage를 File로 저장한다`(tempFolder: TempFolder) {
        val image = bufferedImageOf(100, 100)
        image.useGraphics { g ->
            g.color = Color.RED
            g.fillRect(0, 0, 100, 100)
        }

        val file = tempFolder.createFile()
        image.write(ImageFormat.JPG, file).shouldBeTrue()

        val loaded = bufferedImageOf(file)
        loaded.width shouldBeEqualTo 100
        loaded.height shouldBeEqualTo 100
    }

    @Test
    fun `BufferedImage를 OutputStream으로 저장한다`() {
        val image = bufferedImageOf(100, 100)
        image.useGraphics { g ->
            g.color = Color.BLUE
            g.fillRect(0, 0, 100, 100)
        }

        val bos = ByteArrayOutputStream()
        image.write(ImageFormat.PNG, bos).shouldBeTrue()
        bos.toByteArray().shouldNotBeEmpty()
    }

    @Test
    fun `BufferedImage를 ByteArray로 변환한다`() {
        val image = bufferedImageOf(50, 50)
        image.useGraphics { g ->
            g.color = Color.GREEN
            g.fillRect(0, 0, 50, 50)
        }

        val bytes = image.toByteArray("png")
        bytes.shouldNotBeEmpty()

        val loaded = bufferedImageOf(bytes)
        loaded.width shouldBeEqualTo 50
        loaded.height shouldBeEqualTo 50
    }

    @Test
    fun `useGraphics로 그래픽 작업 후 Graphics2D가 dispose 된다`() {
        val image = bufferedImageOf(100, 100)

        image.useGraphics { g ->
            g.color = Color.RED
            g.fillRect(0, 0, 50, 50)
            g.color = Color.BLUE
            g.fillRect(50, 50, 50, 50)
        }

        // 그래픽 작업 후 이미지가 정상적으로 인코딩되는지 확인
        val bytes = image.toByteArray("png")
        bytes.shouldNotBeEmpty()
    }

    @Test
    fun `drawImage로 이미지를 합성한다`() {
        val base = bufferedImageOf(200, 200)
        base.useGraphics { g ->
            g.color = Color.WHITE
            g.fillRect(0, 0, 200, 200)
        }

        val overlay = bufferedImageOf(50, 50)
        overlay.useGraphics { g ->
            g.color = Color.RED
            g.fillRect(0, 0, 50, 50)
        }

        base.drawImage(overlay, 10, 10)

        val bytes = base.toByteArray("png")
        bytes.shouldNotBeEmpty()
    }

    @Test
    fun `drawRenderedImage로 변환된 이미지를 그린다`() {
        val base = bufferedImageOf(200, 200)
        val source = bufferedImageOf(100, 100)
        source.useGraphics { g ->
            g.color = Color.GREEN
            g.fillRect(0, 0, 100, 100)
        }

        val transform = java.awt.geom.AffineTransform.getTranslateInstance(50.0, 50.0)
        base.drawRenderedImage(source, transform)

        val bytes = base.toByteArray("png")
        bytes.shouldNotBeEmpty()
    }
}
