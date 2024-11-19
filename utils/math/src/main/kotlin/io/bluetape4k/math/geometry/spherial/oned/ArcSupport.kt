package io.bluetape4k.math.geometry.spherial.oned

import org.apache.commons.math3.geometry.spherical.oned.Arc

fun arcOf(lower: Double, upper: Double, tolerance: Double): Arc =
    Arc(lower, upper, tolerance)
