package io.bluetape4k.math.integration

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.math.integration.Integrator.Companion.DEFAULT_MAXEVAL
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator

/**
 * [Integrator] 인터페이스의 기본 구현체.
 * Apache Commons Math의 [UnivariateIntegrator]를 감싸 Kotlin 람다 기반 API를 제공합니다.
 *
 * ```kotlin
 * // 구체적인 구현 클래스를 통해 사용합니다.
 * val integrator = SimpsonIntegrator()
 * val result = integrator.integrate(lower = 0.0, upper = 1.0) { x -> x * x }
 * // result ≈ 0.3333 (∫x² dx from 0 to 1)
 * ```
 */
abstract class AbstractIntegrator: Integrator {

    companion object: KLogging()

    protected abstract val apacheIntegrator: UnivariateIntegrator

    override val relativeAccuracy: Double
        get() = apacheIntegrator.relativeAccuracy

    override val absoluteAccuracy: Double
        get() = apacheIntegrator.absoluteAccuracy

    /**
     * 함수의 [lower, upper] 구간을 적분합니다.
     *
     * ```kotlin
     * val integrator = SimpsonIntegrator()
     * val result = integrator.integrate(lower = 0.0, upper = 1.0) { x -> x * x }
     * // result ≈ 0.3333 (∫x² dx from 0 to 1)
     * ```
     *
     * @param evaluator 적분할 함수
     * @param lower 시작 위치
     * @param upper 끝 위치
     * @return 적분 값
     */
    override fun integrate(lower: Double, upper: Double, evaluator: (Double) -> Double): Double {
        assert(lower <= upper) { "lower[$lower] <= upper[$upper] 이어야 합니다." }
        log.trace { "lower=$lower, upper=$upper 범위의 적분을 수행합니다." }

        val result = apacheIntegrator.integrate(DEFAULT_MAXEVAL, evaluator, lower, upper)
        log.trace { "Integration result=$result" }
        return result
    }
}
