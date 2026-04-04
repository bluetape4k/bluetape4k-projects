package io.bluetape4k.math.equation

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.BaseUnivariateSolver

typealias ApacheBrentSolver = org.apache.commons.math3.analysis.solvers.BrentSolver

/**
 * Brent 방법으로 특정 함수의 Root를 찾는다.
 * 이분법보다 빠른 수렴 속도를 제공합니다.
 *
 * ```kotlin
 * val equator = BrentEquator()
 * val root = equator.solve(min = 0.0, max = 2.0) { x -> x * x - 2.0 }
 * // root ≈ 1.4142 (sqrt(2))
 * ```
 */
class BrentEquator: AbstractEquator() {

    override val solver: BaseUnivariateSolver<UnivariateFunction> = ApacheBrentSolver()

}
