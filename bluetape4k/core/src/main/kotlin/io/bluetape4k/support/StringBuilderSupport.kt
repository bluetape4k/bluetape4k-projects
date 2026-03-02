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
    val iter = iterable.iterator()
    if (iter.hasNext()) {
        append(iter.next())
    }
    while (iter.hasNext()) {
        append(separator)
        append(iter.next())
    }
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
    return appendItems(sequence.asIterable(), separator)
}

/**
 * [Iterable] 요소를 [builder]에 [separator]로 구분해 추가합니다.
 *
 * ## 동작/계약
 * - 입력이 비어 있으면 [builder]는 변경되지 않습니다.
 * - [builder]를 직접 변경(mutate)하며 새 [StringBuilder]는 만들지 않습니다.
 * - 요소의 `toString()` 결과를 순서대로 추가합니다.
 * - 시간 복잡도는 요소 수 `n`에 대해 `O(n)`입니다.
 *
 * ```kotlin
 * val b = StringBuilder()
 * listOf("a", "b", "c").appendItems(b, "/")
 * check(b.toString() == "a/b/c")
 * ```
 *
 * @param builder 누적 문자열을 기록할 대상
 * @param separator 요소 사이에 삽입할 구분자
 */
fun <T> Iterable<T>.appendItems(builder: StringBuilder, separator: String = ", ") {
    val iter = iterator()
    if (iter.hasNext()) {
        builder.append(iter.next())
    }
    while (iter.hasNext()) {
        builder.append(separator)
        builder.append(iter.next())
    }
}

/**
 * [Sequence] 요소를 [builder]에 [separator]로 구분해 추가합니다.
 *
 * ## 동작/계약
 * - [Sequence]를 한 번 순회하며 지연 평가 결과를 즉시 [builder]에 기록합니다.
 * - [builder]를 직접 변경(mutate)하며 새 [StringBuilder]는 만들지 않습니다.
 * - 입력이 비어 있으면 [builder]는 변경되지 않습니다.
 * - 시간 복잡도는 요소 수 `n`에 대해 `O(n)`입니다.
 *
 * ```kotlin
 * val b = StringBuilder()
 * sequenceOf(1, 2, 3).appendItems(b, "-")
 * check(b.toString() == "1-2-3")
 * ```
 *
 * @param builder 누적 문자열을 기록할 대상
 * @param separator 요소 사이에 삽입할 구분자
 */
fun <T> Sequence<T>.appendItems(builder: StringBuilder, separator: String = ", ") {
    return asIterable().appendItems(builder, separator)
}
