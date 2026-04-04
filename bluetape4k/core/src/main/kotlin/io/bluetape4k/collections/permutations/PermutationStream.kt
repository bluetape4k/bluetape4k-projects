package io.bluetape4k.collections.permutations

import java.util.*
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.BinaryOperator
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.IntFunction
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.function.ToDoubleFunction
import java.util.function.ToIntFunction
import java.util.function.ToLongFunction
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

/**
 * [Permutation]을 Java [Stream] 형태로 제공하기 위한 래퍼 클래스입니다.
 *
 * 순열의 지연 평가 특성을 유지하면서 Java Stream API의 연산을 사용할 수 있습니다.
 *
 * ```kotlin
 * val numbers = permutationOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
 * val stream = PermutationStream(numbers)
 *
 * // map → filter → skip → limit → sorted → distinct 체이닝
 * val result = stream
 *     .map { it * 2 }
 *     .filter { it > 4 }
 *     .skip(1)
 *     .limit(4)
 *     .sorted()
 *     .distinct()
 *     .collect(Collectors.toList())
 * // result = [8, 10, 12, 14]
 *
 * // 최솟값 조회
 * val min = PermutationStream(numbers).min(compareBy { it })
 * // min = Optional[1]
 *
 * // 누적 합산
 * val sum = PermutationStream(numbers).reduce(0) { a, b -> a + b }
 * // sum = 55
 * ```
 *
 * @param T 요소 타입
 * @param underlying 래핑할 순열
 */
class PermutationStream<T>(val underlying: Permutation<T>): Stream<T> {

    private var closed: Boolean = false
    private var closeHandler: Runnable? = null

    private fun underlyingStream(): Stream<T> = underlying.toList().stream()

    /**
     * 초깃값과 누산 함수를 사용하여 모든 요소를 하나의 값으로 줄입니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3, 4, 5)
     * val sum = PermutationStream(perm).reduce(0) { a, b -> a + b }
     * // sum = 15
     * ```
     *
     * @param identity 초깃값
     * @param accumulator 두 값을 합치는 함수
     * @return 누산 결과
     */
    override fun reduce(identity: T, accumulator: BinaryOperator<T>): T {
        return underlying.reduce(identity) { a, b -> accumulator.apply(a, b) }
    }

    /**
     * 초깃값과 누산·결합 함수를 사용하여 다른 타입 [U]로 줄입니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val joined = PermutationStream(perm).reduce("", { s, i -> s + i }, { a, b -> a + b })
     * // joined = "123"
     * ```
     *
     * @param identity 초깃값
     * @param accumulator 누산 함수
     * @param combiner 병렬 스트림에서 부분 결과를 합치는 함수
     * @return 누산 결과
     */
    override fun <U> reduce(identity: U, accumulator: BiFunction<U, in T, U>, combiner: BinaryOperator<U>): U {
        return underlying.reduce(identity) { a, b -> accumulator.apply(a, b) }
    }

    /**
     * 누산 함수만으로 요소를 줄입니다. 순열이 비어 있으면 [Optional.empty]를 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(3, 1, 4, 1, 5)
     * val max = PermutationStream(perm).reduce { a, b -> if (a > b) a else b }
     * // max = Optional[5]
     * ```
     *
     * @param accumulator 두 값을 합치는 함수
     * @return 누산 결과 (순열이 비어 있으면 empty)
     */
    @Suppress("UNCHECKED_CAST")
    override fun reduce(accumulator: BinaryOperator<T>): Optional<T> {
        return Optional.ofNullable(underlying.reduce { a: T, b: T -> accumulator.apply(a, b) }) as Optional<T>
    }

    /**
     * 순열의 반복자를 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf("a", "b", "c")
     * val iter = PermutationStream(perm).iterator()
     * iter.next() // "a"
     * ```
     *
     * @return 가변 반복자
     */
    override fun iterator(): MutableIterator<T> {
        return underlying.iterator()
    }

    /**
     * 순열의 요소 수를 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(10, 20, 30)
     * PermutationStream(perm).count() // 3L
     * ```
     *
     * @return 요소 수
     */
    override fun count(): Long {
        return underlying.size.toLong()
    }

