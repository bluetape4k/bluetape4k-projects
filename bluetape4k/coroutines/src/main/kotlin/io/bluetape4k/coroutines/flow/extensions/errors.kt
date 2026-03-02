package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll

/**
 * upstream 예외 발생 시 단일 기본값을 방출하고 종료합니다.
 *
 * ## 동작/계약
 * - 정상 요소는 그대로 통과하고 예외 시 `value` 1개를 추가 방출합니다.
 * - fallback 방출 후 스트림은 완료됩니다.
 * - `catch` 규칙에 따라 취소 예외는 보통 재전파됩니다.
 *
 * ```kotlin
 * val result = flow<Int> { error("boom") }.catchAndReturn(-1).toList()
 * // result == [-1]
 * ```
 *
 * @param value 예외 시 대신 방출할 값입니다.
 */
fun <T> Flow<T>.catchAndReturn(value: T): Flow<T> =
    catch { emit(value) }

/**
 * upstream 예외 발생 시 핸들러로 fallback 값을 계산해 방출합니다.
 *
 * ## 동작/계약
 * - 예외를 `errorHandler`에 전달하고 반환값을 1개 방출합니다.
 * - 핸들러 내부 예외는 다시 하류로 전파됩니다.
 *
 * ```kotlin
 * val result = flow<Int> { error("boom") }
 *   .catchAndReturn { e -> e.message?.length ?: 0 }
 *   .toList()
 * // result == [4]
 * ```
 *
 * @param errorHandler 예외를 받아 대체 값을 계산하는 함수입니다.
 */
fun <T> Flow<T>.catchAndReturn(errorHandler: suspend (cause: Throwable) -> T): Flow<T> =
    catch { emit(errorHandler(it)) }

/**
 * upstream 예외 발생 시 대체 Flow로 전환합니다.
 *
 * ## 동작/계약
 * - 예외가 나기 전까지의 요소는 유지되고, 예외 시점부터 `fallback`을 이어서 방출합니다.
 * - 내부적으로 `emitAll(fallback)`을 사용합니다.
 *
 * ```kotlin
 * val result = flow<Int> { emit(1); error("boom") }
 *   .catchAndResume(flowOf(9, 10)).toList()
 * // result == [1, 9, 10]
 * ```
 *
 * @param fallback 예외 시 이어서 방출할 대체 Flow입니다.
 */
fun <T> Flow<T>.catchAndResume(fallback: Flow<T>): Flow<T> =
    catch { emitAll(fallback) }

/**
 * upstream 예외 발생 시 핸들러가 반환한 대체 Flow로 전환합니다.
 *
 * ## 동작/계약
 * - 예외를 `fallbackHandler`에 전달해 동적으로 fallback Flow를 선택합니다.
 * - 핸들러 또는 fallback 내부 예외는 하류로 전파됩니다.
 *
 * ```kotlin
 * val result = flow<Int> { error("boom") }
 *   .catchAndResume { flowOf(it.message?.length ?: 0) }
 *   .toList()
 * // result == [4]
 * ```
 *
 * @param fallbackHandler 예외를 받아 대체 Flow를 반환하는 함수입니다.
 */
fun <T> Flow<T>.catchAndResume(fallbackHandler: suspend (cause: Throwable) -> Flow<T>): Flow<T> =
    catch { emitAll(fallbackHandler(it)) }
