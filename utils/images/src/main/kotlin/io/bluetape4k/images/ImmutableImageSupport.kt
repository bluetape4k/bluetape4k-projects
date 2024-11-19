package io.bluetape4k.images

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.coroutines.CoImageWriter
import io.bluetape4k.images.coroutines.CoWriteContext
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
suspend fun immutableImageOfSuspending(file: File): ImmutableImage =
    immutableImageOfSuspending(file.toPath())

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
suspend fun immutableImageOfSuspending(path: Path): ImmutableImage =
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
suspend fun loadImageSuspending(file: File): ImmutableImage {
    return loadImageSuspending(file.toPath())
}

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
suspend fun loadImageSuspending(path: Path): ImmutableImage {
    return immutableImageOf(path.readAllBytesSuspending())
}

/**
 * Coroutines 환경에서 [ImmutableImage] 정보를 [writer]를 통해 [ByteArray]로 변환합니다.
 *
 * ```
 * val image = immutableImageOf(File("image.jpg"))
 * val bytes = image.bytesSuspending(JpegWriter())
 * ```
 *
 * @param writer 이미지를 쓰기 위한 [CoImageWriter]
 * @return 이미지 정보를 담은 ByteArray
 */
suspend fun ImmutableImage.bytesSuspending(writer: CoImageWriter): ByteArray {
    return ByteArrayOutputStream(DEFAULT_BUFFER_SIZE).use { bos ->
        writer.writeSuspending(this, this.metadata, bos)
        bos.toByteArray()
    }
}

/**
 * Coroutines 환경에서 [ImmutableImage] 정보를 [writer]를 통해 [destPath]에 저장합니다.
 *
 * ```
 * val image = immutableImageOf(File("image.jpg"))
 * image.writeSuspending(JpegWriter(), File("output.jpg"))
 * ```
 *
 * @param writer 이미지를 쓰기 위한 [CoImageWriter]
 * @param destPath 저장할 파일의 경로
 * @return 저장된 파일의 크기
 */
suspend fun ImmutableImage.writeSuspending(writer: CoImageWriter, destPath: Path): Long {
    val bytes = bytesSuspending(writer)
    return destPath.writeSuspending(bytes)
}

/**
 * [ImmutableImage] 정보를 쓰기 작업을 위해 [writer]를 사용하는 [CoWriteContext]를 생성합니다.
 *
 * ```
 * val image = immutableImageOf(File("image.jpg"))
 * val context = image.forCoWriter(writer)
 * context.write(destPath)
 * ```
 *
 * @param writer 이미지를 쓰기 위한 [CoImageWriter]
 * @return [CoWriteContext] instance
 */
fun ImmutableImage.forCoWriter(writer: CoImageWriter): CoWriteContext =
    CoWriteContext(writer, this, this.metadata)


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
inline fun ImmutableImage.useGraphics(action: (graphics: Graphics2D) -> Unit) {
    val graphics = this.awt().createGraphics()
    try {
        action(graphics)
    } finally {
        graphics.dispose()
    }
}
