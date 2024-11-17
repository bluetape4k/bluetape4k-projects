package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * [Flow]를 제공하는 [flowSupplier]를 비동기로 수행해서 emit 하는 [Flow]를 빌드합니다.
 *
 * ```
 * suspend fun remoteCall1(): R1 = ...
 * suspend fun remoteCall2(r1: R1): R2 = ...
 *
 * fun example1(): Flow<R2> = defer {
 *   val r1 = remoteCall1()
 *   val r2 = remoteCall2(r1)
 *   flowOf(r2)
 * }
 *
 * fun example2(): Flow<R1> = defer { flowOf(remoteCall1()) }
 * ```
 *
 * @param flowSupplier [Flow]를 제공하는 supplier
 */
inline fun <T> defer(
    crossinline flowSupplier: suspend () -> Flow<T>,
): Flow<T> = flow {
    emitAll(flowSupplier())
}
