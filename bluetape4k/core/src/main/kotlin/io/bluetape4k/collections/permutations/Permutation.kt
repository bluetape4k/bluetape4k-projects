package io.bluetape4k.collections.permutations

import java.util.Objects
import kotlin.collections.AbstractList

/**
 * 지연 평가(lazy evaluation) 기반의 순열(Permutation) 추상 클래스입니다.
 *
 * [AbstractList]와 [Sequence]를 동시에 구현하여 리스트처럼 인덱스 접근이 가능하면서도
 * 무한 시퀀스를 표현할 수 있습니다. tail은 필요 시점에 평가되어 메모리 효율적인 연산을 제공합니다.
 *
 * ```kotlin
 * val perm = permutationOf(1, 2, 3)
 * val head = perm.head                          // 1
 * val doubled = perm.map { it * 2 }            // [2, 4, 6]
 * val evens = perm.filter { it % 2 == 0 }      // [2]
 * val first2 = perm.take(2).toList()           // [1, 2]
 * val rest = perm.drop(1).toList()             // [2, 3]
 * val zipped = perm.zip(permutationOf(10, 20, 30)) { a, b -> a + b }
 * val zipList = zipped.toList()                // [11, 22, 33]
 * ```
 *
 * @param E 요소 타입
 */
abstract class Permutation<E>: AbstractList<E>(), Sequence<E> {

    /**
     * 순열의 첫 번째 요소입니다.
     *
     * ```kotlin
     * val perm = permutationOf(10, 20, 30)
     * val h = perm.head   // 10
     * ```
     */
    abstract val head: E

    /**
     * 첫 번째 요소를 제외한 나머지 순열입니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val t = perm.tail.toList()   // [2, 3]
     * ```
     */
    abstract val tail: Permutation<E>

    /** tail이 이미 평가되었는지 여부 */
    protected abstract val isTailDefined: Boolean

    /**
     * 두 순열을 연결합니다.
     *
     * ```kotlin
     * val a = permutationOf(1, 2)
     * val b = permutationOf(3, 4)
     * val combined = (a + b).toList()   // [1, 2, 3, 4]
     * ```
     *
     * @param other 뒤에 연결할 순열
     * @return 연결된 새 순열
     */
    operator fun plus(other: Permutation<E>): Permutation<E> {
        return concat(this, other)
    }

    override operator fun get(index: Int): E {
        if (index < 0) {
            throw IndexOutOfBoundsException(index.toString())
        }

        var curr = this
        (index downTo 1).forEach {
            if (curr.tail.isEmpty()) {
                throw IndexOutOfBoundsException(index.toString())
            }
            curr = curr.tail
        }
        return curr.head
    }

    /**
     * 각 요소에 [mapper]를 적용한 새 순열을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val doubled = perm.map { it * 2 }.toList()   // [2, 4, 6]
     *
     * val chars = permutationOf('a', 'b', 'c')
     * val upper = chars.map(Char::uppercaseChar).toList()   // ['A', 'B', 'C']
     * ```
     *
     * @param mapper 변환 함수
     * @return 변환된 순열
     */
    abstract fun <R: Any?> map(mapper: (E) -> R): Permutation<R>

    /**
     * 이 순열을 [Sequence]로 변환합니다.
     *
     * @return 순열의 요소를 순회하는 시퀀스
     */
    open fun sequence(): Sequence<E> = Sequence { this.iterator() }

    override fun toString(): String {
        return mkString(", ", "[", "]", true)
    }

    /**
     * 순열의 요소들을 문자열로 연결합니다.
     *
     * @param separator 구분자
     * @param prefix 접두사
     * @param postfix 접미사
     * @param lazy true이면 평가되지 않은 tail은 `?`로 표시
     * @return 포맷된 문자열
     */
    @JvmOverloads
    open fun mkString(
        separator: CharSequence = ", ",
        prefix: CharSequence = "",
        postfix: CharSequence = "",
        lazy: Boolean = false,
    ): String {
        val sb = StringBuilder(prefix)

        var curr: Permutation<E> = this
        while (!curr.isEmpty()) {
            sb.append(curr.head)
            if (!lazy || curr.isTailDefined) {
                if (!curr.tail.isEmpty()) {
                    sb.append(separator)
                }
                curr = curr.tail
            } else {
                sb.append(separator).append("?")
                break
            }
        }
        return sb.append(postfix).toString()
    }

    /**
     * [predicate] 조건을 만족하는 요소만 포함하는 순열을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3, 4, 5)
     * val evens = perm.filter { it % 2 == 0 }.toList()   // [2, 4]
     * ```
     *
     * @param predicate 필터 조건
     * @return 필터링된 순열
     */
    abstract fun filter(predicate: (E) -> Boolean): Permutation<E>

