package io.bluetape4k.math.integration

import org.apache.commons.math3.analysis.integration.UnivariateIntegrator

typealias ApacheRombergIntegrator = org.apache.commons.math3.analysis.integration.RombergIntegrator

/**
 * [Romberg Algorithm](https://mathworld.wolfram.com/RombergIntegration.html) 을 이용하여 적분을 수행합니다.
 * Richardson 외삽법을 사용하여 높은 정밀도의 적분 결과를 제공합니다.
 *
 * ```kotlin
 * val integrator = RombergIntegrator()
 * val result = integrator.integrate(lower = 0.0, upper = 1.0) { x -> x * x }
 * // result ≈ 0.3333 (∫x² dx from 0 to 1)
 * ```
 */
class RombergIntegrator: AbstractIntegrator() {

    override val apacheIntegrator: UnivariateIntegrator = ApacheRombergIntegrator()

}
