package io.bluetape4k.math.commons

import java.math.BigDecimal
import kotlin.math.absoluteValue

fun Double.abs(): Double = absoluteValue
fun Float.abs(): Float = absoluteValue
fun Long.abs(): Long = absoluteValue
fun Int.abs(): Int = absoluteValue

@JvmName("absOfDouble")
fun Iterable<Double>.abs(): Iterable<Double> = map { it.absoluteValue }

@JvmName("absOfFloat")
fun Iterable<Float>.abs(): Iterable<Float> = map { it.absoluteValue }

@JvmName("absOfLong")
fun Iterable<Long>.abs(): Iterable<Long> = map { it.absoluteValue }

@JvmName("absOfInt")
fun Iterable<Int>.abs(): Iterable<Int> = map { it.absoluteValue }

@JvmName("absOfBigDecimal")
fun Iterable<BigDecimal>.abs(): Iterable<BigDecimal> = map { it.abs() }
