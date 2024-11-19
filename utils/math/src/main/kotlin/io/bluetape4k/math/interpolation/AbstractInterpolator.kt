package io.bluetape4k.math.interpolation

import io.bluetape4k.logging.KLogging
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator

abstract class AbstractInterpolator: Interpolator {

    companion object: KLogging() {
        const val MINIMUM_SIZE = 5
    }

    protected abstract val apacheInterpolator: UnivariateInterpolator

    /**
     * X, Y 변량에 따른 함수를 보간하는 함수를 반환합니다.
     *
     * @param xs
     * @param ys
     * @return
     */
    override fun interpolate(xs: DoubleArray, ys: DoubleArray): (Double) -> Double {
        val interpolationFunc = apacheInterpolator.interpolate(xs, ys)
        return { x: Double -> interpolationFunc.value(x) }
    }
}
