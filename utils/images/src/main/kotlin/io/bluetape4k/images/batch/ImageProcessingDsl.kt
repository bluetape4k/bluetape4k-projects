package io.bluetape4k.images.batch

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.Position
import io.bluetape4k.images.coroutines.SuspendImageWriter
import io.bluetape4k.images.coroutines.SuspendJpegWriter
import io.bluetape4k.images.filters.WatermarkFilterType
import io.bluetape4k.images.filters.dsl.ImageFilterChain
import io.bluetape4k.images.filters.dsl.applyFilters
import io.bluetape4k.images.filters.dsl.gaussianBlur
import io.bluetape4k.images.filters.dsl.watermark
import io.bluetape4k.images.fonts.DEFAULT_FONT
import io.bluetape4k.images.transforms.SaliencyStrategy
import io.bluetape4k.images.transforms.smartCropTo
import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requirePositiveNumber
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font

/**
 * 배치 이미지 변환 단계를 선언하는 DSL입니다.
 */
class ImageProcessingDsl {
    private val transforms = mutableListOf<(ImmutableImage) -> ImmutableImage>()
    private var writer: SuspendImageWriter? = null

    /**
     * 이미지를 지정 크기로 리사이즈합니다.
     */
    fun resize(width: Int, height: Int) {
        width.requirePositiveNumber("width")
        height.requirePositiveNumber("height")
        transforms += { image -> image.scaleTo(width, height) }
    }

    /**
     * 이미지를 지정 크기에 맞춥니다.
     */
    fun fit(width: Int, height: Int) {
        width.requirePositiveNumber("width")
        height.requirePositiveNumber("height")
        transforms += { image -> image.fit(width, height) }
    }

    /**
     * 가우시안 블러 필터를 적용합니다.
     */
    fun gaussianBlur(radius: Int) {
        radius.requirePositiveNumber("radius")
        filters { gaussianBlur(radius) }
    }

    /**
     * 텍스트 워터마크를 적용합니다.
     */
    fun watermark(
        text: String,
        font: Font = DEFAULT_FONT,
        type: WatermarkFilterType = WatermarkFilterType.COVER,
        antiAlias: Boolean = true,
        alpha: Double = DEFAULT_TEXT_WATERMARK_ALPHA,
        color: Color = Color.WHITE,
    ) {
        filters { watermark(text, font, type, antiAlias, alpha, color) }
    }

    /**
     * 로고 이미지를 워터마크로 합성합니다.
     */
    fun watermark(
        logo: ImmutableImage,
        position: Position = Position.BottomRight,
        alpha: Float = DEFAULT_IMAGE_WATERMARK_ALPHA,
    ) {
        alpha.requireInRange(MIN_IMAGE_WATERMARK_ALPHA, MAX_IMAGE_WATERMARK_ALPHA, "alpha")
        transforms += { image -> image.overlayLogo(logo, position, alpha) }
    }

    /**
     * 살리언시 기반 스마트 크롭을 적용합니다.
     */
    fun smartCrop(
        width: Int,
        height: Int,
        strategy: SaliencyStrategy = SaliencyStrategy.SobelEnergy,
    ) {
        width.requirePositiveNumber("width")
        height.requirePositiveNumber("height")
        transforms += { image -> image.smartCropTo(width, height, strategy) }
    }

    /**
     * 필터 DSL 블록을 변환 단계로 추가합니다.
     */
    fun filters(block: ImageFilterChain.() -> Unit) {
        transforms += { image -> image.applyFilters(block) }
    }

    /**
     * JPEG writer를 선택합니다.
     */
    fun toJpeg(
        quality: Int = DEFAULT_JPEG_QUALITY,
        progressive: Boolean = false,
    ) {
        quality.requireInRange(JPEG_QUALITY_MIN, JPEG_QUALITY_MAX, "quality")
        writer(SuspendJpegWriter.Default.withCompression(quality).withProgressive(progressive))
    }

    /**
     * 결과 이미지 writer를 직접 선택합니다.
     */
    fun writer(writer: SuspendImageWriter) {
        check(this.writer == null) { "writer는 한 번만 선택할 수 있습니다." }
        this.writer = writer
    }

    internal fun apply(image: ImmutableImage): ImmutableImage =
        transforms.fold(image) { current, transform -> transform(current) }

    internal fun selectedWriter(): SuspendImageWriter? = writer

    private fun ImmutableImage.overlayLogo(
        logo: ImmutableImage,
        position: Position,
        alpha: Float,
    ): ImmutableImage {
        val target = copy()
        val x = position.calculateX(target.width, target.height, logo.width, logo.height).coerceAtLeast(0)
        val y = position.calculateY(target.width, target.height, logo.width, logo.height).coerceAtLeast(0)
        val graphics = target.awt().createGraphics()
        try {
            graphics.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
            graphics.drawImage(logo.awt(), x, y, null)
        } finally {
            graphics.dispose()
        }

        return target
    }

    private companion object {
        private const val DEFAULT_JPEG_QUALITY = 80
        private const val DEFAULT_TEXT_WATERMARK_ALPHA = 0.1
        private const val DEFAULT_IMAGE_WATERMARK_ALPHA = 1.0f
        private const val MIN_IMAGE_WATERMARK_ALPHA = 0.0f
        private const val MAX_IMAGE_WATERMARK_ALPHA = 1.0f
    }
}
