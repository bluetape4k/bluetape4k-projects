package io.bluetape4k.math.interpolation

import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator

typealias ApacheNevilleInterpolator = org.apache.commons.math3.analysis.interpolation.NevilleInterpolator

class NevilleInterpolator: AbstractInterpolator() {

    override val apacheInterpolator: UnivariateInterpolator = ApacheNevilleInterpolator()

}
