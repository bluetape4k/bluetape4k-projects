package io.bluetape4k.math.geometry.spherial.oned

import org.apache.commons.math3.geometry.spherical.oned.S1Point

fun <T: Number> T.toS1Point(): S1Point = S1Point(this.toDouble())
