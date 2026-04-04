package io.bluetape4k.images.scaler

import io.bluetape4k.images.bufferedImageOf
import io.bluetape4k.images.drawRenderedImage
import io.bluetape4k.support.requirePositiveNumber
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

/**
 * 이미지를 지정된 [width], [height] 크기로 Scaling 한다
 *
 * ```kotlin
 * val image = bufferedImageOf(800, 600)
 * val scaled = image.scale(400, 300)
 * // scaled.width == 400
 * // scaled.height == 300
 * val proportionalScaled = image.scale(400, 400, proportional = true)
 * // proportionalScaled.width == 400 (비율 유지)
 * ```
 *
 * @param width        Scaled image의 width
 * @param height       Scaled image의 height
 * @param proportional 비례 적용 여부
 * @return scaled [BufferedImage]
 */
fun BufferedImage.scale(width: Int, height: Int, proportional: Boolean = true): BufferedImage {
    val xScale = width.toDouble() / this.width
    val yScale = height.toDouble() / this.height

    return if (proportional) {
        scale(xScale.coerceAtMost(yScale))
    } else {
        scale(xScale, yScale)
    }
}

/**
 * 원본 이미지를 [ratio] 만큼 scaling 을 수행합니다.
 *
 * ```kotlin
 * val image = bufferedImageOf(800, 600)
 * val scaled = image.scale(0.5)
 * // scaled.width == 400
 * // scaled.height == 300
 * ```
 *
 * @param ratio scaling 할 비율
 * @return Scaled [BufferedImage]
 */
fun BufferedImage.scale(ratio: Double): BufferedImage {
    ratio.requirePositiveNumber("ratio")

    val w = (this.width * ratio).toInt()
    val h = (this.height * ratio).toInt()
    val xScale = w.toDouble() / this.width
    val yScale = h.toDouble() / this.height

    return scale(xScale, yScale)
}

/**
 * 이미지를 [xScale], [yScale] 비율에 따라 Scaling 한다
 *
 * ```kotlin
 * val image = bufferedImageOf(800, 600)
 * val scaled = image.scale(0.5, 0.75)
 * // scaled.width == 400
 * // scaled.height == 450
 * ```
 *
 * @param xScale x 축에 대한 scaling 비율
 * @param yScale y 축에 대한 scaling 비율
 * @return scaled [BufferedImage]
 */
fun BufferedImage.scale(xScale: Double, yScale: Double): BufferedImage {
    val transform = AffineTransform.getScaleInstance(xScale, yScale)
    val w = (this.width * xScale).toInt()
    val h = (this.height * yScale).toInt()

    return bufferedImageOf(w, h).also { scaled ->
        scaled.drawRenderedImage(this@scale, transform)
    }
}
