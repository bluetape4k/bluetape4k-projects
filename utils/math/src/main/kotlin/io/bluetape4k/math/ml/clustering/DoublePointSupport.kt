package io.bluetape4k.math.ml.clustering

import org.apache.commons.math3.ml.clustering.DoublePoint


fun doublePointOf(x: Double, y: Double): DoublePoint =
    DoublePoint(doubleArrayOf(x, y))

fun doublePointOf(values: DoubleArray): DoublePoint = DoublePoint(values)

fun <N: Number> doublePointOf(vararg values: N): DoublePoint =
    DoublePoint(values.map { it.toDouble() }.toDoubleArray())

fun DoubleArray.toDoublePoint(): DoublePoint = DoublePoint(this)
