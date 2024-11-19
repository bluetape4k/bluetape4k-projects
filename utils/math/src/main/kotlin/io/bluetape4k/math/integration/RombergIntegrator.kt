package io.bluetape4k.math.integration

import org.apache.commons.math3.analysis.integration.UnivariateIntegrator

typealias ApacheRombergIntegrator = org.apache.commons.math3.analysis.integration.RombergIntegrator

/**
 * [Romberg Algorithm](https://mathworld.wolfram.com/RombergIntegration.html) 을 이용하여 적분을 수행합니다.
 */
class RombergIntegrator: AbstractIntegrator() {

    override val apacheIntegrator: UnivariateIntegrator = ApacheRombergIntegrator()

}
