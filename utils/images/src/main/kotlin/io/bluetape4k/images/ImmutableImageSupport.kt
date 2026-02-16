package io.bluetape4k.images

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.coroutines.SuspendImageWriter
import io.bluetape4k.images.coroutines.SuspendWriteContext
import io.bluetape4k.io.readAllBytesSuspending
import io.bluetape4k.io.writeSuspending
import java.awt.Graphics2D
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Path

/**
 * [ByteArray]를 읽어 [ImmutableImage]로 변환합니다.
 *
 * ```
 * val image = immutableImageOf(byteArrayOf(0x00, 0x01, 0x02, 0x03))
 * ```
 *
 * @param bytes 이미지 정보를 담은 [ByteArray]
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
fun immutableImageOf(bytes: ByteArray): ImmutableImage =
    ImmutableImage.loader().fromBytes(bytes)

/**
 * [InputStream]을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ```
 * val image = immutableImageOf(File("image.jpg").inputStream())
 * ```
 *
 * @param inputStream 이미지 정보를 담은 [InputStream]
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
fun immutableImageOf(inputStream: InputStream): ImmutableImage =
    ImmutableImage.loader().fromStream(inputStream.buffered())

/**
 * [File]을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ```
 * val image = immutableImageOf(File("image.jpg"))
 * ```
 *
 * @param file 이미지 파일
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
fun immutableImageOf(file: File): ImmutableImage =
    ImmutableImage.loader().fromFile(file)

/**
 * [Path]의 파일을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ```
 * val image = immutableImageOf(File("image.jpg"))
 * ```
 *
 * @param path 이미지 파일의 경로
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
fun immutableImageOf(path: Path): ImmutableImage =
    ImmutableImage.loader().fromPath(path)

/**
 * Coroutines 환경에서 [File]을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ```
 * val image = immutableImageOfSuspending(File("image.jpg"))
 * ```
 *
 * @param file 이미지 파일
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
suspend fun suspendImmutableImageOf(file: File): ImmutableImage =
    suspendImmutableImageOf(file.toPath())

/**
 * Coroutines 환경에서 [Path]의 파일을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ```
 * val image = immutableImageOfSuspending(File("image.jpg"))
 * ```
 *
 * @param path 이미지 파일의 경로
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
suspend fun suspendImmutableImageOf(path: Path): ImmutableImage =
    immutableImageOf(path.readAllBytesSuspending())


/**
 * Coroutines 환경에서 [File]을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ```
 * val image = loadImageSuspending(File("image.jpg"))
 * ```
 *
 * @param file 이미지 파일
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
suspend fun suspendLoadImage(file: File): ImmutableImage =
    suspendLoadImage(file.toPath())

/**
 * Coroutines 환경에서 [Path]의 파일을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ```
 * val image = loadImageSuspending(File("image.jpg"))
 * ```
 *
 * @param path 이미지 파일의 경로
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
suspend fun suspendLoadImage(path: Path): ImmutableImage =
    immutableImageOf(path.readAllBytesSuspending())

/**
 * Coroutines 환경에서 [ImmutableImage] 정보를 [writer]를 통해 [ByteArray]로 변환합니다.
 *
 * ```
 * val image = immutableImageOf(File("image.jpg"))
 * val bytes = image.bytesSuspending(JpegWriter())
 * ```
 *
 * @param writer 이미지를 쓰기 위한 [SuspendImageWriter]
 * @return 이미지 정보를 담은 ByteArray
 */
suspend inline fun ImmutableImage.suspendBytes(writer: SuspendImageWriter): ByteArray =
    ByteArrayOutputStream(DEFAULT_BUFFER_SIZE).use { bos ->
        writer.suspendWrite(this, this.metadata, bos)
        bos.toByteArray()
    }

/**
 * Coroutines 환경에서 [ImmutableImage] 정보를 [writer]를 통해 [destPath]에 저장합니다.
 *
 * ```
 * val image = immutableImageOf(File("image.jpg"))
 * image.writeSuspending(JpegWriter(), File("output.jpg"))
 * ```
 *
 * @param writer 이미지를 쓰기 위한 [SuspendImageWriter]
 * @param destPath 저장할 파일의 경로
 * @return 저장된 파일의 크기
 */
suspend fun ImmutableImage.suspendWrite(writer: SuspendImageWriter, destPath: Path): Long {
    val bytes = suspendBytes(writer)
    return destPath.writeSuspending(bytes)
}

/**
 * [ImmutableImage] 정보를 쓰기 작업을 위해 [writer]를 사용하는 [SuspendWriteContext]를 생성합니다.
 *
 * ```
 * val image = immutableImageOf(File("image.jpg"))
 * val context = image.forSuspendWriter(writer)
 * context.write(destPath)
 * ```
 *
 * @param writer 이미지를 쓰기 위한 [SuspendImageWriter]
 * @return [SuspendWriteContext] instance
 */
fun ImmutableImage.forSuspendWriter(writer: SuspendImageWriter): SuspendWriteContext =
    SuspendWriteContext(writer, this, this.metadata)


/**
 * [ImmutableImage]에 그리기 작업 ([action]) 을 수행합니다.
 *
 * ```
 * val image = immutableImageOf(File("image.jpg"))
 * image.useGraphics { graphics ->
 *    graphics.color = Color.RED
 *    graphics.fillRect(0, 0, 100, 100)
 *    // ...
 * }
 * ```
 *
 * @param action 그래픽 작업
 */
inline fun ImmutableImage.useGraphics(
    @BuilderInference action: (graphics: Graphics2D) -> Unit,
) {
    val graphics: Graphics2D = this.awt().createGraphics()
    try {
        action(graphics)
    } finally {
        graphics.dispose()
    }
}
