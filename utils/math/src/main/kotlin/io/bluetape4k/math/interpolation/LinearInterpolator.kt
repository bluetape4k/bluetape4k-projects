package io.bluetape4k.math.interpolation

import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator

typealias ApacheLinearInterpolator = org.apache.commons.math3.analysis.interpolation.LinearInterpolator

class LinearInterpolator: AbstractInterpolator() {
    override val apacheInterpolator: UnivariateInterpolator = ApacheLinearInterpolator()

}
