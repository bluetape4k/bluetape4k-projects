package io.bluetape4k.mutiny

import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.converters.UniConverter
import java.time.Duration
import java.util.concurrent.CompletionStage
import java.util.concurrent.Future

/**
 * 완료값이 없는 성공 [Uni]를 생성합니다.
 *
 * ## 동작/계약
 * - 즉시 완료되는 `Uni<Void>`를 반환합니다.
 * - 새 `Uni` 인스턴스를 생성하며 외부 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val uni = voidUni()
 * val result = uni.await().indefinitely()
 * // result == null
 * ```
 */
fun voidUni(): Uni<Void> = Uni.createFrom().voidItem()

/**
 * `null` 값을 방출하는 [Uni]를 생성합니다.
 *
 * ## 동작/계약
 * - 제네릭 타입 [T]에 대해 `null` 아이템을 방출합니다.
 * - 새 `Uni` 인스턴스를 생성하며 실패를 발생시키지 않습니다.
 *
 * ```kotlin
 * val uni = nullUni<String>()
 * val result = uni.await().indefinitely()
 * // result == null
 * ```
 */
fun <T> nullUni(): Uni<T> = Uni.createFrom().nullItem()

/**
 * 고정된 값을 방출하는 [Uni]를 생성합니다.
 *
 * ## 동작/계약
 * - 구독 시마다 같은 [item]을 반환합니다.
 * - 수신 객체를 변경하지 않고 새 `Uni`를 할당합니다.
 *
 * ```kotlin
 * val uni = uniOf(42)
 * val result = uni.await().indefinitely()
 * // result == 42
 * ```
 */
fun <T> uniOf(item: T): Uni<T> = Uni.createFrom().item(item)

/**
 * 공급 함수 결과를 방출하는 [Uni]를 생성합니다.
 *
 * ## 동작/계약
 * - 구독할 때마다 [supplier]가 실행되어 새 값을 계산합니다.
 * - [supplier] 예외는 실패 `Uni`로 전파됩니다.
 *
 * ```kotlin
 * var n = 0
 * val uni = uniOf { ++n }
 * // uni.await().indefinitely() == 1
 * // uni.await().indefinitely() == 2
 * ```
 */
fun <T> uniOf(supplier: () -> T): Uni<T> = Uni.createFrom().item(supplier)

/**
 * 지정한 예외로 실패하는 [Uni]를 생성합니다.
 *
 * ## 동작/계약
 * - 구독 시 항상 같은 [failure]로 실패합니다.
 * - 새 `Uni` 인스턴스를 생성하며 외부 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val uni = uniFailureOf<Int>(IllegalStateException("boom"))
 * // uni.await().indefinitely() -> IllegalStateException
 * ```
 */
fun <T> uniFailureOf(failure: Throwable): Uni<T> = Uni.createFrom().failure(failure)

/**
 * 공급 함수가 반환한 예외로 실패하는 [Uni]를 생성합니다.
 *
 * ## 동작/계약
 * - 구독 시마다 [failureSupplier]가 호출됩니다.
 * - 공급 함수에서 만든 예외가 실패 원인으로 사용됩니다.
 *
 * ```kotlin
 * val uni = uniFailureOf<Int> { IllegalArgumentException("invalid") }
 * // uni.await().indefinitely() -> IllegalArgumentException
 * ```
 */
fun <T> uniFailureOf(failureSupplier: () -> Throwable): Uni<T> = Uni.createFrom().failure(failureSupplier)

/**
 * 상태 값과 매퍼를 이용해 [Uni]를 생성합니다.
 *
 * ## 동작/계약
 * - [state]를 캡처하고 구독 시 [mapper]를 적용한 결과를 방출합니다.
 * - [mapper]에서 예외가 나면 실패 `Uni`로 전파됩니다.
 *
 * ```kotlin
 * val uni = uniOf(10) { "[$it]" }
 * val result = uni.await().indefinitely()
 * // result == "[10]"
 * ```
 */
fun <T, S> uniOf(state: S, mapper: (S) -> T): Uni<T> {
    return Uni.createFrom().item({ state }, mapper)
}

/**
 * 상태 공급 함수와 매퍼를 이용해 [Uni]를 생성합니다.
 *
 * ## 동작/계약
 * - 구독 시마다 [stateSupplier]와 [mapper]가 순서대로 실행됩니다.
 * - 각 구독은 독립 실행되며 새 결과를 계산합니다.
 *
 * ```kotlin
 * var n = 0
 * val uni = uniOf({ ++n }) { it * 2 }
 * // uni.await().indefinitely() == 2
 * // uni.await().indefinitely() == 4
 * ```
 */
fun <T, S> uniOf(stateSupplier: () -> S, mapper: (S) -> T): Uni<T> {
    return Uni.createFrom().item(stateSupplier, mapper)
}

