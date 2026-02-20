package io.bluetape4k.micrometer.instrument

import io.micrometer.core.instrument.AbstractTimer
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.TimeUnit

/**
 * Suspend 함수의 실행 시간을 측정하는 Timer 확장 함수입니다.
 *
 * ```kotlin
 * val timer = registry.timer("api.call")
 * val result = timer.recordSuspend {
 *     api.fetchData()  // suspend 함수
 * }
 * ```
 *
 * @param T 반환 타입
 * @param block 측정할 suspend 함수
 * @return block의 실행 결과
 */
suspend fun <T> Timer.recordSuspend(block: suspend () -> T): T =
    when (val timer = this) {
        is AbstractTimer -> timer.recordSuspendInternal(block)
        else             -> block()
    }

/**
 * AbstractTimer의 낶部 구현: suspend 함수 실행 시간을 나노초 단위로 측정합니다.
 *
 * @param T 반환 타입
 * @param block 측정할 suspend 함수
 * @return block의 실행 결과
 */
internal suspend inline fun <T> AbstractTimer.recordSuspendInternal(block: suspend () -> T): T {
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
 * val timer = registry.timer("flow.processing")
 * val flow = dataFlow.withTimer(timer)
 *     .collect { data ->
 *         // 처리 로직
 *     }
 * ```
 *
 * @param T Flow의 요소 타입
 * @param timer 측정할 Timer 인스턴스
 * @return 타이머가 적용된 Flow
 */
fun <T> Flow<T>.withTimer(timer: Timer): Flow<T> {
    var start = 0L
    return this
        .onStart { start = System.nanoTime() }
        .onCompletion { error ->
            val end = System.nanoTime()
            timer.record(end - start, TimeUnit.NANOSECONDS)
        }
}
