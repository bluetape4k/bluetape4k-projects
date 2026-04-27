package io.bluetape4k.images.svg

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.apache.batik.transcoder.SVGAbstractTranscoder
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import org.xml.sax.XMLReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

/**
 * Apache Batik 기반 SVG 래스터라이저 ([SuspendSvgRasterizer]) 구현체입니다.
 *
 * ## 동작/계약
 * - 외부 리소스 접근은 기본적으로 금지됩니다 ([SvgRasterizeOptions.allowExternalResources] = `false`).
 * - XML 외부 엔티티 공격(XXE)을 방어하기 위해 SAX 파서를 하드닝합니다.
 * - 모든 블로킹 I/O는 [Dispatchers.IO] + [runInterruptible]로 래핑되어 코루틴 취소를 전파합니다.
 * - [SvgRasterizeOptions.maxWidthPx] 및 [SvgRasterizeOptions.maxHeightPx]를 초과하는 치수는 예외를 발생시킵니다.
 * - Batik에 직접 의존합니다. 이 클래스를 사용하려면 `batik-transcoder` 의존성이 클래스패스에 있어야 합니다.
 *
 * ```kotlin
 * val rasterizer = BatikSvgRasterizer()
 * File("diagram.svg").inputStream().use { svg ->
 *     val image = rasterizer.rasterize(svg, SvgRasterizeOptions(width = 800))
 *     image.output(PngWriter.MaxCompression, File("diagram.png"))
 * }
 * ```
 *
 * @see SuspendSvgRasterizer
 * @see SvgRasterizeOptions
 */
class BatikSvgRasterizer : SuspendSvgRasterizer {

    companion object : KLoggingChannel()

    override suspend fun rasterize(
        input: InputStream,
        options: SvgRasterizeOptions,
    ): ImmutableImage = runInterruptible(Dispatchers.IO) {
        rasterizeBlocking(input, options)
    }

    private fun rasterizeBlocking(input: InputStream, options: SvgRasterizeOptions): ImmutableImage {
        val transcoder = buildTranscoder(options)
        val bos = ByteArrayOutputStream()
        val xmlReader = buildSecureXmlReader(options)

        val transcoderInput = TranscoderInput().apply {
            setXMLReader(xmlReader)
            setInputStream(input)
        }
        val transcoderOutput = TranscoderOutput(bos)

        transcoder.transcode(transcoderInput, transcoderOutput)

        log.debug { "SVG 래스터화 완료: ${bos.size()} bytes" }

        return ImmutableImage.loader().fromBytes(bos.toByteArray())
    }

    private fun buildTranscoder(options: SvgRasterizeOptions): PNGTranscoder {
        val transcoder = PNGTranscoder()

        options.width?.let { w ->
            require(w <= options.maxWidthPx) {
                "width($w)이 maxWidthPx(${options.maxWidthPx})를 초과합니다."
            }
            transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, w.toFloat())
        }
        options.height?.let { h ->
            require(h <= options.maxHeightPx) {
                "height($h)이 maxHeightPx(${options.maxHeightPx})를 초과합니다."
            }
            transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, h.toFloat())
        }
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 25.4f / options.dpi)

        // XXE/SSRF 방어: 외부 리소스 접근 금지 (기본값)
        transcoder.addTranscodingHint(
            SVGAbstractTranscoder.KEY_ALLOW_EXTERNAL_RESOURCES,
            options.allowExternalResources
        )

        options.backgroundColor?.let { bg ->
            val argb = bg.rgb
            transcoder.addTranscodingHint(PNGTranscoder.KEY_BACKGROUND_COLOR, bg)
            transcoder.addTranscodingHint(PNGTranscoder.KEY_FORCE_TRANSPARENT_WHITE, argb == 0xFFFFFFFF.toInt())
        }

        return transcoder
    }

    private fun buildSecureXmlReader(options: SvgRasterizeOptions): XMLReader {
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            isValidating = false
            // XXE 방어
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", !options.allowExternalResources)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }
        return factory.newSAXParser().xmlReader
    }
}
