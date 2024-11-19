package io.bluetape4k.math.interpolation

import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator

typealias ApacheAkimaSplineInterpolator = org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator

class AkimaSplineInterpolator: AbstractInterpolator() {

    override val apacheInterpolator: UnivariateInterpolator = ApacheAkimaSplineInterpolator()
}
