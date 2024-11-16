package io.bluetape4k.support

/**
 * [StringBuilder] 에 [Iterable] 값 들을 [separator]로 구분하여 추가합니다.
 *
 * ```
 * val list = listOf(1, 2, 3, 4, 5)
 * val builder = StringBuilder()
 * builder.appendItems(list, separator = ", ")
 * println(builder.toString()) // 1, 2, 3, 4, 5
 * ```
 *
 * @param T         [iterable] 값의 수형
 * @param iterable  추가할 값들
 * @param separator 값들을 구분할 문자열 (기본값 : ", ")
 *
 * @see joinToString
 */
fun <T> StringBuilder.appendItems(iterable: Iterable<T>, separator: String = ", ") {
    appendItems(iterable.asSequence(), separator)
}

/**
 * [StringBuilder] 에 [Sequence] 를 추가합니다.
 *
 * ```
 * val sequence = sequenceOf(1, 2, 3, 4, 5)
 * val builder = StringBuilder()
 * builder.appendItems(sequence, separator = ", ")
 * println(builder.toString()) // 1, 2, 3, 4, 5
 * ```
 *
 * @param T         [sequence] 값의 수형
 * @param sequence  추가할 값들
 * @param separator 값들을 구분할 문자열 (기본값 : ", ")
 *
 * @see joinToString
 */
fun <T> StringBuilder.appendItems(sequence: Sequence<T>, separator: String = ", ") {
    val iter = sequence.iterator()
    if (iter.hasNext()) {
        append(iter.next())
    }
    while (iter.hasNext()) {
        append(separator)
        append(iter.next())
    }
}
