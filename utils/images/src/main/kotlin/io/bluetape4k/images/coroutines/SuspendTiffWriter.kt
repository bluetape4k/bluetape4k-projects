package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.nio.ImageWriter
import io.bluetape4k.images.IIORegistryUtils
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream
import java.io.OutputStream

/**
 * TIFF 형식의 이미지를 생성하는 [ImageWriter] + [SuspendImageWriter] 구현체입니다.
 *
 * TwelveMonkeys ImageIO TIFF 라이브러리를 사용하여 단일 페이지 TIFF 파일을 작성합니다.
 * 다중 페이지 TIFF는 [SuspendTiffMultiPageWriter]를 사용하세요.
 *
 * ## 동작/계약
 * - `compression`은 [TiffCompression] 중 하나를 선택합니다. 기본값은 [TiffCompression.DEFLATE]입니다.
 * - `quality`는 0.0 ~ 1.0 범위의 Float로, [TiffCompression.JPEG] 압축 시에만 적용됩니다.
 * - 초기화 시 TwelveMonkeys TIFF SPI를 IIORegistry에 강제 등록합니다.
 * - [SuspendImageWriter.suspendWrite]는 [Dispatchers.IO] + [runInterruptible]로 실행되어 코루틴 취소를 전파합니다.
 *
 * ```kotlin
 * val writer = SuspendTiffWriter.Default
 * val image = immutableImageOf(File("photo.jpg"))
 * val bos = ByteArrayOutputStream()
 * writer.suspendWrite(image, bos)
 * // bos.size() > 0 → TIFF 바이너리 데이터
 * ```
 *
 * @property compression TIFF 압축 방식 (기본값: [TiffCompression.DEFLATE])
 * @property quality     JPEG 압축 품질 (0.0~1.0, 기본값: 0.9f)
 *
 * @see SuspendTiffMultiPageWriter
 * @see TiffCompression
 */
class SuspendTiffWriter(
    val compression: TiffCompression = TiffCompression.DEFLATE,
    val quality: Float = 0.9f,
) : ImageWriter, SuspendImageWriter {

    init {
        require(quality in 0.0f..1.0f) { "quality must be in 0.0..1.0: $quality" }
    }

    companion object : KLoggingChannel() {
        init {
            IIORegistryUtils.registerApplicationClasspathSpis()
        }

        @JvmStatic
        val Default = SuspendTiffWriter()

        @JvmStatic
        val Lzw = SuspendTiffWriter(TiffCompression.LZW)

        @JvmStatic
        val Uncompressed = SuspendTiffWriter(TiffCompression.NONE)

        @JvmStatic
        val JpegCompression = SuspendTiffWriter(TiffCompression.JPEG, quality = 0.9f)
    }

    /**
     * Coroutines 방식으로 [image]를 TIFF 형식으로 [out]에 씁니다.
     *
     * [runInterruptible]로 래핑되어 코루틴 취소를 전파합니다.
     */
    override suspend fun suspendWrite(image: AwtImage, metadata: ImageMetadata, out: OutputStream) {
        runInterruptible(Dispatchers.IO) { write(image, metadata, out) }
    }

    /**
     * [image]를 TIFF 형식으로 [out]에 씁니다. (블로킹 구현)
     *
     * ## 동작/계약
     * - TwelveMonkeys ImageIO TIFF writer가 없는 경우 [IllegalStateException]을 발생시킵니다.
     * - writer는 `finally` 블록에서 반드시 [javax.imageio.ImageWriter.dispose]됩니다.
     *
     * @throws java.io.IOException TIFF 쓰기 실패 시
     * @throws IllegalStateException TwelveMonkeys TIFF writer를 찾을 수 없는 경우
     */
    override fun write(image: AwtImage, metadata: ImageMetadata, out: OutputStream) {
        val type = ImageTypeSpecifier.createFromBufferedImageType(image.awt().type)
        val writers = ImageIO.getImageWriters(type, "tiff")
        check(writers.hasNext()) {
            "TIFF ImageWriter를 찾을 수 없습니다. TwelveMonkeys ImageIO TIFF 라이브러리가 클래스패스에 있는지 확인하세요."
        }

        val writer = writers.next()
        val param = writer.defaultWriteParam

        if (param.canWriteCompressed()) {
            param.compressionMode = ImageWriteParam.MODE_EXPLICIT
            param.compressionType = compression.ioName
            if (compression == TiffCompression.JPEG && param.canWriteCompressed()) {
                param.compressionQuality = quality
            }
        }

        val ios = MemoryCacheImageOutputStream(out)
        try {
            writer.output = ios
            writer.write(null, IIOImage(image.awt(), null, null), param)
            ios.flush()
        } finally {
            // dispose()가 예외를 던져도 ios.close()가 반드시 실행되도록 분리
            // CancellationException은 반드시 재전파하여 코루틴 취소 계약을 보존
            try { writer.dispose() } catch (e: Throwable) {
                if (e is CancellationException) throw e
                log.warn("TIFF writer.dispose() 실패", e)
            }
            try { ios.close() } catch (e: Throwable) {
                if (e is CancellationException) throw e
                log.warn("TIFF ios.close() 실패", e)
            }
        }
    }
}
