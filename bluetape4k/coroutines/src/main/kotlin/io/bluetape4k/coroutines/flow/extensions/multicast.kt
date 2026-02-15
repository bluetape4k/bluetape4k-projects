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
 * 하나의 collector 를 upstream source 로 공유하고, 여러 소비자에게 값을 multicasts 합니다
 *
 * ```
 * flowOf(1, 2, 3)
 *    .multicast({ ReplaySubject() })
 *    .collect { println(it) }
 * ```
 *
 * @param subjectSupplier multicasting 을 위한 [SubjectApi] 를 생성하는 함수
 * @param transform multicasting 된 Flow 에 대한 변환 함수
 */
fun <T, R> Flow<T>.multicast(
    subjectSupplier: () -> SubjectApi<T>,
    transform: suspend (Flow<T>) -> Flow<R>,
): Flow<R> =
    multicastInternal(this, subjectSupplier, transform)

/**
 * [SubjectApi]를 경유해 source를 단일 구독으로 공유하고,
 * [transform] 결과를 downstream으로 전달합니다.
 */
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

        // publish
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

        // subject
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

/**
 * [multicastInternal]의 취소 상태를 보관합니다.
 */
private class MulticastState {
    val cancelled = atomic(false)
}
