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
 * ## 동작/계약
 * - 전달된 [items]를 순서대로 방출하는 새 [Multi]를 생성합니다.
 * - 입력 배열을 변경하지 않습니다.
 *
 * ```kotlin
 * val result = multiOf(1, 2, 3).collect().asList().await().indefinitely()
 * // result == [1, 2, 3]
 * ```
 */
fun <T> multiOf(vararg items: T): Multi<T> {
    return Multi.createFrom().items(*items)
}

/**
 * [start, endExclusive) 범위의 int 값을 publish 하는 [Multi]를 생성합니다.
 *
 * ## 동작/계약
 * - 시작값은 포함하고 [endExclusive]는 제외합니다.
 * - 범위 길이만큼 아이템을 생성하므로 추가 컬렉션 할당 없이 스트림으로 방출합니다.
 *
 * ```kotlin
 * val result = multiRangeOf(10, 15).collect().asList().await().indefinitely()
 * // result == [10, 11, 12, 13, 14]
 * ```
 */
fun multiRangeOf(start: Int, endExclusive: Int): Multi<Int> {
    return Multi.createFrom().range(start, endExclusive)
}

/**
 * 각 아이템 방출 시 부수 효과 콜백을 실행합니다.
 *
 * ## 동작/계약
 * - 원본 아이템은 변경 없이 그대로 전달됩니다.
 * - [callback] 예외는 `Multi` 실패로 전파됩니다.
 *
 * ```kotlin
 * val seen = mutableListOf<Int>()
 * val result = multiOf(1, 2).onEach { seen += it }.collect().asList().await().indefinitely()
 * // seen == [1, 2]
 * // result == [1, 2]
 * ```
 */
fun <T> Multi<T>.onEach(callback: (T) -> Unit): Multi<T> {
    return onItem().invoke { item: T -> callback(item) }
}

/**
 * [Iterable]을 [Multi]로 변환합니다.
 *
 * ## 동작/계약
 * - 반복 순서를 유지해 요소를 방출합니다.
 * - 수신 객체를 변경하지 않고 새 `Multi`를 생성합니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).asMulti().collect().asList().await().indefinitely()
 * // result == [1, 2, 3]
 * ```
 */
fun <T> Iterable<T>.asMulti(): Multi<T> = Multi.createFrom().iterable(this)

/**
 * [Sequence]를 [Multi]로 변환합니다.
 *
 * ## 동작/계약
 * - `Sequence`를 `Stream`으로 브리지해 지연 평가 순서를 유지합니다.
 * - 각 구독에서 시퀀스를 순회하며 새 값을 방출합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1, 2, 3).asMulti().collect().asList().await().indefinitely()
 * // result == [1, 2, 3]
 * ```
 */
fun <T> Sequence<T>.asMulti(): Multi<T> = Multi.createFrom().items { this.asStream() }

/**
 * Java [Stream]을 [Multi]로 변환합니다.
 *
 * ## 동작/계약
 * - 전달된 스트림을 그대로 사용해 아이템을 방출합니다.
 * - 스트림 재사용은 지원되지 않으므로 한 번 소비된 스트림은 다시 구독할 수 없습니다.
 *
 * ```kotlin
 * val result = Stream.of("a", "b", "c").asMulti().collect().asList().await().indefinitely()
 * // result == ["a", "b", "c"]
 * ```
 */
fun <T> Stream<T>.asMulti(): Multi<T> = Multi.createFrom().items { this }

/**
 * [IntStream]을 [Multi]로 변환합니다.
 *
 * ## 동작/계약
 * - Kotlin `Sequence`로 변환 후 `Multi`를 생성합니다.
 * - 기존 스트림은 소비되며 변경되지 않습니다.
 *
 * ```kotlin
 * val result = IntStream.of(1, 2, 3).asMulti().collect().asList().await().indefinitely()
 * // result == [1, 2, 3]
 * ```
 */
fun IntStream.asMulti(): Multi<Int> = asSequence().asMulti()

/**
 * [LongStream]을 [Multi]로 변환합니다.
 *
 * ## 동작/계약
 * - 요소 순서를 유지해 `Long` 값을 방출합니다.
 * - 새 `Multi`를 반환하며 수신 스트림을 mutate 하지 않습니다.
 *
 * ```kotlin
 * val result = LongStream.of(1L, 2L).asMulti().collect().asList().await().indefinitely()
 * // result == [1L, 2L]
 * ```
 */
fun LongStream.asMulti(): Multi<Long> = asSequence().asMulti()

/**
 * [DoubleStream]을 [Multi]로 변환합니다.
 *
 * ## 동작/계약
 * - 요소 순서를 유지해 `Double` 값을 방출합니다.
 * - 스트림은 구독 시 소비됩니다.
 *
 * ```kotlin
 * val result = DoubleStream.of(1.0, 2.0).asMulti().collect().asList().await().indefinitely()
 * // result == [1.0, 2.0]
 * ```
 */
fun DoubleStream.asMulti(): Multi<Double> = asSequence().asMulti()

