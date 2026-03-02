package io.bluetape4k.math.interpolation

/**
 * `(x, y)` 샘플 점으로 보간 함수를 생성하는 계약입니다.
 *
 * ## 동작/계약
 * - 구현체는 샘플 배열을 받아 `Double -> Double` 함수를 반환합니다.
 * - 입력 검증(길이 동일성, 최소 점 개수, 정렬 여부)은 구현체에서 수행합니다.
 *
 * ```kotlin
 * val f = interpolator.interpolate(doubleArrayOf(0.0, 1.0), doubleArrayOf(0.0, 1.0))
 * // f(0.5) == 0.5
 * ```
 */
fun interface Interpolator {

    /**
     * X, Y 변량에 따른 함수를 보간하는 함수를 반환합니다.
     *
     * ## 동작/계약
     * - [xs], [ys]의 인덱스는 동일 점을 나타내야 합니다.
     * - 반환 함수는 주어진 샘플 기반 보간값을 계산합니다.
     *
     * @param xs x 좌표 배열
     * @param ys y 좌표 배열
     *
     * ```kotlin
     * val fn = interpolator.interpolate(xs, ys)
     * val y = fn(2.0)
     * // y == [보간된 값]
     * ```
     */
    fun interpolate(xs: DoubleArray, ys: DoubleArray): (Double) -> Double

}
