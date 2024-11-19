package io.bluetape4k.math.equation

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.BaseUnivariateSolver

typealias ApacheRegulaFalsiSolver = org.apache.commons.math3.analysis.solvers.RegulaFalsiSolver

class RegulaFalsiEquator: AbstractEquator() {

    override val solver: BaseUnivariateSolver<UnivariateFunction> = ApacheRegulaFalsiSolver()

}
