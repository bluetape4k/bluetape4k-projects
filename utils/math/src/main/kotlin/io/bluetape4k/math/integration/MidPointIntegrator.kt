package io.bluetape4k.math.integration

import org.apache.commons.math3.analysis.integration.UnivariateIntegrator

typealias ApacheMidPointIntegrator = org.apache.commons.math3.analysis.integration.MidPointIntegrator

/**
 * 중점법을 이용한 적분을 수행합니다.
 * 각 구간의 중점에서 함수값을 이용하여 적분값을 근사합니다.
 *
 * ```kotlin
 * val integrator = MidPointIntegrator()
 * val result = integrator.integrate(lower = 0.0, upper = 1.0) { x -> x * x }
 * // result ≈ 0.3333 (∫x² dx from 0 to 1)
 * ```
 */
class MidPointIntegrator: AbstractIntegrator() {

    override val apacheIntegrator: UnivariateIntegrator = ApacheMidPointIntegrator()

}
