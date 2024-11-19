package io.bluetape4k.math.integration

import org.apache.commons.math3.analysis.integration.UnivariateIntegrator

typealias ApacheMidPointIntegrator = org.apache.commons.math3.analysis.integration.MidPointIntegrator

/**
 * 중점법을 이용한 적분을 수행합니다.
 */
class MidPointIntegrator: AbstractIntegrator() {

    override val apacheIntegrator: UnivariateIntegrator = ApacheMidPointIntegrator()

}
