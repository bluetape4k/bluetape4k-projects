package io.bluetape4k.mutiny

import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.converters.UniConverter
import java.time.Duration
import java.util.concurrent.CompletionStage
import java.util.concurrent.Future

fun voidUni(): Uni<Void> = Uni.createFrom().voidItem()
fun <T> nullUni(): Uni<T> = Uni.createFrom().nullItem()

fun <T> uniOf(item: T): Uni<T> = Uni.createFrom().item(item)
fun <T> uniOf(supplier: () -> T): Uni<T> = Uni.createFrom().item(supplier)

fun <T> uniFailureOf(failure: Throwable): Uni<T> = Uni.createFrom().failure(failure)
fun <T> uniFailureOf(failureSupplier: () -> Throwable): Uni<T> = Uni.createFrom().failure(failureSupplier)

fun <T, S> uniOf(state: S, mapper: (S) -> T): Uni<T> {
    return Uni.createFrom().item({ state }, mapper)
}

fun <T, S> uniOf(stateSupplier: () -> S, mapper: (S) -> T): Uni<T> {
    return Uni.createFrom().item(stateSupplier, mapper)
}

fun <T, S> uniConvertOf(item: T, converter: UniConverter<T, S>): Uni<S> {
    return Uni.createFrom().converter(converter, item)
}

fun <T> Uni<T>.onEach(callback: (item: T) -> Unit): Uni<T> {
    return onItem().invoke { item: T -> callback(item) }
}

fun <T> CompletionStage<T>.asUni(): Uni<T> {
    return Uni.createFrom().completionStage(this)
}

fun <T> uniCompletionStageOf(supplier: () -> CompletionStage<T>): Uni<T> {
    return Uni.createFrom().completionStage(supplier)
}

fun <T, S> uniCompletionStageOf(supplier: T, mapper: (T) -> CompletionStage<S>): Uni<S> {
    return Uni.createFrom().completionStage({ supplier }, mapper)
}

fun <T> Future<T>.asUni(timeout: Duration): Uni<T> {
    return Uni.createFrom().future(this, timeout)
}

fun <T> uniFutureOf(supplier: () -> java.util.concurrent.Future<T>, timeout: Duration): Uni<T> {
    return Uni.createFrom().future(supplier, timeout)
}
