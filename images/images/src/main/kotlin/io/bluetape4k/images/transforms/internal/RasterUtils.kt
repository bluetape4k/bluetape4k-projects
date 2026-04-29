package io.bluetape4k.images.transforms.internal

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.KotlinLogging
import java.awt.Color
import java.awt.image.BufferedImage

private val log = KotlinLogging.logger {}

/**
 * 변환 결과 이미지의 최대 픽셀 수.
 *
 * 64M pixels (약 256MB ARGB 메모리 사용량) 까지를 안전 한계로 제한합니다.
 * 변환 입력이 이 값을 초과하면 OOM 위험이 있으므로 호출 측에서 사전 검증해야 합니다.
 */
internal const val MAX_OUTPUT_PIXELS: Long = 67_108_864L

/**
 * [ImmutableImage] 의 내부 [BufferedImage] 를 [BufferedImage.TYPE_INT_ARGB] 형식으로 반환합니다.
 *
 * 이미 `TYPE_INT_ARGB` 인 경우 원본을 그대로 반환하고, 그렇지 않으면 새 ARGB 버퍼를 만들어
 * `Graphics2D` 로 그려서 반환합니다.
 *
 * @return ARGB 포맷의 [BufferedImage].
 */
internal fun ImmutableImage.toIntArgb(): BufferedImage {
    val src = awt()
    if (src.type == BufferedImage.TYPE_INT_ARGB) {
        return src
    }
    val dst = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_ARGB)
    val g = dst.createGraphics()
    try {
        g.drawImage(src, 0, 0, null)
    } finally {
        g.dispose()
    }
    return dst
}

/**
 * 동일한 너비와 높이를 가진 [BufferedImage.TYPE_INT_ARGB] 사본을 생성합니다.
 *
 * 원본 이미지를 보존한 채 픽셀 단위 작업을 수행해야 할 때 사용합니다.
 *
 * @return 새로 생성된 ARGB 사본.
 */
internal fun BufferedImage.copyArgb(): BufferedImage {
    val dst = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = dst.createGraphics()
    try {
        g.drawImage(this, 0, 0, null)
    } finally {
        g.dispose()
    }
    return dst
}

/**
 * 이미지 전체를 지정한 [color] 로 채웁니다.
 *
 * `Graphics2D.setBackground` + `clearRect` 를 사용하므로 알파 채널을 보존하여
 * 투명한 색으로 채울 수도 있습니다. 원본을 변경(mutating)하므로 internal 사용에 한정합니다.
 *
 * @param color 채울 색상.
 * @return 같은 [BufferedImage] 인스턴스 (체이닝 용도).
 */
internal fun BufferedImage.fillColor(color: Color): BufferedImage {
    val g = createGraphics()
    try {
        g.background = color
        g.clearRect(0, 0, width, height)
    } finally {
        g.dispose()
    }
    return this
}

/**
 * 이미지 전체 픽셀을 ARGB packed `Int` 배열로 읽어옵니다.
 *
 * 길이는 `width * height` 이며, row-major 순서로 저장됩니다.
 *
 * @return ARGB 픽셀 배열.
 */
internal fun BufferedImage.getArgbPixels(): IntArray =
    getRGB(0, 0, width, height, IntArray(width * height), 0, width)

/**
 * ARGB packed `Int` 배열을 이미지 전체 픽셀로 기록합니다.
 *
 * 배열 길이는 `width * height` 와 일치해야 하며, row-major 순서를 가정합니다.
 *
 * @param pixels 기록할 ARGB 픽셀 배열.
 * @return 같은 [BufferedImage] 인스턴스 (체이닝 용도).
 */
internal fun BufferedImage.setArgbPixels(pixels: IntArray): BufferedImage {
    setRGB(0, 0, width, height, pixels, 0, width)
    return this
}

/**
 * Alpha/Red/Green/Blue 4채널을 packed ARGB `Int` 로 결합합니다.
 *
 * 각 채널은 하위 8비트만 사용하며, 8비트를 초과하는 비트는 잘립니다.
 *
 * @param a Alpha 채널 (0..255).
 * @param r Red 채널 (0..255).
 * @param g Green 채널 (0..255).
 * @param b Blue 채널 (0..255).
 * @return packed ARGB `Int`.
 */
internal inline fun argb(a: Int, r: Int, g: Int, b: Int): Int =
    (a and 0xFF shl 24) or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)

/** packed ARGB `Int` 의 alpha 채널(0..255) 을 추출합니다. */
internal inline fun Int.alphaComponent(): Int = (this ushr 24) and 0xFF

/** packed ARGB `Int` 의 red 채널(0..255) 을 추출합니다. */
internal inline fun Int.redComponent(): Int = (this ushr 16) and 0xFF

/** packed ARGB `Int` 의 green 채널(0..255) 을 추출합니다. */
internal inline fun Int.greenComponent(): Int = (this ushr 8) and 0xFF

/** packed ARGB `Int` 의 blue 채널(0..255) 을 추출합니다. */
internal inline fun Int.blueComponent(): Int = this and 0xFF
