package io.bluetape4k.math.equation

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.BaseUnivariateSolver

typealias ApacheRiddersSolver = org.apache.commons.math3.analysis.solvers.RiddersSolver

class RiddersEquator: AbstractEquator() {

    override val solver: BaseUnivariateSolver<UnivariateFunction> = ApacheRiddersSolver()
}