    /**
     * 각 요소를 [Long]으로 변환하여 [LongStream]을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val longStream = PermutationStream(perm).mapToLong { it.toLong() }
     * longStream.sum() // 6L
     * ```
     *
     * @param mapper 요소를 Long으로 변환하는 함수
     * @return LongStream
     */
    override fun mapToLong(mapper: ToLongFunction<in T>?): LongStream {
        return underlyingStream().mapToLong(mapper)
    }

    /**
     * 모든 요소가 조건을 만족하는지 검사합니다.
     *
     * ```kotlin
     * val perm = permutationOf(2, 4, 6)
     * PermutationStream(perm).allMatch { it % 2 == 0 } // true
     * ```
     *
     * @param predicate 검사 조건
     * @return 모든 요소가 조건을 만족하면 true
     */
    override fun allMatch(predicate: Predicate<in T>): Boolean {
        return underlying.allMatch { predicate.test(it) }
    }

    /**
     * 각 요소를 변환하여 새로운 [Stream]을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val strings = PermutationStream(perm).map { it.toString() }.collect(Collectors.toList())
     * // strings = ["1", "2", "3"]
     * ```
     *
     * @param mapper 변환 함수
     * @return 변환된 Stream
     */
    override fun <R> map(mapper: Function<in T, out R>): Stream<R> {
        return underlying.map { mapper.apply(it) }.toStream()
    }

    /**
     * 첫 번째 요소를 [Optional]로 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(10, 20, 30)
     * PermutationStream(perm).findFirst() // Optional[10]
     * ```
     *
     * @return 첫 번째 요소 (비어 있으면 empty)
     */
    @Suppress("UNCHECKED_CAST")
    override fun findFirst(): Optional<T> {
        return Optional.ofNullable(underlying.head) as Optional<T>
    }

    /**
     * 임의의 요소를 [Optional]로 반환합니다. 현재 구현에서는 첫 번째 요소를 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(10, 20, 30)
     * PermutationStream(perm).findAny() // Optional[10]
     * ```
     *
     * @return 임의 요소 (비어 있으면 empty)
     */
    @Suppress("UNCHECKED_CAST")
    override fun findAny(): Optional<T> {
        return Optional.ofNullable(underlying.head) as Optional<T>
    }

    /**
     * 각 요소를 [Int]로 변환하여 [IntStream]을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1L, 2L, 3L)
     * val intStream = PermutationStream(perm).mapToInt { it.toInt() }
     * intStream.sum() // 6
     * ```
     *
     * @param mapper 요소를 Int로 변환하는 함수
     * @return IntStream
     */
    override fun mapToInt(mapper: ToIntFunction<in T>): IntStream {
        return underlyingStream().mapToInt(mapper)
    }

    /**
     * 각 요소에 대해 소비 함수를 실행합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val collected = mutableListOf<Int>()
     * PermutationStream(perm).forEach { collected += it }
     * // collected = [1, 2, 3]
     * ```
     *
     * @param action 각 요소에 실행할 소비 함수
     */
    override fun forEach(action: Consumer<in T>) {
        underlying.forEach { action.accept(it) }
    }

    /**
     * 각 요소를 [Stream]으로 변환한 뒤 하나의 [Stream]으로 평탄화합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val flat = PermutationStream(perm)
     *     .flatMap { listOf(it, it * 10).stream() }
     *     .collect(Collectors.toList())
     * // flat = [1, 10, 2, 20, 3, 30]
     * ```
     *
     * @param mapper 요소를 Stream으로 변환하는 함수
     * @return 평탄화된 Stream
     */
    override fun <R> flatMap(mapper: Function<in T, out Stream<out R>>): Stream<R> {
        return underlying.flatMap { mapper.apply(it).collect(Collectors.toList<R>()) }.toStream()
    }