    /**
     * 각 요소를 [Iterable]로 변환하고 평탄화한 순열을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val flat = perm.flatMap { listOf(it, it * 10) }.toList()
     * // [1, 10, 2, 20, 3, 30]
     * ```
     *
     * @param mapper 변환 함수
     * @return 평탄화된 순열
     */
    abstract fun <R> flatMap(mapper: (E) -> Iterable<R>): Permutation<R>

    /**
     * 최대 [maxSize]개의 요소만 포함하는 순열을 반환합니다.
     * [take]의 별칭입니다.
     *
     * @param maxSize 최대 요소 수
     * @return 제한된 순열
     */
    fun limit(maxSize: Long): Permutation<E> = take(maxSize)

    /**
     * 순열의 모든 요소를 리스트로 변환합니다.
     * 무한 순열에서는 사용하지 마세요.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val list = perm.toList()   // [1, 2, 3]
     * ```
     *
     * @return 요소 리스트
     */
    open fun toList(): List<E> = mutableListOf<E>().also { it.addAll(this.force()) }

    /**
     * 최대 [maxSize]개의 요소만 포함하는 순열을 반환합니다.
     *
     * ```kotlin
     * val naturals = numbers(1)                   // 무한 순열 1, 2, 3, ...
     * val first5 = naturals.take(5).toList()      // [1, 2, 3, 4, 5]
     * ```
     *
     * @param maxSize 최대 요소 수 (0 이상)
     * @return 제한된 순열
     */
    open fun take(maxSize: Long): Permutation<E> {
        require(maxSize >= 0L)
        return if (maxSize == 0L) emptyPermutation() else takeUnsafe(maxSize)
    }

    abstract fun takeUnsafe(maxSize: Long): Permutation<E>

    /**
     * 앞에서 [startInclusive]개의 요소를 건너뛴 순열을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3, 4, 5)
     * val rest = perm.drop(2).toList()   // [3, 4, 5]
     * ```
     *
     * @param startInclusive 건너뛸 요소 수 (0 이상)
     * @return 건너뛴 후의 순열
     */
    open fun drop(startInclusive: Long): Permutation<E> {
        require(startInclusive >= 0)
        return dropUnsafe(startInclusive)
    }

    open fun dropUnsafe(startInclusive: Long): Permutation<E> {
        return if (startInclusive > 0) tail.drop(startInclusive - 1) else this
    }

    override fun subList(fromIndex: Int, toIndex: Int): Permutation<E> = slice(fromIndex.toLong(), toIndex.toLong())

    /**
     * [startInclusive]부터 [endExclusive]까지의 요소를 포함하는 부분 순열을 반환합니다.
     *
     * @param startInclusive 시작 인덱스 (포함)
     * @param endExclusive 종료 인덱스 (제외)
     * @return 부분 순열
     */
    open fun slice(startInclusive: Long, endExclusive: Long): Permutation<E> {
        require(startInclusive >= 0 && startInclusive <= endExclusive)
        return dropUnsafe(startInclusive).takeUnsafe(endExclusive - startInclusive)
    }

    /**
     * 각 요소에 [action]을 적용합니다.
     * 무한 순열에서는 사용하지 마세요.
     *
     * @param action 각 요소에 적용할 함수
     */
    open fun forEach(action: (E) -> Unit) {
        tailrec fun traverse(seq: Permutation<E>, action: (E) -> Unit) {
            if (seq.isEmpty()) return
            action(seq.head)
            traverse(seq.tail, action)
        }
        traverse(this, action)
    }

    /**
     * 순열의 요소들을 주어진 연산으로 축약합니다.
     * 순열이 비어있거나 요소가 하나뿐이면 null을 반환합니다.
     *
     * @param operation 축약 연산
     * @return 축약 결과 또는 null
     */
    @Suppress("UNCHECKED_CAST")
    fun <R: E> reduce(operation: (R, E) -> R): R? {
        if (isEmpty()) {
            return null
        }
        if (tail.isEmpty()) {
            return head as R
        }
        var result = head as R
        var curr = tail
        while (!curr.isEmpty()) {
            result = operation(result, curr.head)
            curr = curr.tail
        }
        return result
    }

    /**
     * 초기값과 함께 순열의 요소들을 축약합니다.
     *
     * @param identity 초기값
     * @param operation 축약 연산
     * @return 축약 결과
     */
    fun <R> reduce(identity: R, operation: (R, E) -> R): R {
        var result = identity
        var curr = this
        while (!curr.isEmpty()) {
            result = operation(result, curr.head)
            curr = curr.tail
        }
        return result
    }

    /**
     * 주어진 속성 기준으로 최대 요소를 반환합니다.
     *
     * @param propertyFunc 비교할 속성 추출 함수
     * @return 최대 요소 또는 null
     */
    fun <C: Comparable<C>> maxBy(propertyFunc: (E) -> C): E? = max(propertyFunToComparator(propertyFunc))

