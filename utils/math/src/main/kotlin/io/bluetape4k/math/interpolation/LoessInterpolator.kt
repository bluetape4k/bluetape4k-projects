package io.bluetape4k.math.interpolation

import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator

typealias ApacheLoessInterpolator = org.apache.commons.math3.analysis.interpolation.LoessInterpolator

class LoessInterpolator: AbstractInterpolator() {

    override val apacheInterpolator: UnivariateInterpolator = ApacheLoessInterpolator()

}
