package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * 도(Degree), 분(Minute), 초(Second)로 좌표를 표현하는 클래스입니다.
 *
 * ```kotlin
 * val dms = DMS(degree = 126, minute = 58, second = 40.8)
 * println(dms.degree) // 126
 * println(dms.minute) // 58
 * println(dms.second) // 40.8
 *
 * val a = DMS(37, 33, 57.54)
 * val b = DMS(37, 33, 58.00)
 * println(a < b) // true
 * ```
 *
 * @param degree 도 (정수)
 * @param minute 분 (정수)
 * @param second 초 (소수 포함)
 */
data class DMS(
    val degree: Int,
    val minute: Int,
    val second: Double,
): Comparable<DMS>, Serializable {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    override fun compareTo(other: DMS): Int {
        var diff = degree.compareTo(other.degree)
        if (diff == 0) diff = minute.compareTo(other.minute)
        if (diff == 0) diff = second.compareTo(other.second)
        return diff
    }
}
