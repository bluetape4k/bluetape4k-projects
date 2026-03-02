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
 * val writer = SuspendJpegWriter()
 * val image = immutableImageOf(File("image.jpg"))
 * val context = SuspendWriteContext(writer, image, metadata)
 * context.write("output.jpg")
 * ```
 *
 * @property writer 이미지 쓰기 작업을 수행할 [SuspendImageWriter]
 * @property image 이미지 데이터
 * @property metadata 이미지 메타데이터
 */
class SuspendWriteContext(
    val writer: SuspendImageWriter,
    private val image: AwtImage,
    private val metadata: ImageMetadata,
) {

    companion object: KLoggingChannel()

    /**
     * 이미지를 인코딩하여 [ByteArray]로 반환합니다.
     *
     * @return 인코딩된 이미지 데이터
     */
    suspend fun bytes(): ByteArray {
        return ByteArrayOutputStream().use { bos ->
            writer.suspendWrite(image, metadata, bos)
            bos.toByteArray()
        }
    }

    /**
     * 이미지를 인코딩하여 [ByteArrayInputStream]으로 반환합니다.
     *
     * @return 인코딩된 이미지 데이터를 담은 [ByteArrayInputStream]
     */
    suspend fun stream(): ByteArrayInputStream {
        return ByteArrayInputStream(bytes())
    }

    /**
     * 이미지를 인코딩하여 [path] 경로 문자열에 저장합니다.
     *
     * @param path 저장할 파일 경로 문자열
     * @return 저장된 파일의 [Path]
     */
    suspend fun write(path: String): Path {
        return write(Paths.get(path))
    }

    /**
     * 이미지를 인코딩하여 [file]에 저장합니다.
     *
     * - [file]이 존재하지 않으면 예외가 발생합니다.
     *
     * @param file 저장할 대상 [File]
     * @return 저장된 [File]
     */
    suspend fun write(file: File): File {
        require(file.exists()) { "File not found: ${file.absolutePath}" }

        write(file.toPath())
        return file
    }

    /**
     * 이미지를 인코딩하여 [path]에 저장합니다.
     *
     * @param path 저장할 파일 [Path]
     * @return 저장된 파일의 [Path]
     */
    suspend fun write(path: Path): Path {
        path.writeSuspending(bytes())
        return path
    }

    /**
     * 이미지를 인코딩하여 [out]에 씁니다.
     *
     * @param out 쓰기 대상 [OutputStream]
     */
    suspend fun write(out: OutputStream) {
        writer.suspendWrite(image, metadata, out)
    }
}
