package io.bluetape4k.images.coroutines.animated

import com.sksamuel.scrimage.nio.AnimatedGif
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path

suspend fun AnimatedGif.suspendBytes(writer: SuspendGif2WebpWriter): ByteArray {
    return forSuspendWriter(writer).bytes()
}

suspend fun AnimatedGif.suspendWrite(writer: SuspendGif2WebpWriter, bos: ByteArrayOutputStream) {
    forSuspendWriter(writer).write(bos)
}

suspend fun AnimatedGif.suspendWrite(writer: SuspendGif2WebpWriter, path: Path): Path {
    return forSuspendWriter(writer).write(path)
}

suspend fun AnimatedGif.suspendOutput(writer: SuspendGif2WebpWriter, file: File): File {
    return forSuspendWriter(writer).write(file)
}

suspend fun AnimatedGif.suspendOutput(writer: SuspendGif2WebpWriter, path: Path): Path {
    return forSuspendWriter(writer).write(path)
}

fun AnimatedGif.forSuspendWriter(writer: SuspendGif2WebpWriter): SuspendAnimatedWriteContext =
    SuspendAnimatedWriteContext(writer, this)