    /**
     * 주어진 속성 기준으로 최소 요소를 반환합니다.
     *
     * @param propertyFunc 비교할 속성 추출 함수
     * @return 최소 요소 또는 null
     */
    fun <C: Comparable<C>> minBy(propertyFunc: (E) -> C): E? = min(propertyFunToComparator(propertyFunc))

    fun max(comparator: (a: E, b: E) -> Int): E? = max(Comparator(comparator))
    fun min(comparator: (a: E, b: E) -> Int): E? = min(Comparator(comparator))

    open fun max(comparator: Comparator<in E>): E? = greatestByComparator(comparator)
    open fun min(comparator: Comparator<in E>): E? = greatestByComparator(comparator.reversed())

    private fun <C: Comparable<C>> propertyFunToComparator(propertyFunc: (E) -> C): Comparator<in E> {
        return Comparator { a, b ->
            val aProperty = propertyFunc(a)
            val bProperty = propertyFunc(b)
            aProperty.compareTo(bProperty)
        }
    }

    private fun greatestByComparator(comparator: Comparator<in E>): E? {
        if (isEmpty()) {
            return null
        }
        if (tail.isEmpty()) {
            return head
        }

        var minSoFar = head
        var curr = this.tail
        while (!curr.isEmpty()) {
            minSoFar = maxByComparator(minSoFar, curr.head, comparator)
            curr = curr.tail
        }
        return minSoFar
    }

    /**
     * 순열의 요소 개수입니다.
     * 무한 순열에서는 사용하지 마세요.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val n = perm.size          // 3
     * val empty = emptyPermutation<Int>()
     * val isZero = empty.size    // 0
     * ```
     */
    override val size: Int
        get() {
            var count = 0
            var curr: Permutation<E> = this
            while (!curr.isEmpty()) {
                count++
                curr = curr.tail
            }
            return count
        }

    override fun iterator(): MutableIterator<E> {
        return PermutationIterator(this)
    }

    /**
     * 하나라도 [predicate]를 만족하는 요소가 있는지 확인합니다.
     *
     * @param predicate 조건
     * @return 조건을 만족하는 요소가 있으면 true
     */
    open fun anyMatch(predicate: (E) -> Boolean): Boolean = predicate(head) || tail.anyMatch(predicate)

    /**
     * 모든 요소가 [predicate]를 만족하는지 확인합니다.
     *
     * @param predicate 조건
     * @return 모든 요소가 조건을 만족하면 true
     */
    open fun allMatch(predicate: (E) -> Boolean): Boolean = predicate(head) && tail.allMatch(predicate)

    /**
     * 어떤 요소도 [predicate]를 만족하지 않는지 확인합니다.
     *
     * @param predicate 조건
     * @return 조건을 만족하는 요소가 없으면 true
     */
    open fun noneMatch(predicate: (E) -> Boolean): Boolean = !predicate(head) && tail.noneMatch(predicate)

    /**
     * 두 순열의 요소를 하나씩 짝지어 [zipper]로 결합한 순열을 반환합니다.
     * 두 순열 중 짧은 쪽 길이에 맞춰 결합이 끝납니다.
     *
     * ```kotlin
     * val a = permutationOf(1, 2, 3)
     * val b = permutationOf(10, 20, 30)
     * val sums = a.zip(b) { x, y -> x + y }.toList()   // [11, 22, 33]
     *
     * val names = permutationOf("Alice", "Bob")
     * val scores = permutationOf(90, 85, 70)
     * val pairs = names.zip(scores) { n, s -> "$n:$s" }.toList()
     * // ["Alice:90", "Bob:85"]
     * ```
     *
     * @param second 두 번째 순열
     * @param zipper 결합 함수
     * @return 결합된 순열
     */
    open fun <S, R> zip(second: Permutation<S>, zipper: (E, S) -> R): Permutation<R> {
        if (second.isEmpty()) {
            return empty()
        } else {
            val headsZipped = zipper.invoke(head, second.head)
            return cons(headsZipped) { tail.zip(second.tail, zipper) }
        }
    }

    /**
     * [predicate]를 만족하는 동안의 요소만 포함하는 순열을 반환합니다.
     *
     * @param predicate 조건
     * @return 조건을 만족하는 접두 순열
     */
    open fun takeWhile(predicate: (E) -> Boolean): Permutation<E> {
        return if (predicate(head)) {
            cons(head) { tail.takeWhile(predicate) }
        } else {
            emptyPermutation()
        }
    }

    /**
     * [predicate]를 만족하는 동안의 요소를 건너뛴 순열을 반환합니다.
     *
     * @param predicate 조건
     * @return 조건을 불만족하는 첫 요소부터의 순열
     */
    open fun dropWhile(predicate: (E) -> Boolean): Permutation<E> {
        return if (predicate(head)) {
            tail.dropWhile(predicate)
        } else {
            this
        }
    }

