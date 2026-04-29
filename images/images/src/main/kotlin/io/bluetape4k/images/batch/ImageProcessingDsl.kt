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
 *
 * [processImages] 또는 [processImageFiles]의 람다 블록으로 사용하며,
 * 호출 순서대로 변환이 파이프라인에 추가됩니다. 마지막으로 `toJpeg()` 같은
 * writer 설정을 호출해야 결과를 파일로 저장할 수 있습니다.
 *
 * ```kotlin
 * // 기본 사용 예시: 리사이즈 → 가우시안 블러 → JPEG 저장
 * flowOf(imagePath)
 *     .processImages {
 *         resize(1024, 768)
 *         gaussianBlur(radius = 3)
 *         toJpeg(quality = 85)
 *     }
 *     .writeImagesTo(outputDir)
 *
 * // 워터마크 텍스트 + 스마트 크롭 + JPEG 저장
 * flowOf(imagePath)
 *     .processImages {
 *         smartCrop(width = 800, height = 600)
 *         watermark("© 2025 Bluetape4k")
 *         toJpeg(quality = 90, progressive = true)
 *     }
 *     .writeImagesTo(outputDir)
 * ```
 */
class ImageProcessingDsl {
    private val transforms = mutableListOf<(ImmutableImage) -> ImmutableImage>()
    private var writer: SuspendImageWriter? = null

    /**
     * 이미지를 지정 크기로 리사이즈합니다.
     *
     * 원본 비율을 유지하지 않고 [width] × [height]로 강제 스케일합니다.
     * 비율을 유지하면서 맞추려면 [fit]을 사용하세요.
     *
     * ```kotlin
     * flowOf(imagePath).processImages {
     *     resize(1920, 1080)
     *     toJpeg(quality = 90)
     * }
     * ```
     *
     * @param width 출력 너비 (픽셀, 양수)
     * @param height 출력 높이 (픽셀, 양수)
     */
    fun resize(width: Int, height: Int) {
        width.requirePositiveNumber("width")
        height.requirePositiveNumber("height")
        transforms += { image -> image.scaleTo(width, height) }
    }

    /**
     * 이미지를 원본 비율을 유지하면서 [width] × [height] 경계에 맞춥니다.
     *
     * 경계를 벗어나지 않는 최대 크기로 축소·확대하므로 이미지가 잘리지 않습니다.
     * 비율 무시 강제 스케일링은 [resize]를 사용하세요.
     *
     * ```kotlin
     * flowOf(imagePath).processImages {
     *     fit(800, 600)
     *     toJpeg(quality = 85)
     * }
     * ```
     *
     * @param width 경계 너비 (픽셀, 양수)
     * @param height 경계 높이 (픽셀, 양수)
     */
    fun fit(width: Int, height: Int) {
        width.requirePositiveNumber("width")
        height.requirePositiveNumber("height")
        transforms += { image -> image.fit(width, height) }
    }

    /**
     * 가우시안 블러 필터를 적용합니다.
     *
     * [radius]가 클수록 더 강한 블러 효과가 적용됩니다.
     *
     * ```kotlin
     * flowOf(imagePath).processImages {
     *     gaussianBlur(radius = 5)
     *     toJpeg(quality = 80)
     * }
     * ```
     *
     * @param radius 블러 반경 (픽셀, 양수)
     */
    fun gaussianBlur(radius: Int) {
        radius.requirePositiveNumber("radius")
        filters { gaussianBlur(radius) }
    }

    /**
     * 텍스트 워터마크를 이미지에 적용합니다.
     *
     * [type]으로 워터마크 배치 방식을 선택하고, [alpha]로 투명도를 조절합니다.
     * 기본값은 흰색 반투명 전면 커버 워터마크입니다.
     *
     * ```kotlin
     * flowOf(imagePath).processImages {
     *     watermark(
     *         text = "© 2025 My Company",
     *         alpha = 0.15,
     *         color = Color.WHITE,
     *     )
     *     toJpeg(quality = 85)
     * }
     * ```
     *
     * @param text 워터마크로 표시할 텍스트
     * @param font 텍스트 폰트 (기본값: [DEFAULT_FONT])
     * @param type 워터마크 배치 방식 ([WatermarkFilterType])
     * @param antiAlias 안티앨리어싱 적용 여부
     * @param alpha 텍스트 투명도 (0.0 ~ 1.0)
     * @param color 텍스트 색상 (기본값: 흰색)
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
     *
     * [logo]를 [position] 위치에 [alpha] 투명도로 오버레이합니다.
     * 텍스트 대신 이미지 로고를 삽입할 때 사용합니다.
     *
     * ```kotlin
     * val logo = ImmutableImage.loader().fromFile(File("logo.png"))
     *
     * flowOf(imagePath).processImages {
     *     watermark(
     *         logo = logo,
     *         position = Position.BottomRight,
     *         alpha = 0.8f,
     *     )
     *     toJpeg(quality = 90)
     * }
     * ```
     *
     * @param logo 워터마크로 사용할 로고 이미지
     * @param position 로고를 배치할 위치 (기본값: 우측 하단)
     * @param alpha 로고 불투명도 (0.0f ~ 1.0f, 기본값: 1.0f)
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
     * 살리언시(Saliency) 기반 스마트 크롭을 적용합니다.
     *
     * [strategy] 알고리즘으로 이미지의 중요 영역을 탐지하여 [width] × [height] 크기로
     * 가장 의미 있는 부분을 잘라냅니다. 기본 전략은 [SaliencyStrategy.SobelEnergy]입니다.
     *
     * ```kotlin
     * flowOf(imagePath).processImages {
     *     smartCrop(
     *         width = 400,
     *         height = 400,
     *         strategy = SaliencyStrategy.SobelEnergy,
     *     )
     *     toJpeg(quality = 88)
     * }
     * ```
     *
     * @param width 크롭 후 너비 (픽셀, 양수)
     * @param height 크롭 후 높이 (픽셀, 양수)
     * @param strategy 살리언시 탐지 전략 (기본값: [SaliencyStrategy.SobelEnergy])
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
     *
     * 이 함수를 호출하면 변환 결과가 [ImageBatchResult.WritableImage]로 감싸져
     * [writeImagesTo]로 파일 저장이 가능해집니다. writer는 한 번만 설정할 수 있습니다.
     *
     * ```kotlin
     * flowOf(imagePath).processImages {
     *     resize(1280, 720)
     *     // quality 0~100, progressive = true 이면 점진적 로딩 JPEG
     *     toJpeg(quality = 85, progressive = true)
     * }
     * ```
     *
     * @param quality JPEG 압축 품질 (0 ~ 100, 기본값: 80)
     * @param progressive `true`이면 프로그레시브 JPEG로 인코딩
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
