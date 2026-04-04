package io.bluetape4k.math.equation

import io.bluetape4k.logging.KLogging
import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.BaseUnivariateSolver

/**
 * [Equator] 인터페이스의 기본 구현체.
 * Apache Commons Math의 [BaseUnivariateSolver]를 감싸 Kotlin 람다 기반 API를 제공합니다.
 *
 * ```kotlin
 * // 직접 사용이 아닌, 구체적인 구현 클래스를 통해 사용합니다.
 * val equator = BrentEquator()  // AbstractEquator의 구현체
 * val root = equator.solve(min = 0.0, max = 2.0) { x -> x * x - 2.0 }
 * // root ≈ 1.4142 (sqrt(2))
 * ```
 */
abstract class AbstractEquator: Equator {

    companion object: KLogging()

    protected abstract val solver: BaseUnivariateSolver<UnivariateFunction>

    override val absoluteAccuracy: Double
        get() = solver.absoluteAccuracy

    override fun solve(
        maxEval: Int,
        min: Double,
        max: Double,
        evaluator: (Double) -> Double,
    ): Double {
        return solver.solve(
            maxEval,
            UnivariateFunction { x: Double -> evaluator(x) },
            min,
            max
        )
    }
}
