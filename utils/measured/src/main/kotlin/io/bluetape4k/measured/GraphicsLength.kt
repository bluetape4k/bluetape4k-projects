package io.bluetape4k.measured

/**
 * 그래픽/디스플레이 좌표계 길이 단위를 나타냅니다.
 */
open class GraphicsLength(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        /** 픽셀 단위입니다. */
        @JvmField val pixels: GraphicsLength = GraphicsLength("px")
    }
}

/**
 * 숫자를 픽셀 단위 측정값으로 변환합니다.
 */
fun Number.pixels(): Measure<GraphicsLength> = this * GraphicsLength.pixels
