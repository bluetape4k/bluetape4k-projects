package io.bluetape4k.images

import io.bluetape4k.images.coroutines.CoJpegWriter
import io.bluetape4k.images.coroutines.CoPngWriter
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path

class ImmutableImageSupportTest: AbstractImageTest() {

    companion object: KLogging()

    private val useTempFile = true

    @ParameterizedTest(name = "load write coroutines: {0}.jpg")
    @MethodSource("getImageFileNames")
    fun `load and write jpg image async`(filename: String, tempFolder: TempFolder) = runTest {
        val image =
            loadImageSuspending(Path.of("${BASE_PATH}/$filename.jpg"))

        if (useTempFile) {
            image.forCoWriter(CoJpegWriter.Default).write(tempFolder.createFile().toPath())
        } else {
            image.forCoWriter(CoJpegWriter.Default)
                .write(Path.of("${BASE_PATH}/${filename}_async.jpg"))
        }
    }

    @ParameterizedTest(name = "load write coroutines: {0}.png")
    @MethodSource("getImageFileNames")
    fun `load and write png image async`(filename: String, tempFolder: TempFolder) = runTest {
        val image =
            loadImageSuspending(Path.of("${BASE_PATH}/$filename.png"))

        if (useTempFile) {
            image.forCoWriter(CoPngWriter.MaxCompression).write(tempFolder.createFile().toPath())
        } else {
            image.forCoWriter(CoPngWriter.MaxCompression)
                .write(Path.of("${BASE_PATH}/${filename}_async.png"))
        }
    }
}
