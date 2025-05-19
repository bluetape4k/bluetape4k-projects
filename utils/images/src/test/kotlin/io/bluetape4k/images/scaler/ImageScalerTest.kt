package io.bluetape4k.images.scaler

import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.ImageFormat
import io.bluetape4k.images.write
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

@TempFolderTest
class ImageScalerTest: AbstractImageTest() {

    companion object: KLoggingChannel()

    private val needManualView = false

    private fun BufferedImage.writeJpg(path: String) {
        if (needManualView) {
            this.write(ImageFormat.JPG, path)
        }
    }

    @Test
    fun `이미지를 비율로 scale 한다`(tempFolder: TempFolder) {
        getImage(CAFE_JPG).use { input ->
            val image: BufferedImage = ImageIO.read(input)
            val scaled: BufferedImage = image.scale(0.15)

            scaled.width shouldBeGreaterThan 0
            scaled.height shouldBeGreaterThan 0

            scaled.writeJpg("$BASE_PATH/cafe_ratio.jpg")
            scaled.write(ImageFormat.JPG, tempFolder.createFile())
        }
    }

    @Test
    fun `이미지를 특정 크기 100x100로 Scaling한다`(tempFolder: TempFolder) {
        getImage(CAFE_JPG).use { input ->
            val image = ImageIO.read(input)
            val scaled = image.scale(100, 100, proportional = false)

            scaled.width shouldBeEqualTo 100
            scaled.height shouldBeEqualTo 100

            scaled.writeJpg("$BASE_PATH/cafe_fixed.jpg")
            scaled.write(ImageFormat.JPG, tempFolder.createFile())
        }
    }

    @Test
    fun `이미지를 특정 크기 100x100을 비례적으로 Scaling 한다`(tempFolder: TempFolder) {
        getImage(CAFE_JPG).use { input ->
            val image = ImageIO.read(input)
            val scaled = image.scale(100, 100)

            if (image.width > image.height) {
                scaled.width shouldBeEqualTo 100
            } else {
                scaled.height shouldBeEqualTo 100
            }

            scaled.writeJpg("$BASE_PATH/cafe_proportional.jpg")
            scaled.write(ImageFormat.JPG, tempFolder.createFile())
        }
    }
}
