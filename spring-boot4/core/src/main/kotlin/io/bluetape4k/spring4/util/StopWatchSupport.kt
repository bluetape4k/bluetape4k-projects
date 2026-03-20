package io.bluetape4k.spring4.util

import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import org.springframework.util.StopWatch

/**
 * 지정한 body를 실행할 때, [StopWatch]를 이용하여 실행시간을 측정합니다.
 *
 * ## 동작/계약
 * - 새 [StopWatch]를 생성해 `start()` 후 [body]를 실행하고, 예외 여부와 무관하게 `finally`에서 `stop()`합니다.
 * - [body]에서 예외가 발생하면 예외를 그대로 전파합니다.
 * - 수신 객체가 없는 top-level 함수이며 호출마다 새 [StopWatch]를 할당합니다.
 *
 * ```kotlin
 * val sw = withStopWatch("coroutines") {
 *     Thread.sleep(100)
 * }
 * // sw.taskCount == 1
 * ```
 *
 * @param id StopWatch의 `id`
 * @param body 실행할 함수
 * @return StopWatch 인스턴스
 */
inline fun withStopWatch(
    id: String = TimebasedUuid.Epoch.nextIdAsString(),
    body: () -> Unit,
): StopWatch =
    StopWatch(id).apply {
        start()
        try {
            body()
        } finally {
            stop()
        }
    }

/**
 * 지정한 body를 실행할 때, [StopWatch]를 이용하여 실행시간을 측정합니다.
 *
 * ## 동작/계약
 * - suspend [body] 실행 전후로 `start()/stop()`을 수행합니다.
 * - [body] 실패 시 예외를 삼키지 않고 그대로 전파합니다.
 * - 호출마다 새 [StopWatch]가 생성됩니다.
 *
 * ```kotlin
 * val sw = withSuspendStopWatch("coroutines") {
 *     delay(100)
 * }
 * // sw.taskCount == 1
 * ```
 *
 * @param id StopWatch의 `id`
 * @param body 실행할 함수
 * @return StopWatch 인스턴스
 */
suspend inline fun withSuspendStopWatch(
    id: String = TimebasedUuid.Epoch.nextIdAsString(),
    body: suspend () -> Unit,
): StopWatch =
    StopWatch(id).apply {
        start()
        try {
            body()
        } finally {
            stop()
        }
    }

/**
 * [StopWatch]를 이용하여 [body]의 실행 시간을 측정합니다.
 *
 * ## 동작/계약
 * - 수신 [StopWatch]가 이미 실행 중이면 `check`에 의해 [IllegalStateException]이 발생합니다.
 * - `start(taskName)` 후 [body]를 실행하고 `finally`에서 `stop()`을 보장합니다.
 * - 수신 객체 상태를 변경해 task 기록을 추가합니다.
 *
 * ```kotlin
 * val stopwatch = StopWatch()
 *
 * val result = stopwatch.task("task1") {
 *      Thread.sleep(100)
 *      "task1"
 * }
 * // result == "task1"
 * ```
 *
 * @receiver StopWatch 인스턴스
 * @param taskName task 이름
 * @param body 실행할 함수
 * @throws IllegalStateException StopWatch가 이미 실행 중인 경우 발생합니다.
 */
inline fun <T> StopWatch.task(
    taskName: String = TimebasedUuid.Epoch.nextIdAsString(),
    body: () -> T,
): T {
    check(!isRunning) { "StopWatch already started, please stop at first." }
    return try {
        start(taskName)
        body()
    } finally {
        stop()
    }
}

/**
 * [StopWatch]를 이용하여 suspend [body]의 실행 시간을 측정합니다.
 *
 * ## 동작/계약
 * - 수신 [StopWatch]가 이미 실행 중이면 `check`에 의해 [IllegalStateException]이 발생합니다.
 * - suspend [body] 실행 전후로 `start(taskName)/stop()`을 수행합니다.
 * - 수신 객체 상태를 변경해 task 정보를 누적합니다.
 *
 * ```kotlin
 * val stopwatch = StopWatch()
 *
 * val result = stopwatch.suspendTask("task1") {
 *      delay(100)
 *      "task1"
 * }
 * // result == "task1"
 * ```
 *
 * @receiver StopWatch 인스턴스
 * @param taskName task 이름
 * @param body 실행할 함수
 * @throws IllegalStateException StopWatch가 이미 실행 중인 경우 발생합니다.
 */
suspend inline fun <T> StopWatch.suspendTask(
    taskName: String = TimebasedUuid.Epoch.nextIdAsString(),
    body: suspend () -> T,
): T {
    check(!isRunning) { "StopWatch already started, please stop at first." }
    return try {
        start(taskName)
        body()
    } finally {
        stop()
    }
}
