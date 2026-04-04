package io.bluetape4k.math.equation

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.BaseUnivariateSolver

typealias ApacheRegulaFalsiSolver = org.apache.commons.math3.analysis.solvers.RegulaFalsiSolver

/**
 * Regula Falsi(거짓 위치법)으로 특정 함수의 Root를 찾는다.
 *
 * ```kotlin
 * val equator = RegulaFalsiEquator()
 * val root = equator.solve(min = 0.0, max = 2.0) { x -> x * x - 2.0 }
 * // root ≈ 1.4142 (sqrt(2))
 * ```
 */
class RegulaFalsiEquator: AbstractEquator() {

    override val solver: BaseUnivariateSolver<UnivariateFunction> = ApacheRegulaFalsiSolver()

}
