package io.bluetape4k.mutiny

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.groups.MultiRepetition
import io.smallrye.mutiny.groups.UniRepeat
import java.util.concurrent.CompletionStage
import java.util.function.Supplier
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream
import kotlin.streams.asSequence
import kotlin.streams.asStream

/**
 * [items]를 제공하는 [Multi]를 생성합니다.
 *
 * @param T  요소의 수형
 * @param items emit 할 요소들
 * @return [Multi] 인스턴스
 */
fun <T> multiOf(vararg items: T): Multi<T> {
    return Multi.createFrom().items(*items)
}

/**
 * [start, endExclusive) 범위의 int 값을 publish 하는 [Multi]를 생성합니다.
 *
 * @param start         하한 값
 * @param endExclusive  상한 값 (제외)
 * @return [Multi] instance
 */
fun multiRangeOf(start: Int, endExclusive: Int): Multi<Int> {
    return Multi.createFrom().range(start, endExclusive)
}

fun <T> Multi<T>.onEach(callback: (T) -> Unit): Multi<T> {
    return onItem().invoke { item: T -> callback(item) }
}

fun <T> Iterable<T>.asMulti(): Multi<T> = Multi.createFrom().iterable(this)

fun <T> Sequence<T>.asMulti(): Multi<T> = Multi.createFrom().items { this.asStream() }

fun <T> Stream<T>.asMulti(): Multi<T> = Multi.createFrom().items { this }

fun IntStream.asMulti(): Multi<Int> = asSequence().asMulti()
fun LongStream.asMulti(): Multi<Long> = asSequence().asMulti()
fun DoubleStream.asMulti(): Multi<Double> = asSequence().asMulti()

fun CharProgression.asMulti(): Multi<Char> = asSequence().asMulti()
fun IntProgression.asMulti(): Multi<Int> = asSequence().asMulti()
fun LongProgression.asMulti(): Multi<Long> = asSequence().asMulti()

fun IntArray.asMulti(): Multi<Int> = asIterable().asMulti()
fun LongArray.asMulti(): Multi<Long> = asIterable().asMulti()
fun FloatArray.asMulti(): Multi<Float> = asIterable().asMulti()
fun DoubleArray.asMulti(): Multi<Double> = asIterable().asMulti()

fun <T> MultiRepetition.deferUni(supplier: () -> Uni<T>): UniRepeat<T> {
    return this.uni(Supplier { supplier() })
}

fun <T> MultiRepetition.deferCompletionStage(supplier: () -> CompletionStage<T>): UniRepeat<T> {
    return this.completionStage { supplier() }
}
