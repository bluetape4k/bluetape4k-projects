package io.bluetape4k.micrometer.instrument

import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit

/**
 * Suspend 함수의 실행 시간을 측정하는 Timer 확장 함수입니다.
 *
 * ```kotlin
 * val registry = SimpleMeterRegistry()
 * val timer = registry.timer("api.call")
 * val result = timer.recordSuspend {
 *     "response-data"   // suspend 함수 결과
 * }
 * // result == "response-data"
 * // timer.count() == 1L
 * ```
 *
 * @param T 반환 타입
 * @param block 측정할 suspend 함수
 * @return block의 실행 결과
 */
suspend fun <T> Timer.recordSuspend(block: suspend () -> T): T =
    recordSuspendInternal(block)

/**
 * [Timer] 구현체 종류와 관계없이 suspend 함수 실행 시간을 나노초 단위로 측정합니다.
 *
 * @param T 반환 타입
 * @param block 측정할 suspend 함수
 * @return block의 실행 결과
 */
internal suspend inline fun <T> Timer.recordSuspendInternal(block: suspend () -> T): T {
    val start = System.nanoTime()
    return try {
        block()
    } finally {
        val end = System.nanoTime()
        record(end - start, TimeUnit.NANOSECONDS)
    }
}

/**
 * Flow의 실행 시간을 측정하는 확장 함수입니다.
 * Flow가 시작될 때 타이머를 시작하고, 완료될 때 경과 시간을 기록합니다.
 *
 * ```kotlin
 * val registry = SimpleMeterRegistry()
 * val timer = registry.timer("flow.processing")
 * val items = mutableListOf<Int>()
 * flowOf(1, 2, 3).withTimer(timer).collect { items.add(it) }
 * // items == listOf(1, 2, 3)
 * // timer.count() == 1L
 * ```
 *
 * @param T Flow의 요소 타입
 * @param timer 측정할 Timer 인스턴스
 * @return 타이머가 적용된 Flow
 */
fun <T> Flow<T>.withTimer(timer: Timer): Flow<T> =
    flow {
        val start = System.nanoTime()
        try {
            emitAll(this@withTimer)
        } finally {
            timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS)
        }
    }
