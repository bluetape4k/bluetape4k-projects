package io.bluetape4k.measured

import io.bluetape4k.measured.GraphicsLength.Companion.pixels


/**
 * 그래픽/디스플레이 좌표계 길이 단위를 나타냅니다.
 *
 * ## 동작/계약
 * - 기준 단위는 픽셀([pixels])입니다.
 * - 디스플레이 좌표 연산 시 픽셀 단위 [Measure]를 생성합니다.
 *
 * ```kotlin
 * val width = 1920.pixels()
 * // width.toString() == "1920.0 px"
 * ```
 */
open class GraphicsLength(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        /** 픽셀 단위입니다. */
        @JvmField
        val pixels: GraphicsLength = GraphicsLength("px")
    }
}

/**
 * 숫자를 픽셀 단위 측정값으로 변환합니다.
 *
 * ```kotlin
 * val value = 1920.pixels()
 * // value.toString() == "1920.0 px"
 * // value.amount == 1920.0
 * ```
 */
fun Number.pixels(): Measure<GraphicsLength> = this * GraphicsLength.pixels
