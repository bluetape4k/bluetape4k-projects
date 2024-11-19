package io.bluetape4k.math

import java.math.BigDecimal

fun Sequence<BigDecimal>.sum(): BigDecimal =
    fold(BigDecimal.ZERO) { acc, x -> acc + x }

fun Iterable<BigDecimal>.sum(): BigDecimal =
    fold(BigDecimal.ZERO) { acc, x -> acc + x }

fun Sequence<BigDecimal>.average(): BigDecimal = sum() / count().toBigDecimal()
fun Iterable<BigDecimal>.average(): BigDecimal = sum() / count().toBigDecimal()
