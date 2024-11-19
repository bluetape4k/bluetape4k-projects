package io.bluetape4k.images.coroutines.animated

import com.sksamuel.scrimage.nio.AnimatedGif
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path

suspend fun AnimatedGif.bytesSuspending(writer: CoGif2WebpWriter): ByteArray {
    return forCoWriter(writer).bytes()
}

suspend fun AnimatedGif.writeSuspending(writer: CoGif2WebpWriter, bos: ByteArrayOutputStream) {
    forCoWriter(writer).write(bos)
}

suspend fun AnimatedGif.writeSuspending(writer: CoGif2WebpWriter, path: Path): Path {
    return forCoWriter(writer).write(path)
}

suspend fun AnimatedGif.outputSuspending(writer: CoGif2WebpWriter, file: File): File {
    return forCoWriter(writer).write(file)
}

suspend fun AnimatedGif.outputSuspending(writer: CoGif2WebpWriter, path: Path): Path {
    return forCoWriter(writer).write(path)
}

fun AnimatedGif.forCoWriter(writer: CoGif2WebpWriter): CoAnimatedWriteContext =
    CoAnimatedWriteContext(writer, this)
