package io.bluetape4k.collections.permutations

/**
 * 순열의 종료를 나타내는 싱글턴 클래스입니다.
 *
 * 빈 순열을 표현하며, 모든 연산은 빈 순열을 반환하거나 예외를 발생시킵니다.
 *
 * 예제:
 * ```kotlin
 * val empty = Nil.instance<Int>()
 * empty.isEmpty() // true
 * empty.head // NoSuchElementException 발생
 * ```
 *
 * @param T 요소 타입
 */
class Nil<T>: Permutation<T>() {

    companion object {
        @JvmField
        val NIL: Nil<Any> = Nil()

        /**
         * 타입 캐스팅된 빈 순열 인스턴스를 반환합니다.
         *
         * @return 빈 순열
         */
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T> instance(): Nil<T> = NIL as Nil<T>
    }

    override val head: T
        get() = throw NoSuchElementException("head of empty sequence")

    override val tail: Permutation<T>
        get() = throw NoSuchElementException("tail of empty sequence")

    override val isTailDefined: Boolean
        get() = false

    override fun get(index: Int): T = throw IndexOutOfBoundsException(index.toString())

    override fun <R: Any?> map(mapper: (T) -> R): Permutation<R> = instance()
    override fun filter(predicate: (T) -> Boolean): Permutation<T> = instance()
    override fun <R> flatMap(mapper: (T) -> Iterable<R>): Permutation<R> = instance()
    override fun takeUnsafe(maxSize: Long): Permutation<T> = instance()
    override fun dropUnsafe(startInclusive: Long): Permutation<T> = instance()

    override fun forEach(action: (T) -> Unit) {
        /* 빈 순열이므로 아무것도 하지 않음 */
    }

    override fun max(comparator: Comparator<in T>): T? = null
    override fun min(comparator: Comparator<in T>): T? = null

    override val size: Int get() = 0

    override fun anyMatch(predicate: (T) -> Boolean): Boolean = false
    override fun allMatch(predicate: (T) -> Boolean): Boolean = true
    override fun noneMatch(predicate: (T) -> Boolean): Boolean = true

    override fun <S, R> zip(second: Permutation<S>, zipper: (T, S) -> R): Permutation<R> = instance()

    override fun takeWhile(predicate: (T) -> Boolean): Permutation<T> = instance()
    override fun dropWhile(predicate: (T) -> Boolean): Permutation<T> = instance()

    override fun slidingUnsafe(size: Long): Permutation<List<T>> = instance()
    override fun groupedUnsafe(size: Long): Permutation<List<T>> = instance()

    override fun scan(initial: T, binFunc: (T, T) -> T): Permutation<T> = permutationOf(initial)

    override fun distinct(): Permutation<T> = instance()
    override fun startsWith(iterator: Iterator<T>): Boolean = !iterator.hasNext()
    override fun force(): Permutation<T> = this

    override fun equals(other: Any?): Boolean = other is Nil<*>
    override fun hashCode(): Int = Nil.hashCode()
    override fun isEmpty(): Boolean = true
}
