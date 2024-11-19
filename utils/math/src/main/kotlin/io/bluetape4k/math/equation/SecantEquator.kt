package io.bluetape4k.math.equation

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.BaseUnivariateSolver

typealias ApacheSecantSolver = org.apache.commons.math3.analysis.solvers.SecantSolver

class SecantEquator: AbstractEquator() {

    override val solver: BaseUnivariateSolver<UnivariateFunction> = ApacheSecantSolver()

}
