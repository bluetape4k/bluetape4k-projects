package io.bluetape4k.images

import io.bluetape4k.io.suspendWrite
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Resourcex
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.imageio.ImageIO
import javax.imageio.ImageWriter

@TempFolderTest
abstract class AbstractImageTest {

    companion object: KLoggingChannel() {
        const val BASE_PATH = "src/test/resources/images"
        const val AQUA_JPG = "images/splitter/aqua.jpg"
        const val EVERLAND_JPG = "images/splitter/everland.jpg"
        const val CAFE_JPG = "images/cafe.jpg"
        const val LANDSCAPE_JPG = "images/landscape.jpg"
    }

    protected fun getImageFileNames() = listOf(
        "homer", "labor"
    )

    protected fun getImage(path: String): InputStream = Resourcex.getInputStream(path)!!

    protected fun writeToFile(bytes: ByteArray, filename: String, format: ImageFormat = ImageFormat.JPG) {
        val path = Paths.get("$BASE_PATH/$filename.${format.name}")
        if (Files.exists(path)) {
            Files.delete(path)
        }
        Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    }

    protected fun getImageWriter(format: ImageFormat): ImageWriter =
        ImageIO.getImageWritersByFormatName(format.name).next()

    protected fun writeToFile(items: List<ByteArray>, filename: String, format: ImageFormat) {
        items
            .forEachIndexed { index, bytes ->
                val path = Paths.get("$BASE_PATH/${filename}_${index}.${format.name}")
                if (Files.exists(path)) {
                    Files.delete(path)
                }
                Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            }
    }

    protected suspend fun writeToFileAsync(items: Flow<ByteArray>, filename: String, format: ImageFormat) {
        items
            .buffer()
            .collectIndexed { index, bytes ->
                val path = Paths.get("$BASE_PATH/${filename}_${index}.${format.name}")
                withContext(currentCoroutineContext()) {
                    if (Files.exists(path)) {
                        Files.delete(path)
                    }
                }
                path.suspendWrite(bytes)
                // Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            }
    }
}
