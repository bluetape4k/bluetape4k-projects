package io.bluetape4k.math.transform

import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.TransformUtils

fun DoubleArray.scale(d: Double): DoubleArray =
    TransformUtils.scaleArray(this, d)

fun Array<Complex>.scale(d: Double): Array<Complex> =
    TransformUtils.scaleArray(this, d)

fun Array<Complex>.toRealImaginaryArray(): Array<DoubleArray> =
    TransformUtils.createRealImaginaryArray(this)

fun Array<DoubleArray>.toComplexArray(): Array<Complex> =
    TransformUtils.createComplexArray(this)

fun Int.exactLog2(): Int = TransformUtils.exactLog2(this)
