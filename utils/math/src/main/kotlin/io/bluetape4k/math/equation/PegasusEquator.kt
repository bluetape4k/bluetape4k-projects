package io.bluetape4k.math.equation

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.BaseUnivariateSolver

typealias ApachePegasusSolver = org.apache.commons.math3.analysis.solvers.PegasusSolver

class PegasusEquator: AbstractEquator() {

    override val solver: BaseUnivariateSolver<UnivariateFunction> = ApachePegasusSolver()

}
