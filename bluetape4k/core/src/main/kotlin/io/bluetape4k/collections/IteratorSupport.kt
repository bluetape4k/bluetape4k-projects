package io.bluetape4k.collections

/**
 * [Iterator]를 [MutableIterator]로 변환합니다.
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
