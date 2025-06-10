package io.bluetape4k.images

import io.bluetape4k.images.coroutines.SuspendJpegWriter
import io.bluetape4k.images.coroutines.SuspendPngWriter
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path

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
}
