package io.bluetape4k.math.equation

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.BaseUnivariateSolver

typealias ApacheRiddersSolver = org.apache.commons.math3.analysis.solvers.RiddersSolver

/**
 * Ridders 방법으로 특정 함수의 Root를 찾는다.
 * 이분법과 유사하지만 2차 수렴 속도를 제공합니다.
 *
 * ```kotlin
 * val equator = RiddersEquator()
 * val root = equator.solve(min = 0.0, max = 2.0) { x -> x * x - 2.0 }
 * // root ≈ 1.4142 (sqrt(2))
 * ```
 */
class RiddersEquator: AbstractEquator() {

    override val solver: BaseUnivariateSolver<UnivariateFunction> = ApacheRiddersSolver()
}
