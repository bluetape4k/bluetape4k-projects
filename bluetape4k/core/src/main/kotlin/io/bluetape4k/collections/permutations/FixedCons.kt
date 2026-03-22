package io.bluetape4k.collections.permutations

/**
 * tail이 즉시 평가되는(eager) 순열 구현체입니다.
 *
 * [Cons]와 달리 tail이 생성 시점에 이미 결정되어 있으므로
 * 동기화가 필요 없고, [isTailDefined]는 항상 true입니다.
 *
 * @param E 요소 타입
 * @param head 순열의 첫 번째 요소
 * @param tail 나머지 순열
 */
class FixedCons<E>(
    override val head: E,
    override val tail: Permutation<E>,
): Permutation<E>() {

    override val isTailDefined: Boolean get() = true

    override fun <R> map(mapper: (E) -> R): Permutation<R> {
        return cons(mapper(head)) { tail.map(mapper) }
    }

    override fun filter(predicate: (E) -> Boolean): Permutation<E> {
        return if (predicate(head)) {
            cons(head, tail.filter(predicate))
        } else {
            tail.filter(predicate)
        }
    }

    override fun <R> flatMap(mapper: (E) -> Iterable<R>): Permutation<R> {
        val result = mutableListOf<R>()
        mapper(head).forEach { result.add(it) }
        return concat(result, tail.flatMap(mapper))
    }

    override fun takeUnsafe(maxSize: Long): Permutation<E> {
        return if (maxSize > 1) {
            cons(head, tail.takeUnsafe(maxSize - 1))
        } else {
            permutationOf(head)
        }
    }

    override fun isEmpty(): Boolean = false
}
