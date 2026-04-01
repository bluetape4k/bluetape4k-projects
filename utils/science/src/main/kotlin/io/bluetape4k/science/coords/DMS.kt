package io.bluetape4k.science.coords

/**
 * 도(Degree), 분(Minute), 초(Second)로 좌표를 표현하는 클래스입니다.
 *
 * @param degree 도 (정수)
 * @param minute 분 (정수)
 * @param second 초 (소수 포함)
 */
data class DMS(
    val degree: Int,
    val minute: Int,
    val second: Double,
): Comparable<DMS> {

    override fun compareTo(other: DMS): Int {
        var diff = degree.compareTo(other.degree)
        if (diff == 0) diff = minute.compareTo(other.minute)
        if (diff == 0) diff = second.compareTo(other.second)
        return diff
    }
}
