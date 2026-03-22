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
 * @param T 요소 타입
 * @param underlying 래핑할 순열
 */
class PermutationStream<T>(val underlying: Permutation<T>): Stream<T> {

    private var closed: Boolean = false
    private var closeHandler: Runnable? = null

    private fun underlyingStream(): Stream<T> = underlying.toList().stream()

    override fun reduce(identity: T, accumulator: BinaryOperator<T>): T {
        return underlying.reduce(identity) { a, b -> accumulator.apply(a, b) }
    }

    override fun <U> reduce(identity: U, accumulator: BiFunction<U, in T, U>, combiner: BinaryOperator<U>): U {
        return underlying.reduce(identity) { a, b -> accumulator.apply(a, b) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun reduce(accumulator: BinaryOperator<T>): Optional<T> {
        return Optional.ofNullable(underlying.reduce { a: T, b: T -> accumulator.apply(a, b) }) as Optional<T>
    }

    override fun iterator(): MutableIterator<T> {
        return underlying.iterator()
    }

    override fun count(): Long {
        return underlying.size.toLong()
    }

    override fun mapToLong(mapper: ToLongFunction<in T>?): LongStream {
        return underlyingStream().mapToLong(mapper)
    }

    override fun allMatch(predicate: Predicate<in T>): Boolean {
        return underlying.allMatch { predicate.test(it) }
    }

    override fun <R> map(mapper: Function<in T, out R>): Stream<R> {
        return underlying.map { mapper.apply(it) }.toStream()
    }

    @Suppress("UNCHECKED_CAST")
    override fun findFirst(): Optional<T> {
        return Optional.ofNullable(underlying.head) as Optional<T>
    }

    @Suppress("UNCHECKED_CAST")
    override fun findAny(): Optional<T> {
        return Optional.ofNullable(underlying.head) as Optional<T>
    }

    override fun mapToInt(mapper: ToIntFunction<in T>): IntStream {
        return underlyingStream().mapToInt(mapper)
    }

    override fun forEach(action: Consumer<in T>) {
        underlying.forEach { action.accept(it) }
    }

    override fun <R> flatMap(mapper: Function<in T, out Stream<out R>>): Stream<R> {
        return underlying.flatMap { mapper.apply(it).collect(Collectors.toList<R>()) }.toStream()
    }

    override fun flatMapToDouble(mapper: Function<in T, out DoubleStream>?): DoubleStream {
        return underlyingStream().flatMapToDouble(mapper)
    }

    override fun spliterator(): Spliterator<T> {
        return underlyingStream().spliterator()
    }

    override fun parallel(): Stream<T> = underlyingStream().parallel()

    override fun <R, A> collect(collector: Collector<in T, A, R>?): R {
        return underlyingStream().collect(collector)
    }

    override fun <R> collect(
        supplier: Supplier<R>?,
        accumulator: BiConsumer<R, in T>?,
        combiner: BiConsumer<R, R>?,
    ): R {
        return underlyingStream().collect(supplier, accumulator, combiner)
    }

    @Suppress("UNCHECKED_CAST")
    override fun min(comparator: Comparator<in T>): Optional<T> {
        return Optional.ofNullable(underlying.min(comparator)) as Optional<T>
    }

    override fun noneMatch(predicate: Predicate<in T>): Boolean {
        return underlying.noneMatch { predicate.test(it) }
    }

    override fun sorted(): Stream<T> {
        return underlying.sorted().toStream()
    }

    override fun sorted(comparator: Comparator<in T>): Stream<T> {
        return underlying.sorted(comparator).toStream()
    }

    override fun flatMapToLong(mapper: Function<in T, out LongStream>): LongStream {
        return underlyingStream().flatMapToLong(mapper)
    }

    override fun isParallel(): Boolean = false

    override fun peek(action: Consumer<in T>): Stream<T> {
        return underlying.map {
            action.accept(it)
            it
        }.toStream()
    }

    override fun distinct(): Stream<T> {
        return underlying.distinct().toStream()
    }

    override fun filter(predicate: Predicate<in T>): Stream<T> {
        return underlying.filter { predicate.test(it) }.toStream()
    }

    override fun unordered(): Stream<T> = this

    override fun forEachOrdered(action: Consumer<in T>) {
        underlying.forEach { action.accept(it) }
    }

    override fun limit(maxSize: Long): Stream<T> {
        return underlying.limit(maxSize).toStream()
    }

    override fun toArray(): Array<out Any?> {
        val array = arrayOfNulls<Any>(underlying.size)
        copyToArray(array)
        return array
    }

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

    override fun sequential(): Stream<T> = this

    override fun anyMatch(predicate: Predicate<in T>): Boolean {
        return underlying.anyMatch { predicate.test(it) }
    }

    override fun flatMapToInt(mapper: Function<in T, out IntStream>?): IntStream {
        return underlyingStream().flatMapToInt(mapper)
    }

    override fun onClose(closeHandler: Runnable): Stream<T> {
        this.closeHandler = closeHandler
        return this
    }

    override fun close() {
        closeHandler?.run()
        closed = true
    }

    @Suppress("UNCHECKED_CAST")
    override fun max(comparator: Comparator<in T>): Optional<T> {
        return Optional.ofNullable(underlying.max(comparator)) as Optional<T>
    }

    override fun skip(n: Long): Stream<T> {
        return underlying.drop(n).toStream()
    }

    override fun mapToDouble(mapper: ToDoubleFunction<in T>?): DoubleStream {
        return underlyingStream().mapToDouble(mapper)
    }
}
