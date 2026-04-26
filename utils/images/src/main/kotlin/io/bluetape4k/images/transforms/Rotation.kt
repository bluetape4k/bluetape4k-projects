package io.bluetape4k.images.transforms

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.angles.Radians
import io.bluetape4k.logging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

private val log = KotlinLogging.logger {}

/**
 * 이미지를 지정한 각도(도 단위)만큼 회전합니다.
 *
 * 90, 180, 270도의 경우 scrimage 네이티브 메서드([rotateRight], [rotate], [rotateLeft])를 사용합니다.
 * 임의의 각도인 경우 [AffineTransform]으로 회전하며, 회전 후 이미지가 잘리지 않도록 캔버스를 자동 확장합니다.
 *
 * scrimage는 이미 bounds를 자동 확장합니다. 이 함수는 도(degree) 단위 편의 API와 투명 기본 배경을 제공합니다.
 *
 * ```kotlin
 * val rotated = image.rotateDegrees(45.0)
 * // 45도 회전된 새 이미지 반환 (배경은 투명)
 *
 * val rotatedRed = image.rotateDegrees(30.0, background = Color.RED)
 * // 30도 회전, 빈 영역은 빨간색으로 채움
 * ```
 *
 * @param angle      회전 각도 (도 단위, 시계 방향)
 * @param background 회전 후 빈 영역을 채울 배경색 (기본값: 투명)
 * @return 회전된 새 [ImmutableImage]
 */
fun ImmutableImage.rotateDegrees(angle: Double, background: Color = Color(0, 0, 0, 0)): ImmutableImage {
    val normalized = ((angle % 360) + 360) % 360

    if (Math.abs(normalized % 90) < 1e-9) {
        return when (Math.round(normalized).toInt()) {
            0 -> this
            90 -> rotateRight()
            180 -> rotate(Radians(Math.PI))
            270 -> rotateLeft()
            else -> this
        }
    }

    val radians = Math.toRadians(angle)
    val cos = Math.abs(Math.cos(radians))
    val sin = Math.abs(Math.sin(radians))
    val newW = Math.ceil(width * cos + height * sin).toInt()
    val newH = Math.ceil(width * sin + height * cos).toInt()

    val buf = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
    val g = buf.createGraphics()
    try {
        g.color = background
        g.fillRect(0, 0, newW, newH)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        val transform = AffineTransform()
        transform.translate(newW / 2.0, newH / 2.0)
        transform.rotate(radians)
        transform.translate(-width / 2.0, -height / 2.0)
        g.drawImage(this.awt(), transform, null)
    } finally {
        g.dispose()
    }

    return ImmutableImage.wrapAwt(buf)
}

/**
 * 이미지를 좌우 반전합니다.
 *
 * scrimage의 [flipX]를 래핑하는 편의 함수입니다.
 *
 * ```kotlin
 * val flipped = image.flipHorizontal()
 * // 좌우 반전된 새 이미지 반환
 * ```
 *
 * @return 좌우 반전된 새 [ImmutableImage]
 */
fun ImmutableImage.flipHorizontal(): ImmutableImage = flipX()

/**
 * 이미지를 상하 반전합니다.
 *
 * scrimage의 [flipY]를 래핑하는 편의 함수입니다.
 *
 * ```kotlin
 * val flipped = image.flipVertical()
 * // 상하 반전된 새 이미지 반환
 * ```
 *
 * @return 상하 반전된 새 [ImmutableImage]
 */
fun ImmutableImage.flipVertical(): ImmutableImage = flipY()

/**
 * Coroutines 환경에서 이미지를 지정한 각도(도 단위)만큼 회전합니다.
 *
 * [rotateDegrees]를 [Dispatchers.Default]에서 비동기로 실행합니다.
 *
 * scrimage는 이미 bounds를 자동 확장합니다. 이 함수는 도(degree) 단위 편의 API와 투명 기본 배경을 제공합니다.
 *
 * ```kotlin
 * val rotated = image.suspendRotateDegrees(45.0)
 * // Coroutines 환경에서 45도 회전된 새 이미지 반환
 *
 * val rotatedWithBg = image.suspendRotateDegrees(30.0, background = Color(255, 255, 255, 128))
 * // 30도 회전, 빈 영역은 반투명 흰색으로 채움
 * ```
 *
 * @param angle      회전 각도 (도 단위, 시계 방향)
 * @param background 회전 후 빈 영역을 채울 배경색 (기본값: 투명)
 * @return 회전된 새 [ImmutableImage]
 */
suspend fun ImmutableImage.suspendRotateDegrees(
    angle: Double,
    background: Color = Color(0, 0, 0, 0),
): ImmutableImage = withContext(Dispatchers.Default) {
    rotateDegrees(angle, background)
}
