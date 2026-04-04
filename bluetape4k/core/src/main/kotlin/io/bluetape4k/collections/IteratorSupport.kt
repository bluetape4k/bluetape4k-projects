package io.bluetape4k.collections

/**
 * [Iterator]를 [MutableIterator]로 변환합니다.
 *
 * 읽기 전용 [Iterator]를 [MutableIterator] 인터페이스로 래핑합니다.
 * [remove] 호출 시 [UnsupportedOperationException]이 발생합니다.
 *
 * ```kotlin
 * val iter: Iterator<Int> = listOf(1, 2, 3).iterator()
 * val mutableIter: MutableIterator<Int> = iter.asMutableIterator()
 *
 * mutableIter.hasNext() // true
 * mutableIter.next()    // 1
 * mutableIter.next()    // 2
 * mutableIter.next()    // 3
 * mutableIter.hasNext() // false
 *
 * // remove()는 UnsupportedOperationException 발생
 * // mutableIter.remove()
 * ```
 *
 * @return [MutableIterator]로 래핑된 이터레이터
 */
fun <T> Iterator<T>.asMutableIterator(): MutableIterator<T> {
    return object: MutableIterator<T> {
        override fun hasNext(): Boolean = this@asMutableIterator.hasNext()
        override fun next(): T = this@asMutableIterator.next()
        override fun remove() {
            throw UnsupportedOperationException("remove")
        }
    }
}