    /**
     * 각 요소를 [DoubleStream]으로 변환한 뒤 하나의 [DoubleStream]으로 평탄화합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1.0, 2.0)
     * val ds = PermutationStream(perm).flatMapToDouble { DoubleStream.of(it, it * 2) }
     * ds.toArray() // [1.0, 2.0, 2.0, 4.0]
     * ```
     *
     * @param mapper 요소를 DoubleStream으로 변환하는 함수
     * @return 평탄화된 DoubleStream
     */
    override fun flatMapToDouble(mapper: Function<in T, out DoubleStream>?): DoubleStream {
        return underlyingStream().flatMapToDouble(mapper)
    }

    /**
     * 순열 요소의 [Spliterator]를 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val spliterator = PermutationStream(perm).spliterator()
     * spliterator.estimateSize() // 3
     * ```
     *
     * @return Spliterator
     */
    override fun spliterator(): Spliterator<T> {
        return underlyingStream().spliterator()
    }

    /**
     * 병렬 [Stream]으로 변환합니다. 내부적으로 리스트로 구체화 후 병렬화합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3, 4, 5)
     * val parallelSum = PermutationStream(perm).parallel().mapToInt { it }.sum()
     * // parallelSum = 15
     * ```
     *
     * @return 병렬 Stream
     */
    override fun parallel(): Stream<T> = underlyingStream().parallel()

    /**
     * [Collector]를 사용하여 요소를 수집합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val list = PermutationStream(perm).collect(Collectors.toList())
     * // list = [1, 2, 3]
     * ```
     *
     * @param collector 수집 전략
     * @return 수집 결과
     */
    override fun <R, A> collect(collector: Collector<in T, A, R>?): R {
        return underlyingStream().collect(collector)
    }

    /**
     * supplier/accumulator/combiner를 사용하여 요소를 수집합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val result = PermutationStream(perm).collect(
     *     { mutableListOf<String>() },
     *     { list, item -> list += item.toString() },
     *     { list, items -> list += items },
     * )
     * // result = ["1", "2", "3"]
     * ```
     *
     * @param supplier 컨테이너 생성 함수
     * @param accumulator 요소를 컨테이너에 추가하는 함수
     * @param combiner 두 컨테이너를 합치는 함수
     * @return 수집 결과
     */
    override fun <R> collect(
        supplier: Supplier<R>?,
        accumulator: BiConsumer<R, in T>?,
        combiner: BiConsumer<R, R>?,
    ): R {
        return underlyingStream().collect(supplier, accumulator, combiner)
    }

    /**
     * 비교자를 기준으로 최솟값을 [Optional]로 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(5, 3, 8, 1)
     * PermutationStream(perm).min(compareBy { it }) // Optional[1]
     * ```
     *
     * @param comparator 비교자
     * @return 최솟값 (비어 있으면 empty)
     */
    @Suppress("UNCHECKED_CAST")
    override fun min(comparator: Comparator<in T>): Optional<T> {
        return Optional.ofNullable(underlying.min(comparator)) as Optional<T>
    }

    /**
     * 어떤 요소도 조건을 만족하지 않는지 검사합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 3, 5)
     * PermutationStream(perm).noneMatch { it % 2 == 0 } // true
     * ```
     *
     * @param predicate 검사 조건
     * @return 조건을 만족하는 요소가 없으면 true
     */
    override fun noneMatch(predicate: Predicate<in T>): Boolean {
        return underlying.noneMatch { predicate.test(it) }
    }

    /**
     * 자연 순서로 정렬된 새 [Stream]을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(3, 1, 2)
     * PermutationStream(perm).sorted().collect(Collectors.toList()) // [1, 2, 3]
     * ```
     *
     * @return 정렬된 Stream
     */
    override fun sorted(): Stream<T> {
        return underlying.sorted().toStream()
    }

    /**
     * 지정한 비교자로 정렬된 새 [Stream]을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf("banana", "apple", "cherry")
     * PermutationStream(perm).sorted(compareByDescending { it }).collect(Collectors.toList())
     * // ["cherry", "banana", "apple"]
     * ```
     *
     * @param comparator 정렬 기준 비교자
     * @return 정렬된 Stream
     */
    override fun sorted(comparator: Comparator<in T>): Stream<T> {
        return underlying.sorted(comparator).toStream()
    }

