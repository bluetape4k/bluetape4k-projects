package io.bluetape4k.collections.permutations

/**
 * [Permutation]을 위한 Iterator 구현체입니다.
 *
 * 순열의 요소를 순차적으로 순회합니다. 변경 불가능한(immutable) 순열의 특성상
 * [remove] 연산은 지원하지 않습니다.
 *
 * @param E 요소 타입
 * @param underlying 순회할 순열
 */
class PermutationIterator<E>(
    private var underlying: Permutation<E>,
): MutableIterator<E> {

    override fun hasNext(): Boolean = !underlying.isEmpty()

    override fun next(): E {
        val next = underlying.head
        underlying = underlying.tail
        return next
    }

    override fun remove() {
        throw UnsupportedOperationException("remove")
    }

    /**
     * 남은 요소에 대해 [action]을 적용합니다.
     *
     * @param action 각 요소에 적용할 함수
     */
    fun forEachRemaining(action: (E) -> Unit) {
        underlying.forEach(action)
    }
}
