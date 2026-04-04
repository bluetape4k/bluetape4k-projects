package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * 도(Degree)와 분(Decimal Minutes)으로 좌표를 표현하는 클래스입니다.
 *
 * ```kotlin
 * val dm = DM(degree = 37, minute = 33.99)
 * println(dm.degree) // 37
 * println(dm.minute) // 33.99
 *
 * val a = DM(37, 30.0)
 * val b = DM(37, 45.0)
 * println(a < b) // true
 * ```
 *
 * @param degree 도 (정수 부분)
 * @param minute 분 (소수 포함)
 */
data class DM(
    val degree: Int,
    val minute: Double,
): Comparable<DM>, Serializable {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    override fun compareTo(other: DM): Int {
        var diff = degree.compareTo(other.degree)
        if (diff == 0) diff = minute.compareTo(other.minute)
        return diff
    }
}
