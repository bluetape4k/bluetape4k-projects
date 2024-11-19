package io.bluetape4k.images.splitter

import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.ImageFormat
import io.bluetape4k.images.coroutines.CoJpegWriter
import io.bluetape4k.io.toInputStream
import io.bluetape4k.io.write
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import javax.imageio.ImageIO
import kotlin.test.assertFailsWith

@TempFolderTest
class ImageSplitterTest: AbstractImageTest() {

    companion object: KLogging() {
        private const val AQUA_JPG = "images/splitter/aqua.jpg"
        private const val EVERLAND_JPG = "images/splitter/everland.jpg"
    }

    private val splitter = ImageSplitter(1024)

    @Test
    fun `split 0 height`() = runTest {
        getImage(AQUA_JPG).use { input ->
            assertFailsWith<IllegalArgumentException> {
                splitter.split(input, ImageFormat.JPG, 0)
            }
        }
    }

    @Test
    fun `split very small image but use min split height`() = runTest {
        getImage(AQUA_JPG).use { input ->
            val items = splitter.split(input, ImageFormat.JPG, 5).toList()
            log.debug { "items size=${items.size}" }
            items.shouldNotBeEmpty()
            items.all { it.isNotEmpty() }.shouldBeTrue()

            // 아주 작은 Height 를 지정하면, ImageSplitter.DEFAULT_MIN_HEIGHT 를 사용합니다.
            val firstImage = ImageIO.read(items[0].toInputStream())
            firstImage.height shouldBeEqualTo ImageSplitter.DEFAULT_MIN_HEIGHT
        }
    }

    @ParameterizedTest(name = "split {0}")
    @ValueSource(strings = [AQUA_JPG, EVERLAND_JPG])
    fun `split jpg image with default height`(path: String, tempFolder: TempFolder) = runTest {
        getImage(path).use { input ->
            val items: List<ByteArray> = splitter.split(input, ImageFormat.JPG).toList()
            log.debug { "items size=${items.size}" }

            items.forEach {
                val image = ImageIO.read(it.toInputStream())
                image.height shouldBeLessOrEqualTo splitter.defaultMaxHeight
            }
            items.forEach {
                tempFolder.createFile().write(it)
            }
        }
    }

    @ParameterizedTest(name = "split and compress {0}")
    @ValueSource(strings = [AQUA_JPG, EVERLAND_JPG])
    fun `split and compress image`(path: String, tempFolder: TempFolder) = runTest {
        getImage(path).use { input ->
            val items: Flow<ByteArray> = splitter
                .splitAndCompress(
                    input,
                    ImageFormat.JPG,
                    writer = CoJpegWriter.Default
                )

            items.buffer().collect {
                tempFolder.createFile().write(it)
            }
        }
    }
}
