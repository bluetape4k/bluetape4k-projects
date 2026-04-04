package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.nio.ImageWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

/**
 * Coroutines 방식으로 이미지를 생성하는 [ImageWriter] 입니다.
 *
 * ## 동작/계약
 * - [ImageWriter]를 확장하여 `suspend` 함수 기반의 비동기 쓰기를 지원합니다.
 * - 내부적으로 [Dispatchers.IO] 컨텍스트에서 blocking I/O를 수행합니다.
 *
 * ```kotlin
 * val writer: SuspendImageWriter = SuspendPngWriter.MaxCompression
 * val image = immutableImageOf(File("photo.png"))
 * val bos = ByteArrayOutputStream()
 * writer.suspendWrite(image, bos)
 * // bos.size() > 0
 * ```
 *
 * @see SuspendJpegWriter
 * @see SuspendPngWriter
 * @see SuspendGifWriter
 * @see SuspendWebpWriter
 */
interface SuspendImageWriter: ImageWriter {

    /**
     * Coroutines 방식으로 [image]를 [out]에 씁니다.
     *
     * ```kotlin
     * val writer = SuspendJpegWriter.Default
     * val image = immutableImageOf(File("photo.jpg"))
     * val bos = ByteArrayOutputStream()
     * writer.suspendWrite(image, image.metadata, bos)
     * // bos.size() > 0
     * ```
     *
     * @param image   쓸 이미지
     * @param metadata 이미지의 메타데이터 ([ImageMetadata])
     * @param out    쓰기 대상 [OutputStream]
     */
    suspend fun suspendWrite(image: AwtImage, metadata: ImageMetadata, out: OutputStream) {
        withContext(Dispatchers.IO) {
            write(image, metadata, out)
        }
    }

    /**
     * Coroutines 방식으로 [image]를 [out]에 씁니다.
     *
     * ```kotlin
     * val writer = SuspendPngWriter.MaxCompression
     * val image = immutableImageOf(File("photo.png"))
     * val bos = ByteArrayOutputStream()
     * writer.suspendWrite(image, bos)
     * // bos.size() > 0
     * ```
     *
     * @param image  쓸 이미지
     * @param out    쓰기 대상 [OutputStream]
     */
    suspend fun suspendWrite(image: ImmutableImage, out: OutputStream) {
        suspendWrite(image, image.metadata, out)
    }

    /**
     * [image]를 [out]에 씁니다.
     *
     * ```kotlin
     * val writer = SuspendGifWriter.Default
     * val image = immutableImageOf(File("photo.gif"))
     * val bos = ByteArrayOutputStream()
     * writer.write(image, bos)
     * // bos.size() > 0
     * ```
     *
     * @param image  쓸 이미지
     * @param out    쓰기용 [OutputStream]
     */
    fun write(image: ImmutableImage, out: OutputStream) {
        write(image, image.metadata, out)
    }
}
