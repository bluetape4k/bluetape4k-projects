package io.bluetape4k.math.integration

import org.apache.commons.math3.analysis.integration.UnivariateIntegrator

typealias ApacheTrapezoidIntegrator = org.apache.commons.math3.analysis.integration.TrapezoidIntegrator

/**
 * Trapezoid integrator
 *
 * [사다리꼴 공식](https://ko.wikipedia.org/wiki/%EC%82%AC%EB%8B%A4%EB%A6%AC%EA%BC%B4_%EA%B3%B5%EC%8B%9D) 을 이용하여 적분을 수행합니다.
 */
class TrapezoidIntegrator: AbstractIntegrator() {

    override val apacheIntegrator: UnivariateIntegrator = ApacheTrapezoidIntegrator()

}
