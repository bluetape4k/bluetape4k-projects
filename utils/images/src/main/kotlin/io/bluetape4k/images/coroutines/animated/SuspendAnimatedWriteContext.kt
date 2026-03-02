package io.bluetape4k.images.coroutines.animated

import com.sksamuel.scrimage.nio.AnimatedGif
import io.bluetape4k.io.writeSuspending
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths

/**
 * [SuspendAnimatedImageWriter]를 사용하여 [AnimatedGif]를 Coroutines 방식으로 출력하는 컨텍스트입니다.
 *
 * ```kotlin
 * val writer = SuspendGif2WebpWriter.Default
 * val gif: AnimatedGif = ...
 * val context = SuspendAnimatedWriteContext(writer, gif)
 * context.write(Path.of("output.webp"))
 * ```
 *
 * @property writer 애니메이션 이미지 쓰기에 사용할 [SuspendAnimatedImageWriter]
 * @property gif    출력할 [AnimatedGif] 데이터
 */
class SuspendAnimatedWriteContext(
    val writer: SuspendAnimatedImageWriter,
    val gif: AnimatedGif,
) {

    companion object: KLoggingChannel()

    /**
     * [gif]를 인코딩하여 [ByteArray]로 반환합니다.
     *
     * @return 인코딩된 이미지 데이터
     */
    suspend fun bytes(): ByteArray {
        return ByteArrayOutputStream().use { bos ->
            writer.suspendWrite(gif, bos)
            bos.toByteArray()
        }
    }

    /**
     * [gif]를 인코딩하여 [ByteArrayInputStream]으로 반환합니다.
     *
     * @return 인코딩된 이미지 데이터를 담은 [ByteArrayInputStream]
     */
    suspend fun stream(): ByteArrayInputStream {
        return ByteArrayInputStream(bytes())
    }

    /**
     * [gif]를 인코딩하여 [path] 경로 문자열에 저장합니다.
     *
     * @param path 저장할 파일 경로 문자열
     * @return 저장된 파일의 [Path]
     */
    suspend fun write(path: String): Path {
        return write(Paths.get(path))
    }

    /**
     * [gif]를 인코딩하여 [file]에 저장합니다.
     *
     * @param file 저장할 대상 [File]
     * @return 저장된 [File]
     */
    suspend fun write(file: File): File {
        write(file.toPath())
        return file
    }

    /**
     * [gif]를 인코딩하여 [path]에 저장합니다.
     *
     * @param path 저장할 파일 [Path]
     * @return 저장된 파일의 [Path]
     */
    suspend fun write(path: Path): Path {
        path.writeSuspending(bytes())
        return path
    }

    /**
     * [gif]를 인코딩하여 [out]에 씁니다.
     *
     * @param out 쓰기 대상 [OutputStream]
     */
    suspend fun write(out: OutputStream) {
        writer.suspendWrite(gif, out)
    }
}