    /**
     * 각 요소를 [LongStream]으로 변환한 뒤 하나의 [LongStream]으로 평탄화합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1L, 2L)
     * val ls = PermutationStream(perm).flatMapToLong { LongStream.of(it, it * 10) }
     * ls.toArray() // [1L, 10L, 2L, 20L]
     * ```
     *
     * @param mapper 요소를 LongStream으로 변환하는 함수
     * @return 평탄화된 LongStream
     */
    override fun flatMapToLong(mapper: Function<in T, out LongStream>): LongStream {
        return underlyingStream().flatMapToLong(mapper)
    }

    /**
     * 병렬 여부를 반환합니다. 항상 `false`입니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * PermutationStream(perm).isParallel // false
     * ```
     *
     * @return false
     */
    override fun isParallel(): Boolean = false

    /**
     * 각 요소를 소비 함수로 처리한 뒤 동일 요소를 그대로 전달하는 [Stream]을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val peeked = mutableListOf<Int>()
     * PermutationStream(perm).peek { peeked += it }.collect(Collectors.toList())
     * // peeked = [1, 2, 3]
     * ```
     *
     * @param action 각 요소에 실행할 소비 함수
     * @return 동일 요소를 전달하는 Stream
     */
    override fun peek(action: Consumer<in T>): Stream<T> {
        return underlying.map {
            action.accept(it)
            it
        }.toStream()
    }

    /**
     * 중복 요소를 제거한 새 [Stream]을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 2, 3, 3, 3)
     * PermutationStream(perm).distinct().collect(Collectors.toList()) // [1, 2, 3]
     * ```
     *
     * @return 중복 제거된 Stream
     */
    override fun distinct(): Stream<T> {
        return underlying.distinct().toStream()
    }

    /**
     * 조건을 만족하는 요소만 남긴 새 [Stream]을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3, 4, 5)
     * PermutationStream(perm).filter { it % 2 == 0 }.collect(Collectors.toList()) // [2, 4]
     * ```
     *
     * @param predicate 필터 조건
     * @return 조건을 만족하는 요소만 포함한 Stream
     */
    override fun filter(predicate: Predicate<in T>): Stream<T> {
        return underlying.filter { predicate.test(it) }.toStream()
    }

    /**
     * 순서가 없는 [Stream]으로 표시합니다. 현재 구현에서는 자기 자신을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * PermutationStream(perm).unordered().collect(Collectors.toList()) // [1, 2, 3]
     * ```
     *
     * @return 순서 없는 Stream (자기 자신)
     */
    override fun unordered(): Stream<T> = this

    /**
     * 각 요소에 대해 소비 함수를 순서대로 실행합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val collected = mutableListOf<Int>()
     * PermutationStream(perm).forEachOrdered { collected += it }
     * // collected = [1, 2, 3]
     * ```
     *
     * @param action 각 요소에 실행할 소비 함수
     */
    override fun forEachOrdered(action: Consumer<in T>) {
        underlying.forEach { action.accept(it) }
    }

    /**
     * 최대 [maxSize]개 요소만 포함한 새 [Stream]을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3, 4, 5)
     * PermutationStream(perm).limit(3).collect(Collectors.toList()) // [1, 2, 3]
     * ```
     *
     * @param maxSize 최대 요소 수
     * @return 제한된 Stream
     */
    override fun limit(maxSize: Long): Stream<T> {
        return underlying.limit(maxSize).toStream()
    }

    /**
     * 모든 요소를 배열로 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * PermutationStream(perm).toArray() // [1, 2, 3]
     * ```
     *
     * @return 요소 배열
     */
    override fun toArray(): Array<out Any?> {
        val array = arrayOfNulls<Any>(underlying.size)
        copyToArray(array)
        return array
    }

