package io.bluetape4k.science.coords

/**
 * 도(Degree)와 분(Decimal Minutes)으로 좌표를 표현하는 클래스입니다.
 *
 * @param degree 도 (정수 부분)
 * @param minute 분 (소수 포함)
 */
data class DM(
    val degree: Int,
    val minute: Double,
): Comparable<DM> {

    override fun compareTo(other: DM): Int {
        var diff = degree.compareTo(other.degree)
        if (diff == 0) diff = minute.compareTo(other.minute)
        return diff
    }
}
