package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.metadata.ImageMetadata
import io.bluetape4k.io.writeSuspending
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Coroutines 방식으로 쓰기 작업 시 사용할 Context 입니다.
 *
 * ```
 * val writer = CoJpegWriter()
 * val image = immutableImageOf(File("image.jpg"))
 * val context = CoWriteContext(writer, image, metadata)
 * context.write("output.jpg")
 * ```
 *
 * @property writer 이미지 쓰기 작업을 수행할 [CoImageWriter]
 * @property image 이미지 데이터
 * @property metadata 이미지 메타데이터
 */
class SuspendWriteContext(
    val writer: SuspendImageWriter,
    private val image: AwtImage,
    private val metadata: ImageMetadata,
) {

    companion object: KLoggingChannel()

    suspend fun bytes(): ByteArray {
        return ByteArrayOutputStream().use { bos ->
            writer.suspendWrite(image, metadata, bos)
            bos.toByteArray()
        }
    }

    suspend fun stream(): ByteArrayInputStream {
        return ByteArrayInputStream(bytes())
    }

    suspend fun write(path: String): Path {
        return write(Paths.get(path))
    }

    suspend fun write(file: File): File {
        require(file.exists()) { "File not found: ${file.absolutePath}" }

        write(file.toPath())
        return file
    }

    suspend fun write(path: Path): Path {
        path.writeSuspending(bytes())
        return path
    }

    suspend fun write(out: OutputStream) {
        writer.suspendWrite(image, metadata, out)
    }
}