    /**
     * 슬라이딩 윈도우 방식으로 부분 리스트들의 순열을 생성합니다.
     *
     * @param size 윈도우 크기 (1 이상)
     * @return 슬라이딩 윈도우 순열
     */
    open fun sliding(size: Long): Permutation<List<E>> {
        require(size > 0)
        return slidingUnsafe(size)
    }

    open fun slidingUnsafe(size: Long): Permutation<List<E>> {
        val window = take(size).toList()
        return cons(window) { tail.slidingFullOnly(size) }
    }

    open fun slidingFullOnly(size: Long): Permutation<List<E>> {
        val window = take(size).toList()
        return if (window.size < size) {
            emptyPermutation()
        } else {
            cons(window) { tail.slidingFullOnly(size) }
        }
    }

    /**
     * 고정 크기 그룹으로 요소를 나눈 순열을 생성합니다.
     *
     * @param size 그룹 크기 (1 이상)
     * @return 그룹화된 순열
     */
    open fun grouped(size: Long): Permutation<List<E>> {
        require(size > 0)
        return groupedUnsafe(size)
    }

    open fun groupedUnsafe(size: Long): Permutation<List<E>> {
        val window = take(size).toList()
        return cons(window) { drop(size).groupedUnsafe(size) }
    }

    /**
     * 초기값으로부터 각 요소에 함수를 누적 적용한 순열을 생성합니다.
     *
     * @param initial 초기값
     * @param binFunc 누적 함수
     * @return 누적 결과 순열
     */
    open fun scan(initial: E, binFunc: (E, E) -> E): Permutation<E> {
        return cons(initial) { tail.scan(binFunc(initial, head), binFunc) }
    }

    /**
     * 중복 요소를 제거한 순열을 반환합니다.
     *
     * @return 중복이 제거된 순열
     */
    open fun distinct(): Permutation<E> = filterOutSeen(hashSetOf())

    open fun filterOutSeen(exclude: MutableSet<E>): Permutation<E> {
        val moreDistinct = filter { !exclude.contains(it) }
        if (moreDistinct.isEmpty())
            return emptyPermutation()

        val next = moreDistinct.head
        exclude.add(next)
        return cons(next) { moreDistinct.tail.filterOutSeen(exclude) }
    }

    /**
     * 요소를 자연 순서로 정렬한 순열을 반환합니다.
     * 무한 순열에서는 사용하지 마세요.
     *
     * @return 정렬된 순열
     */
    @Suppress("UNCHECKED_CAST")
    open fun sorted(): Permutation<E> {
        return sorted(Comparator { o1, o2 -> (o1 as Comparable<E>).compareTo(o2) })
    }

    /**
     * 주어진 비교 함수로 정렬한 순열을 반환합니다.
     *
     * @param comparator 비교 함수
     * @return 정렬된 순열
     */
    fun sorted(comparator: (E1: E, E2: E) -> Int): Permutation<E> {
        return sorted(Comparator(comparator))
    }

    /**
     * 주어진 [Comparator]로 정렬한 순열을 반환합니다.
     *
     * @param comparator 비교기
     * @return 정렬된 순열
     */
    open fun sorted(comparator: Comparator<in E>): Permutation<E> {
        val sorted = (this as List<E>).toMutableList().apply { sortWith(comparator) }
        return permutationOf(sorted.iterator())
    }

    /**
     * 주어진 접두사로 시작하는지 확인합니다.
     *
     * @param prefix 접두사
     * @return 접두사로 시작하면 true
     */
    open fun startsWith(prefix: Iterable<E>): Boolean = startsWith(prefix.iterator())

    /**
     * 주어진 Iterator의 요소로 시작하는지 확인합니다.
     *
     * @param iterator 접두사 Iterator
     * @return 접두사로 시작하면 true
     */
    open fun startsWith(iterator: Iterator<E>): Boolean {
        return !iterator.hasNext() ||
                ((head?.equals(iterator.next()) ?: false) && tail.startsWith(iterator))
    }

    /**
     * 순열의 모든 요소를 강제로 평가합니다.
     * 무한 순열에서는 사용하지 마세요.
     *
     * @return 평가된 순열
     */
    open fun force(): Permutation<E> {
        tail.force()
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is Permutation<*>) {
            return !other.isEmpty() && head == other.head && tail == other.tail
        }
        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(head, tail)
    }

    companion object {

        /**
         * 빈 순열을 반환합니다.
         *
         * @return 빈 순열
         */
        @JvmStatic
        fun <E> empty(): Permutation<E> = Nil.instance()

        private fun <E> maxByComparator(first: E, second: E, comparator: Comparator<in E>): E {
            return if (comparator.compare(first, second) >= 0) first else second
        }
    }
}