    /**
     * generator 함수로 생성한 배열에 모든 요소를 담아 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val arr: Array<Int> = PermutationStream(perm).toArray { size -> arrayOfNulls<Int>(size) }
     * // arr = [1, 2, 3]
     * ```
     *
     * @param generator 배열 생성 함수
     * @return 요소 배열
     */
    @Suppress("UNCHECKED_CAST")
    override fun <A> toArray(generator: IntFunction<Array<A>>): Array<out A> {
        val array = generator.apply(underlying.size)
        copyToArray(array as Array<Any?>)
        return array
    }

    private fun copyToArray(array: Array<Any?>) {
        var cur = underlying
        for (i in array.indices) {
            array[i] = cur.head as Any
            cur = cur.tail
        }
    }

    /**
     * 순차 [Stream]으로 표시합니다. 현재 구현에서는 자기 자신을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * PermutationStream(perm).sequential().collect(Collectors.toList()) // [1, 2, 3]
     * ```
     *
     * @return 순차 Stream (자기 자신)
     */
    override fun sequential(): Stream<T> = this

    /**
     * 적어도 하나의 요소가 조건을 만족하는지 검사합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 3, 4, 7)
     * PermutationStream(perm).anyMatch { it % 2 == 0 } // true
     * ```
     *
     * @param predicate 검사 조건
     * @return 조건을 만족하는 요소가 있으면 true
     */
    override fun anyMatch(predicate: Predicate<in T>): Boolean {
        return underlying.anyMatch { predicate.test(it) }
    }

    /**
     * 각 요소를 [IntStream]으로 변환한 뒤 하나의 [IntStream]으로 평탄화합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2)
     * val is_ = PermutationStream(perm).flatMapToInt { IntStream.of(it, it * 10) }
     * is_.toArray() // [1, 10, 2, 20]
     * ```
     *
     * @param mapper 요소를 IntStream으로 변환하는 함수
     * @return 평탄화된 IntStream
     */
    override fun flatMapToInt(mapper: Function<in T, out IntStream>?): IntStream {
        return underlyingStream().flatMapToInt(mapper)
    }

    /**
     * 스트림 종료 시 실행할 핸들러를 등록합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * var closed = false
     * PermutationStream(perm).onClose { closed = true }.close()
     * // closed = true
     * ```
     *
     * @param closeHandler 종료 시 실행할 Runnable
     * @return 자기 자신
     */
    override fun onClose(closeHandler: Runnable): Stream<T> {
        this.closeHandler = closeHandler
        return this
    }

    /**
     * 스트림을 닫고 등록된 종료 핸들러를 실행합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * var closed = false
     * val stream = PermutationStream(perm).onClose { closed = true }
     * stream.close()
     * // closed = true
     * ```
     */
    override fun close() {
        closeHandler?.run()
        closed = true
    }

    /**
     * 비교자를 기준으로 최댓값을 [Optional]로 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(5, 3, 8, 1)
     * PermutationStream(perm).max(compareBy { it }) // Optional[8]
     * ```
     *
     * @param comparator 비교자
     * @return 최댓값 (비어 있으면 empty)
     */
    @Suppress("UNCHECKED_CAST")
    override fun max(comparator: Comparator<in T>): Optional<T> {
        return Optional.ofNullable(underlying.max(comparator)) as Optional<T>
    }

    /**
     * 앞의 [n]개 요소를 건너뛴 새 [Stream]을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3, 4, 5)
     * PermutationStream(perm).skip(2).collect(Collectors.toList()) // [3, 4, 5]
     * ```
     *
     * @param n 건너뛸 요소 수
     * @return 건너뛴 Stream
     */
    override fun skip(n: Long): Stream<T> {
        return underlying.drop(n).toStream()
    }

    /**
     * 각 요소를 [Double]로 변환하여 [DoubleStream]을 반환합니다.
     *
     * ```kotlin
     * val perm = permutationOf(1, 2, 3)
     * val ds = PermutationStream(perm).mapToDouble { it.toDouble() }
     * ds.sum() // 6.0
     * ```
     *
     * @param mapper 요소를 Double로 변환하는 함수
     * @return DoubleStream
     */
    override fun mapToDouble(mapper: ToDoubleFunction<in T>?): DoubleStream {
        return underlyingStream().mapToDouble(mapper)
    }
}
