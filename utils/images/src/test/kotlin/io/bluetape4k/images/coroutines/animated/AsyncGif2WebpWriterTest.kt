package io.bluetape4k.images.coroutines.animated

import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageSource
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.io.readAllBytesSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class AsyncGif2WebpWriterTest: AbstractImageTest() {

    companion object: KLogging()

    private val useTempFolder = true

    @Test
    fun `convert animated gif to webp`(tempFolder: TempFolder) = runSuspendIO {
        val originBytes = Path.of("$BASE_PATH/animated.gif").readAllBytesSuspending()
        val gif2 = AnimatedGifReader.read(ImageSource.of(originBytes))

        val saved = if (useTempFolder) {
            gif2.forCoWriter(CoGif2WebpWriter.Default).write(tempFolder.createFile().toPath())
        } else {
            gif2.forCoWriter(CoGif2WebpWriter.Default).write(Path.of("$BASE_PATH/animated.webp"))
        }
        log.debug { "save animated.webp file to $saved" }
        saved.toFile().exists().shouldBeTrue()
        saved.toFile().length().shouldBeGreaterThan(0)
    }
}
