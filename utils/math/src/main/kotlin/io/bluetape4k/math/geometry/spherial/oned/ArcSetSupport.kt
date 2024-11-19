package io.bluetape4k.math.geometry.spherial.oned

import org.apache.commons.math3.geometry.partitioning.BSPTree
import org.apache.commons.math3.geometry.spherical.oned.ArcsSet
import org.apache.commons.math3.geometry.spherical.oned.Sphere1D

fun arcsSetOf(tolerance: Double): ArcsSet = ArcsSet(tolerance)

fun arcsSetOf(lower: Double, upper: Double, tolerance: Double): ArcsSet = ArcsSet(lower, upper, tolerance)

fun BSPTree<Sphere1D>.buildArcsSet(tolerance: Double): ArcsSet = ArcsSet(this, tolerance)
