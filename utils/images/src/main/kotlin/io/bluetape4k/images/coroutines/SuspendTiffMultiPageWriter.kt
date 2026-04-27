package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.ImmutableImage
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
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * 복수의 이미지 페이지를 단일 TIFF 파일로 기록하는 [SuspendMultiPageImageWriter] 구현체입니다.
 *
 * TwelveMonkeys ImageIO TIFF 라이브러리를 사용하여 다중 페이지 TIFF 파일을 작성합니다.
 *
 * ## 동작/계약
 * - [suspendWrite]는 `images` 리스트를 순서대로 단일 TIFF 파일의 다중 IFD로 씁니다.
 * - [maxPages] 초과 시 [IllegalArgumentException]을 발생시킵니다.
 * - 각 페이지의 픽셀 수([maxPixelsPerPage]) 초과 시 [IllegalArgumentException]을 발생시킵니다.
 * - 블로킹 I/O는 [Dispatchers.IO] + [runInterruptible]로 래핑되어 코루틴 취소를 전파합니다.
 * - writer는 `finally` 블록에서 반드시 [javax.imageio.ImageWriter.dispose]됩니다.
 * - 쓰기 중 예외 발생 시 `out`에 부분 데이터가 남지 않습니다 (내부 버퍼에서 완성 후 복사).
 *
 * ```kotlin
 * val writer = SuspendTiffMultiPageWriter()
 * val images = listOf(page1, page2, page3)
 * val bos = ByteArrayOutputStream()
 * writer.suspendWrite(images, bos)
 * // bos.size() > 0 → 3-페이지 TIFF 바이너리 데이터
 * ```
 *
 * @property compression     TIFF 압축 방식 (기본값: [TiffCompression.DEFLATE])
 * @property maxPages        허용할 최대 페이지 수 (기본값: 1024, 리소스 폭탄 방어)
 * @property maxPixelsPerPage 페이지당 최대 픽셀 수 (기본값: 100_000_000, 리소스 폭탄 방어)
 *
 * @see SuspendTiffWriter
 * @see SuspendMultiPageImageWriter
 */
class SuspendTiffMultiPageWriter(
    val compression: TiffCompression = TiffCompression.DEFLATE,
    val maxPages: Int = 1024,
    val maxPixelsPerPage: Long = 100_000_000L,
) : SuspendMultiPageImageWriter {

    companion object : KLoggingChannel() {
        init {
            IIORegistryUtils.registerApplicationClasspathSpis()
        }

        @JvmStatic
        val Default = SuspendTiffMultiPageWriter()
    }

    /**
     * [images] 리스트를 단일 다중 페이지 TIFF 스트림으로 씁니다.
     *
     * @param images 쓸 이미지 리스트
     * @param out    쓰기 대상 [OutputStream]
     * @throws IllegalArgumentException 페이지 수 또는 픽셀 수 제한 초과 시
     * @throws java.io.IOException TIFF 쓰기 실패 시
     * @throws IllegalStateException TwelveMonkeys TIFF writer를 찾을 수 없는 경우
     */
    override suspend fun suspendWrite(images: List<ImmutableImage>, out: OutputStream) {
        require(images.isNotEmpty()) { "images 리스트가 비어 있습니다." }
        require(images.size <= maxPages) {
            "페이지 수(${images.size})가 maxPages($maxPages)를 초과합니다."
        }
        images.forEachIndexed { idx, img ->
            val pixels = img.width.toLong() * img.height.toLong()
            require(pixels <= maxPixelsPerPage) {
                "페이지 $idx 의 픽셀 수($pixels)가 maxPixelsPerPage($maxPixelsPerPage)를 초과합니다."
            }
        }

        runInterruptible(Dispatchers.IO) {
            writeBlocking(images, out)
        }
    }

    private fun writeBlocking(images: List<ImmutableImage>, out: OutputStream) {
        val firstImage = images.first()
        val type = ImageTypeSpecifier.createFromBufferedImageType(firstImage.awt().type)
        val writers = ImageIO.getImageWriters(type, "tiff")
        check(writers.hasNext()) {
            "TIFF ImageWriter를 찾을 수 없습니다. TwelveMonkeys ImageIO TIFF 라이브러리가 클래스패스에 있는지 확인하세요."
        }

        val writer = writers.next()
        val param = writer.defaultWriteParam

        if (param.canWriteCompressed()) {
            param.compressionMode = ImageWriteParam.MODE_EXPLICIT
            param.compressionType = compression.ioName
        }

        // 부분 출력 오염 방지: 전체 시퀀스를 내부 버퍼에 완성한 후 out에 복사
        val buffer = ByteArrayOutputStream()
        val ios = MemoryCacheImageOutputStream(buffer)
        try {
            writer.output = ios
            writer.prepareWriteSequence(null)
            for (image in images) {
                writer.writeToSequence(IIOImage(image.awt(), null, null), param)
            }
            writer.endWriteSequence()
            ios.flush()
        } finally {
            // dispose()가 예외를 던져도 ios.close()가 반드시 실행되도록 분리
            // CancellationException은 반드시 재전파하여 runInterruptible 취소 계약을 보존
            try { writer.dispose() } catch (e: Throwable) {
                if (e is CancellationException) throw e
                log.warn("TIFF writer.dispose() 실패", e)
            }
            try { ios.close() } catch (e: Throwable) {
                if (e is CancellationException) throw e
                log.warn("TIFF ios.close() 실패", e)
            }
        }
        // 예외 없이 시퀀스가 완성된 경우에만 out으로 복사
        buffer.writeTo(out)
    }
}
