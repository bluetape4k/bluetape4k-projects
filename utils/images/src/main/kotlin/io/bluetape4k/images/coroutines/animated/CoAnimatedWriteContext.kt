package io.bluetape4k.images.coroutines.animated

import com.sksamuel.scrimage.nio.AnimatedGif
import io.bluetape4k.io.writeSuspending
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths

class CoAnimatedWriteContext(
    val writer: CoAnimatedImageWriter,
    val gif: AnimatedGif,
) {
    suspend fun bytes(): ByteArray {
        return ByteArrayOutputStream().use { bos ->
            writer.writeSuspending(gif, bos)
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
        path.writeSuspending(bytes())
        return path
    }

    suspend fun write(out: OutputStream) {
        writer.writeSuspending(gif, out)
    }
}
