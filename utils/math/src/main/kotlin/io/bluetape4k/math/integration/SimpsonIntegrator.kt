package io.bluetape4k.math.integration

import org.apache.commons.math3.analysis.integration.UnivariateIntegrator

typealias ApacheSimpsonIntegrator = org.apache.commons.math3.analysis.integration.SimpsonIntegrator

/**
 * Simpson integrator
 *
 * [심프슨 공식](https://ko.wikipedia.org/wiki/%EC%8B%AC%ED%94%84%EC%8A%A8_%EA%B3%B5%EC%8B%9D) 을 활용하여 적분을 수행합니다.
 */
class SimpsonIntegrator: AbstractIntegrator() {

    override val apacheIntegrator: UnivariateIntegrator = ApacheSimpsonIntegrator()

}
