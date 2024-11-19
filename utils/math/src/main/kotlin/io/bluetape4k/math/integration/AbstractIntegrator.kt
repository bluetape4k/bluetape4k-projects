package io.bluetape4k.math.integration

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.math.integration.Integrator.Companion.DEFAULT_MAXEVAL
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator

abstract class AbstractIntegrator: Integrator {

    companion object: KLogging()

    protected abstract val apacheIntegrator: UnivariateIntegrator

    override val relativeAccuracy: Double
        get() = apacheIntegrator.relativeAccuracy
    override val absoluteAccuracy: Double
        get() = apacheIntegrator.absoluteAccuracy

    /**
     * 함수의 [lower, upper) 구간을 적분합니다.
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
