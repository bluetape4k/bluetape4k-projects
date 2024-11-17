package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll

/**
 * [Flow] 요소를 소비할 때 예외가 catch하고, 대신 [value]를 emit한 다음 정상적으로 완료합니다.
 *
 * ```
 * flowOf(1, 2, 0)
 *    .map { 10 / it }
 *    .catchAndReturn(-1)
 *    .toList()   // [10, 5, -1]
 * ```
 *
 * @param value 예외가 발생했을 때 대신 emit할 값
 */
fun <T> Flow<T>.catchAndReturn(value: T): Flow<T> =
    catch { emit(value) }

/**
 * [Flow] 요소를 소비할 때 예외를 catch하고, 대신 단일 [errorHandler]가 처리한 값을 emit한 다음 정상적으로 완료합니다.
 *
 * ```
 * flowOf(1, 2, 0)
 *   .map { 10 / it }
 *   .catchAndReturn { e -> if (e is ArithmeticException) -1 else throw e }
 *   .toList()   // [10, 5, -1]
 * ```
 *
 * @param errorHandler 예외를 처리하고 대신 emit할 값을 반환하는 핸들러
 */
fun <T> Flow<T>.catchAndReturn(errorHandler: suspend (cause: Throwable) -> T): Flow<T> =
    catch { emit(errorHandler(it)) }

/**
 * [Flow] 요소를 소비할 때 예외를 catch하고, 대신 [fallback] 플로우의 모든 항목을 emit합니다.
 * 만약 [fallback] 플로우도 예외를 던진다면, 예외는 catch되지 않고 다시 던져집니다.
 *
 * ```
 * flowOf(1, 2, 0)
 *  .map { 10 / it }
 *  .catchAndResume(flowOf(-1))
 *  .toList()   // [10, 5, -1]
 * ```
 *
 * @param fallback 예외가 발생했을 때 대신 emit할 플로우
 */
fun <T> Flow<T>.catchAndResume(fallback: Flow<T>): Flow<T> =
    catch { emitAll(fallback) }

/**
 * 플로우 요소를 소비할 때 예외를 catch하고, 대신 [fallbackHandler] 플로우의 모든 항목을 emit합니다.
 * 만약 [fallbackHandler] 플로우도 예외를 던진다면, 예외는 catch되지 않고 다시 던져집니다.
 *
 * ```
 * flowOf(1, 2, 0)
 *  .map { 10 / it }
 *  .catchAndResume { cause -> flowOf(-1) }
 *  .toList()   // [10, 5, -1]
 * ```
 *
 * @param fallbackHandler 예외를 처리하고 대신 emit할 플로우를 반환하는 핸들러
 */
fun <T> Flow<T>.catchAndResume(fallbackHandler: suspend (cause: Throwable) -> Flow<T>): Flow<T> =
    catch { emitAll(fallbackHandler(it)) }
