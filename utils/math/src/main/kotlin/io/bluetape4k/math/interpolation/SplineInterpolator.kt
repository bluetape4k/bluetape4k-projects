package io.bluetape4k.math.interpolation

import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator

typealias ApacheSplineInterpolator = org.apache.commons.math3.analysis.interpolation.SplineInterpolator

class SplineInterpolator: AbstractInterpolator() {

    override val apacheInterpolator: UnivariateInterpolator = ApacheSplineInterpolator()

}
