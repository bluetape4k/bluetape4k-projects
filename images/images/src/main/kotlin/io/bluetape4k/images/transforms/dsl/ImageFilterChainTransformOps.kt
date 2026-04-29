package io.bluetape4k.images.transforms.dsl

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.filters.dsl.ImageFilterChain
import io.bluetape4k.images.transforms.AspectRatio
import io.bluetape4k.images.transforms.ImagePoint
import io.bluetape4k.images.transforms.SaliencyStrategy
import io.bluetape4k.images.transforms.autoCrop
import io.bluetape4k.images.transforms.clahe
import io.bluetape4k.images.transforms.flipHorizontal
import io.bluetape4k.images.transforms.flipVertical
import io.bluetape4k.images.transforms.perspectiveTransform
import io.bluetape4k.images.transforms.rotateDegrees
import io.bluetape4k.images.transforms.smartCrop
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import java.awt.Color

private val log = KotlinLogging.logger {}

private inline fun ImageFilterChain.transformOp(
    name: String,
    crossinline block: (ImmutableImage) -> ImmutableImage,
) {
    addPixel { image ->
        try {
            block(image)
        } catch (e: Exception) {
            log.warn(e) { "[$name] failed: ${image.width}×${image.height}" }
            throw e
        }
    }
}

/**
 * 이미지의 배경색을 감지하여 불필요한 여백을 자동으로 제거합니다.
 *
 * @param tolerance 배경으로 판단하는 RGB 채널별 최대 편차 (0..255, 기본값 10).
 * @param padding 자른 후 각 방향에 추가할 여백 픽셀 수 (0 이상, 기본값 0).
 * @param backgroundColor 기준 배경색. `null` 이면 4개 모서리 평균으로 자동 감지.
 */
fun ImageFilterChain.autoCrop(
    tolerance: Int = 10,
    padding: Int = 0,
    backgroundColor: Color? = null,
) {
    transformOp("autoCrop") { image ->
        image.autoCrop(tolerance, padding, backgroundColor)
    }
}

/**
 * Saliency 기반으로 중요 영역을 중심으로 지정 종횡비로 잘라냅니다.
 *
 * @param aspectRatio 대상 종횡비 (예: [AspectRatio.WIDESCREEN]).
 * @param strategy saliency 계산 전략 (기본값: [SaliencyStrategy.SobelEnergy]).
 */
fun ImageFilterChain.smartCrop(
    aspectRatio: AspectRatio,
    strategy: SaliencyStrategy = SaliencyStrategy.SobelEnergy,
) {
    transformOp("smartCrop") { image ->
        image.smartCrop(aspectRatio, strategy)
    }
}

/**
 * 임의 각도로 이미지를 회전합니다. 투명 배경으로 확장된 캔버스를 반환합니다.
 *
 * @param angle 회전 각도 (도 단위, 양수 = 시계 방향).
 * @param background 확장 영역의 배경색 (기본값: 완전 투명).
 */
fun ImageFilterChain.rotateDegrees(
    angle: Double,
    background: Color = Color(0, 0, 0, 0),
) {
    transformOp("rotateDegrees") { image ->
        image.rotateDegrees(angle, background)
    }
}

/**
 * 이미지를 시계 반대 방향으로 90도 회전합니다 (scrimage `rotateLeft`).
 */
fun ImageFilterChain.rotateLeft() {
    transformOp("rotateLeft") { image ->
        image.rotateLeft()
    }
}

/**
 * 이미지를 시계 방향으로 90도 회전합니다 (scrimage `rotateRight`).
 */
fun ImageFilterChain.rotateRight() {
    transformOp("rotateRight") { image ->
        image.rotateRight()
    }
}

/**
 * 이미지를 수평으로 뒤집습니다 (좌우 반전).
 */
fun ImageFilterChain.flipHorizontal() {
    transformOp("flipHorizontal") { image ->
        image.flipHorizontal()
    }
}

/**
 * 이미지를 수직으로 뒤집습니다 (상하 반전).
 */
fun ImageFilterChain.flipVertical() {
    transformOp("flipVertical") { image ->
        image.flipVertical()
    }
}

/**
 * 4점 호모그래피를 이용하여 원근 변환을 수행합니다.
 *
 * @param sourceCorners 소스 이미지의 4개 꼭짓점 (topLeft, topRight, bottomRight, bottomLeft 순).
 * @param destinationCorners 출력 이미지의 4개 꼭짓점.
 * @param outputWidth 출력 이미지 너비 (픽셀).
 * @param outputHeight 출력 이미지 높이 (픽셀).
 * @param outsideColor 소스 이미지 외부에 매핑되는 영역의 색상 (기본값: 완전 투명).
 */
fun ImageFilterChain.perspectiveTransform(
    sourceCorners: List<ImagePoint>,
    destinationCorners: List<ImagePoint>,
    outputWidth: Int,
    outputHeight: Int,
    outsideColor: Color = Color(0, 0, 0, 0),
) {
    transformOp("perspectiveTransform") { image ->
        image.perspectiveTransform(sourceCorners, destinationCorners, outputWidth, outputHeight, outsideColor)
    }
}

/**
 * CLAHE (Contrast Limited Adaptive Histogram Equalization)로 이미지 대비를 향상시킵니다.
 *
 * @param tileSize 타일 크기 (픽셀 수, 기본값 8).
 * @param clipLimit 히스토그램 클리핑 임계값 (기본값 2.0).
 */
fun ImageFilterChain.clahe(
    tileSize: Int = 8,
    clipLimit: Double = 2.0,
) {
    transformOp("clahe") { image ->
        image.clahe(tileSize, clipLimit)
    }
}
