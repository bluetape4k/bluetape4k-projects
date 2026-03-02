@file:JvmMultifileClass
@file:JvmName("FlowExtensionsKt")

package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.extensions.subject.SubjectApi
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

/**
 * source를 Subject로 fan-out 한 뒤 selector 결과를 반환합니다.
 *
 * ## 동작/계약
 * - `subjectSupplier`가 제공한 Subject에 source 값을 전달하고, `transform(subject)` 결과를 최종 방출합니다.
 * - 결과 Flow가 완료되면 source 전달 코루틴을 취소하기 위해 내부 `cancelled` 플래그를 사용합니다.
 * - source 예외는 `subject.emitError`로 전달되고, selector 쪽 예외는 최종 하류로 전파됩니다.
 * - 내부적으로 source 처리/selector 처리 코루틴을 각각 1개씩 실행합니다.
 *
 * ```kotlin
 * val out = source.multicast({ PublishSubject() }) { shared -> shared.take(1) }
 * // out은 공유 source에서 selector가 선택한 값만 방출
 * ```
 *
 * @param subjectSupplier 공유에 사용할 Subject 생성 함수입니다.
 * @param transform 공유 Flow를 받아 결과 Flow를 만드는 selector입니다.
 */
fun <T, R> Flow<T>.multicast(
    subjectSupplier: () -> SubjectApi<T>,
    transform: suspend (Flow<T>) -> Flow<R>,
): Flow<R> =
    multicastInternal(this, subjectSupplier, transform)

internal fun <T, R> multicastInternal(
    source: Flow<T>,
    subjectSupplier: () -> SubjectApi<T>,
    transform: suspend (Flow<T>) -> Flow<R>,
): Flow<R> = flow {
    coroutineScope {
        val state = MulticastState()
        val subject = subjectSupplier()
        val result = transform(subject)

        val inner = ResumableCollector<R>()

        launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                result.onCompletion { state.cancelled.value = true }
                    .collect {
                        inner.next(it)
                    }
                inner.complete()
            } catch (e: Throwable) {
                inner.error(e)
            }
        }

        launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                source.collect {
                    if (state.cancelled.value) {
                        throw CancellationException()
                    }
                    subject.emit(it)
                    if (state.cancelled.value) {
                        throw CancellationException()
                    }
                }
                subject.complete()
            } catch (e: Throwable) {
                subject.emitError(e)
            }
        }

        inner.drain(this@flow)
    }
}

private class MulticastState {
    val cancelled = atomic(false)
}
