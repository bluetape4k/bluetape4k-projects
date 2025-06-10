package io.bluetape4k.images.coroutines.animated

import com.sksamuel.scrimage.nio.AnimatedGif
import io.bluetape4k.io.suspendWrite
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths

class SuspendAnimatedWriteContext(
    val writer: SuspendAnimatedImageWriter,
    val gif: AnimatedGif,
) {

    companion object: KLoggingChannel()

    suspend fun bytes(): ByteArray {
        return ByteArrayOutputStream().use { bos ->
            writer.suspendWrite(gif, bos)
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
        write(file.toPath())
        return file
    }

    suspend fun write(path: Path): Path {
        path.suspendWrite(bytes())
        return path
    }

    suspend fun write(out: OutputStream) {
        writer.suspendWrite(gif, out)
    }
}
