package io.bluetape4k.math.equation

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.BaseUnivariateSolver

typealias ApacheBisectionSolver = org.apache.commons.math3.analysis.solvers.BisectionSolver

/**
 * 이분법으로 특정 함수의 Root를 찾는다
 */
class BisectionEquator: AbstractEquator() {

    override val solver: BaseUnivariateSolver<UnivariateFunction> = ApacheBisectionSolver()

}
