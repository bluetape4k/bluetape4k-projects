package io.bluetape4k.collections.permutations

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 지연 평가(lazy evaluation) 방식으로 tail을 계산하는 순열 구현체입니다.
 *
 * [tailFunc]는 최초 tail 접근 시 한 번만 평가되며, 이후에는 캐싱된 결과를 반환합니다.
 * [ReentrantLock]과 DCL(Double-Checked Locking) 패턴으로 스레드 안전성을 보장합니다.
 *
 * @param E 요소 타입
 * @param head 순열의 첫 번째 요소
 * @param tailFunc tail을 지연 생성하는 함수
 */
class Cons<E>(
    override val head: E,
    private val tailFunc: () -> Permutation<E>,
): Permutation<E>() {

    private val lock = ReentrantLock()

    @Volatile
    private var tailOrNull: Permutation<E>? = null

    override val tail: Permutation<E>
        get() {
            tailOrNull?.let { return it }
            return lock.withLock {
                tailOrNull?.let { return it }
                tailFunc().also { tailOrNull = it }
            }
        }

    override val isTailDefined: Boolean get() = tailOrNull != null

    override fun <R> map(mapper: (E) -> R): Permutation<R> {
        return cons(mapper(head)) { tail.map(mapper) }
    }

    override fun filter(predicate: (E) -> Boolean): Permutation<E> {
        return if (predicate(head)) {
            cons(head) { tail.filter(predicate) }
        } else {
            tail.filter(predicate)
        }
    }

    override fun <R> flatMap(mapper: (E) -> Iterable<R>): Permutation<R> {
        val result = mutableListOf<R>()
        mapper(head).forEach { result.add(it) }
        return concat(result) { tail.flatMap(mapper) }
    }

    override fun takeUnsafe(maxSize: Long): Permutation<E> {
        return if (maxSize > 1) {
            cons(head) { tail.takeUnsafe(maxSize - 1) }
        } else {
            permutationOf(head)
        }
    }

    override fun isEmpty(): Boolean = false
}
