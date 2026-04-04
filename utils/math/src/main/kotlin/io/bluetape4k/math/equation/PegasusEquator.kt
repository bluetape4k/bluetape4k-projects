package io.bluetape4k.math.equation

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.BaseUnivariateSolver

typealias ApachePegasusSolver = org.apache.commons.math3.analysis.solvers.PegasusSolver

/**
 * Pegasus 방법으로 특정 함수의 Root를 찾는다.
 * Illinois 알고리즘의 개선판으로, 더 빠른 수렴 속도를 제공합니다.
 *
 * ```kotlin
 * val equator = PegasusEquator()
 * val root = equator.solve(min = 0.0, max = 2.0) { x -> x * x - 2.0 }
 * // root ≈ 1.4142 (sqrt(2))
 * ```
 */
class PegasusEquator: AbstractEquator() {

    override val solver: BaseUnivariateSolver<UnivariateFunction> = ApachePegasusSolver()

}