/**
 * 값을 [UniConverter]로 변환해 새 [Uni]를 생성합니다.
 *
 * ## 동작/계약
 * - [converter] 구현이 [item] 변환 방식을 결정합니다.
 * - 변환 과정 예외는 실패 `Uni`로 전파됩니다.
 *
 * ```kotlin
 * val uni = uniConvertOf(10) { source -> uniOf { "[$source]" } }
 * val result = uni.await().indefinitely()
 * // result == "[10]"
 * ```
 */
fun <T, S> uniConvertOf(item: T, converter: UniConverter<T, S>): Uni<S> {
    return Uni.createFrom().converter(converter, item)
}

/**
 * 아이템이 방출될 때마다 콜백을 실행합니다.
 *
 * ## 동작/계약
 * - 원본 아이템은 변경하지 않고 그대로 downstream으로 전달됩니다.
 * - 콜백 예외는 스트림 실패로 전파됩니다.
 *
 * ```kotlin
 * val seen = mutableListOf<Int>()
 * val result = uniOf(7).onEach { seen += it }.await().indefinitely()
 * // seen == [7]
 * // result == 7
 * ```
 */
fun <T> Uni<T>.onEach(callback: (item: T) -> Unit): Uni<T> {
    return onItem().invoke { item: T -> callback(item) }
}

/**
 * [CompletionStage]를 [Uni]로 변환합니다.
 *
 * ## 동작/계약
 * - `CompletionStage`의 성공/실패 신호를 그대로 `Uni`로 브리지합니다.
 * - 기존 스테이지를 변경하지 않고 래핑만 수행합니다.
 *
 * ```kotlin
 * val stage = CompletableFuture.completedFuture(42)
 * val result = stage.asUni().await().indefinitely()
 * // result == 42
 * ```
 */
fun <T> CompletionStage<T>.asUni(): Uni<T> {
    return Uni.createFrom().completionStage(this)
}

/**
 * 공급 함수가 반환한 [CompletionStage]를 [Uni]로 생성합니다.
 *
 * ## 동작/계약
 * - 구독 시마다 [supplier]가 호출됩니다.
 * - 스테이지 완료 결과를 `Uni` 성공/실패로 그대로 전달합니다.
 *
 * ```kotlin
 * val uni = uniCompletionStageOf { CompletableFuture.completedFuture("ok") }
 * val result = uni.await().indefinitely()
 * // result == "ok"
 * ```
 */
fun <T> uniCompletionStageOf(supplier: () -> CompletionStage<T>): Uni<T> {
    return Uni.createFrom().completionStage(supplier)
}

/**
 * 공급 값과 매퍼를 이용해 [CompletionStage] 기반 [Uni]를 생성합니다.
 *
 * ## 동작/계약
 * - [supplier] 값은 캡처되어 [mapper] 입력으로 사용됩니다.
 * - [mapper]가 반환한 스테이지의 완료 결과를 `Uni`로 전달합니다.
 *
 * ```kotlin
 * val uni = uniCompletionStageOf(21) { CompletableFuture.completedFuture(it * 2) }
 * val result = uni.await().indefinitely()
 * // result == 42
 * ```
 */
fun <T, S> uniCompletionStageOf(supplier: T, mapper: (T) -> CompletionStage<S>): Uni<S> {
    return Uni.createFrom().completionStage({ supplier }, mapper)
}

/**
 * [Future]를 지정한 타임아웃으로 기다려 [Uni]로 변환합니다.
 *
 * ## 동작/계약
 * - `Future#get(timeout)` 의미를 따르며 제한 시간을 넘기면 실패합니다.
 * - 원본 `Future`를 변경하지 않고 래핑합니다.
 *
 * ```kotlin
 * val future = CompletableFuture.completedFuture(42)
 * val result = future.asUni(Duration.ofSeconds(1)).await().indefinitely()
 * // result == 42
 * ```
 */
fun <T> Future<T>.asUni(timeout: Duration): Uni<T> {
    return Uni.createFrom().future(this, timeout)
}

/**
 * 공급 함수가 반환한 [Future]를 [Uni]로 생성합니다.
 *
 * ## 동작/계약
 * - 구독 시마다 [supplier]가 호출되고, [timeout] 내 완료를 기다립니다.
 * - 타임아웃 또는 `Future` 실패는 `Uni` 실패로 전파됩니다.
 *
 * ```kotlin
 * val uni = uniFutureOf({ CompletableFuture.completedFuture("done") }, Duration.ofSeconds(1))
 * val result = uni.await().indefinitely()
 * // result == "done"
 * ```
 */
fun <T> uniFutureOf(supplier: () -> java.util.concurrent.Future<T>, timeout: Duration): Uni<T> {
    return Uni.createFrom().future(supplier, timeout)
}
