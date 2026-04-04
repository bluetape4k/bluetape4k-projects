package io.bluetape4k.math.equation

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.BaseUnivariateSolver

typealias ApacheSecantSolver = org.apache.commons.math3.analysis.solvers.SecantSolver

/**
 * 할선법(Secant Method)으로 특정 함수의 Root를 찾는다.
 * 뉴턴 방법과 유사하나 미분값 대신 두 점의 할선을 사용합니다.
 *
 * ```kotlin
 * val equator = SecantEquator()
 * val root = equator.solve(min = 0.0, max = 2.0) { x -> x * x - 2.0 }
 * // root ≈ 1.4142 (sqrt(2))
 * ```
 */
class SecantEquator: AbstractEquator() {

    override val solver: BaseUnivariateSolver<UnivariateFunction> = ApacheSecantSolver()

}
