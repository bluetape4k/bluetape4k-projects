package io.bluetape4k.javatimes.range

import io.bluetape4k.javatimes.minus
import io.bluetape4k.javatimes.plus
import io.bluetape4k.javatimes.toEpochDay
import io.bluetape4k.javatimes.toEpochMillis
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.util.*

private fun mod(a: Int, b: Int): Int {
    val mod = a % b
    return if (mod >= 0) mod else mod + b
}

private fun mod(a: Long, b: Long): Long {
    val mod = a % b
    return if (mod >= 0) mod else mod + b
}

private fun differenceModulo(a: Int, b: Int, c: Int): Int =
    mod(mod(a, c) - mod(b, c), c)

private fun differenceModulo(a: Long, b: Long, c: Long): Long =
    mod(mod(a, c) - mod(b, c), c)

//internal fun getProgressionLastElement(start: Instant, end: Instant, stepMillis: Long): Instant = when {
//    stepMillis > 0 -> end.minusMillis(differenceModulo(end.toEpochMilli(), start.toEpochMilli(), stepMillis))
//    stepMillis < 0 -> end.plusMillis(differenceModulo(start.toEpochMilli(), end.toEpochMilli(), -stepMillis))
//    else           -> throw IllegalArgumentException("stepMillis must not be zero")
//}

@Suppress("UNCHECKED_CAST")
internal fun <T: Date> getProgressionLastElement(start: T, end: T, stepMillis: Long): T = when {
    stepMillis > 0 -> (end - differenceModulo(end.time, start.time, stepMillis)) as T
    stepMillis < 0 -> (end + differenceModulo(start.time, end.time, -stepMillis)) as T
    else           -> throw IllegalArgumentException("stepMillis must not be zero")
}

@Suppress("UNCHECKED_CAST")
internal fun <T> getProgressionLastElement(start: T, end: T, stepMillis: Long): T
        where T: Temporal, T: Comparable<T> {
    return if (end.isSupported(ChronoUnit.MILLIS)) {
        when {
            stepMillis > 0 -> end.minus(
                differenceModulo(end.toEpochMillis(), start.toEpochMillis(), stepMillis),
                ChronoUnit.MILLIS
            ) as T

            stepMillis < 0 -> end.plus(
                differenceModulo(start.toEpochMillis(), end.toEpochMillis(), -stepMillis),
                ChronoUnit.MILLIS
            ) as T

            else           -> throw IllegalArgumentException("stepMillis must not be zero")
        }
    } else {
        when {
            stepMillis > 0 -> end.minus(
                differenceModulo(end.toEpochDay(), start.toEpochDay(), stepMillis),
                ChronoUnit.DAYS
            ) as T

            stepMillis < 0 -> end.plus(
                differenceModulo(start.toEpochDay(), end.toEpochDay(), -stepMillis),
                ChronoUnit.DAYS
            ) as T

            else           -> throw IllegalArgumentException("stepMillis must not be zero")
        }
    }
}
