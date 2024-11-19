package io.bluetape4k.math.interpolation

/**
 * 보간을 수행하는 인터페이스
 */
fun interface Interpolator {

    /**
     * X, Y 변량에 따른 함수를 보간하는 함수를 반환합니다.
     *
     * @param xs
     * @param ys
     * @return
     */
    fun interpolate(xs: DoubleArray, ys: DoubleArray): (Double) -> Double

}