/**
 * [CharProgression]을 [Multi]로 변환합니다.
 *
 * ## 동작/계약
 * - progression의 증가 규칙을 그대로 따라 순차 방출합니다.
 * - 새 `Multi` 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val result = ('a'..'c').asMulti().collect().asList().await().indefinitely()
 * // result == ['a', 'b', 'c']
 * ```
 */
fun CharProgression.asMulti(): Multi<Char> = asSequence().asMulti()

/**
 * [IntProgression]을 [Multi]로 변환합니다.
 *
 * ## 동작/계약
 * - progression 순서와 step을 유지해 방출합니다.
 * - 수신 progression을 변경하지 않습니다.
 *
 * ```kotlin
 * val result = (1..3).asMulti().collect().asList().await().indefinitely()
 * // result == [1, 2, 3]
 * ```
 */
fun IntProgression.asMulti(): Multi<Int> = asSequence().asMulti()

/**
 * [LongProgression]을 [Multi]로 변환합니다.
 *
 * ## 동작/계약
 * - progression 규칙을 유지해 `Long` 값을 방출합니다.
 * - 새 `Multi`를 반환합니다.
 *
 * ```kotlin
 * val result = (1L..3L).asMulti().collect().asList().await().indefinitely()
 * // result == [1L, 2L, 3L]
 * ```
 */
fun LongProgression.asMulti(): Multi<Long> = asSequence().asMulti()

/**
 * [IntArray]를 [Multi]로 변환합니다.
 *
 * ## 동작/계약
 * - 배열 순서를 유지해 아이템을 방출합니다.
 * - `asIterable()` 브리지를 사용하므로 배열 복사는 발생하지 않습니다.
 *
 * ```kotlin
 * val result = intArrayOf(1, 2, 3).asMulti().collect().asList().await().indefinitely()
 * // result == [1, 2, 3]
 * ```
 */
fun IntArray.asMulti(): Multi<Int> = asIterable().asMulti()

/**
 * [LongArray]를 [Multi]로 변환합니다.
 *
 * ## 동작/계약
 * - 배열 요소를 순서대로 방출합니다.
 * - 새 `Multi`를 반환하며 배열 자체는 변경하지 않습니다.
 *
 * ```kotlin
 * val result = longArrayOf(1L, 2L).asMulti().collect().asList().await().indefinitely()
 * // result == [1L, 2L]
 * ```
 */
fun LongArray.asMulti(): Multi<Long> = asIterable().asMulti()

/**
 * [FloatArray]를 [Multi]로 변환합니다.
 *
 * ## 동작/계약
 * - 배열 순서를 유지해 `Float` 값을 방출합니다.
 * - 수신 배열을 mutate 하지 않습니다.
 *
 * ```kotlin
 * val result = floatArrayOf(1f, 2f).asMulti().collect().asList().await().indefinitely()
 * // result == [1.0, 2.0]
 * ```
 */
fun FloatArray.asMulti(): Multi<Float> = asIterable().asMulti()

/**
 * [DoubleArray]를 [Multi]로 변환합니다.
 *
 * ## 동작/계약
 * - 배열 순서대로 `Double` 값을 방출합니다.
 * - 새 `Multi`를 생성하고 원본 배열은 유지됩니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(1.0, 2.0).asMulti().collect().asList().await().indefinitely()
 * // result == [1.0, 2.0]
 * ```
 */
fun DoubleArray.asMulti(): Multi<Double> = asIterable().asMulti()

/**
 * 반복 동작에서 [Uni] 공급 함수를 지연 연결합니다.
 *
 * ## 동작/계약
 * - 재반복 시점마다 [supplier]를 호출해 새 `Uni`를 생성합니다.
 * - 공급 함수 예외는 반복 파이프라인 실패로 전파됩니다.
 *
 * ```kotlin
 * var n = 0
 * val repeat = Multi.createBy().repeating().deferUni { uniOf(++n) }.atMost(2)
 * val result = repeat.collect().asList().await().indefinitely()
 * // result == [1, 2]
 * ```
 */
fun <T> MultiRepetition.deferUni(supplier: () -> Uni<T>): UniRepeat<T> {
    return this.uni(Supplier { supplier() })
}

/**
 * 반복 동작에서 [CompletionStage] 공급 함수를 지연 연결합니다.
 *
 * ## 동작/계약
 * - 재반복마다 [supplier]를 호출해 새 스테이지를 생성합니다.
 * - 스테이지 실패는 반복 파이프라인 실패로 전파됩니다.
 *
 * ```kotlin
 * var n = 0
 * val repeat = Multi.createBy().repeating()
 *     .deferCompletionStage { java.util.concurrent.CompletableFuture.completedFuture(++n) }
 *     .atMost(2)
 * val result = repeat.collect().asList().await().indefinitely()
 * // result == [1, 2]
 * ```
 */
fun <T> MultiRepetition.deferCompletionStage(supplier: () -> CompletionStage<T>): UniRepeat<T> {
    return this.completionStage { supplier() }
}
