package io.bluetape4k.ranges

/**
 * 값의 범위를 나타내는 인터페이스입니다.
 */
interface Range<T: Comparable<T>> {

    /**
     * 범위의 하한 값
     */
    val first: T

    /**
     * 범위의 상한 값
     */
    val last: T

    operator fun contains(value: T): Boolean

    fun isEmpty(): Boolean
}

fun <T: Comparable<T>> Range<T>.contains(other: Range<T>): Boolean =
    first <= other.first && last >= other.last

/**
 * [Range]의 컬렉션이 오름차순으로 정렬되어 있는지 확인합니다.
 */
fun <T: Comparable<T>> Iterable<Range<T>>.isAscending(): Boolean {
    val first = firstOrNull() ?: return true
    var max = first.first

    val count = drop(1)
        .takeWhile { range ->
            (max <= range.first).apply { max = range.first }
        }.count()

    return this.count() == count
}
