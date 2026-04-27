package io.bluetape4k.images.svg

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout
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
 * - SVG 스크립트 실행(`<script>`, `onload`)을 명시적으로 비활성화합니다.
 * - [SvgRasterizeOptions.timeoutMillis] 시간 초과 시 [kotlinx.coroutines.TimeoutCancellationException]을 발생시킵니다.
 * - [SvgRasterizeOptions.maxWidthPx] 및 [SvgRasterizeOptions.maxHeightPx]는 SVG 자체 치수에도 항상 적용됩니다.
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
    ): ImmutableImage = withTimeout(options.timeoutMillis) {
        runInterruptible(Dispatchers.IO) {
            rasterizeBlocking(input, options)
        }
    }

    private fun rasterizeBlocking(input: InputStream, options: SvgRasterizeOptions): ImmutableImage {
        val transcoder = buildTranscoder(options)
        val bos = ByteArrayOutputStream()
        val xmlReader = buildSecureXmlReader()

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

        // 항상 최대 치수 적용 (width/height 미지정 시 SVG 자체 선언 치수도 제한)
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_MAX_WIDTH, options.maxWidthPx.toFloat())
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_MAX_HEIGHT, options.maxHeightPx.toFloat())

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

        // SVG 스크립트 실행 방어: Batik 기본값에 의존하지 않고 명시적으로 비활성화
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_EXECUTE_ONLOAD, false)
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_ALLOWED_SCRIPT_TYPES, "")
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_CONSTRAIN_SCRIPT_ORIGIN, true)

        options.backgroundColor?.let { bg ->
            val argb = bg.rgb
            transcoder.addTranscodingHint(PNGTranscoder.KEY_BACKGROUND_COLOR, bg)
            transcoder.addTranscodingHint(PNGTranscoder.KEY_FORCE_TRANSPARENT_WHITE, argb == 0xFFFFFFFF.toInt())
        }

        return transcoder
    }

    private fun buildSecureXmlReader(): XMLReader {
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            isValidating = false
            // XXE 방어: 외부 엔티티/파라미터 엔티티는 allowExternalResources 무관 항상 차단
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            // DOCTYPE 선언 자체를 차단하여 billion-laughs DoS 방어
            // allowExternalResources=true여도 DOCTYPE은 독립적 공격 벡터이므로 항상 차단
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        return factory.newSAXParser().xmlReader
    }
}
