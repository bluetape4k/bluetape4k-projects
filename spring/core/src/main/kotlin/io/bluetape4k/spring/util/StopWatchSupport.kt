package io.bluetape4k.spring.util

import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.logging.KotlinLogging
import org.springframework.util.StopWatch

private val log by lazy { KotlinLogging.logger { } }

/**
 * 지정한 body를 실행할 때, [StopWatch]를 이용하여 실행시간을 측정합니다.
 *
 * ```kotlin
 * val sw = withStopWatch("coroutines") {
 *     Thread.sleep(100)
 * }
 * println(sw.prettyPrint())
 * ```
 *
 * @param id StopWatch의 `id`
 * @param body 실행할 함수
 * @return StopWatch 인스턴스
 */
inline fun withStopWatch(
    id: String = TimebasedUuid.Epoch.nextIdAsString(),
    @BuilderInference body: () -> Unit,
): StopWatch = StopWatch(id).apply {
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
 * ```kotlin
 * val sw = withSuspendStopWatch("coroutines") {
 *     delay(100)
 * }
 * println(sw.prettyPrint())
 * ```
 *
 * @param id StopWatch의 `id`
 * @param body 실행할 함수
 * @return StopWatch 인스턴스
 */
suspend inline fun withSuspendStopWatch(
    id: String = TimebasedUuid.Epoch.nextIdAsString(),
    @BuilderInference body: suspend () -> Unit,
): StopWatch = StopWatch(id).apply {
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
 * ```
 * val stopwatch = StopWatch()
 *
 * val result = stopwatch.task("task1") {
 *      Thread.sleep(100)
 *      "task1"
 * }
 *```
 *
 * @receiver StopWatch 인스턴스
 * @param taskName task 이름
 * @param body 실행할 함수
 */
inline fun <T> StopWatch.task(
    taskName: String = TimebasedUuid.Epoch.nextIdAsString(),
    @BuilderInference body: () -> T,
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
 * ```
 * val stopwatch = StopWatch()
 *
 * val result = stopwatch.suspendTask("task1") {
 *      delay(100)
 *      "task1"
 * }
 *```
 *
 * @receiver StopWatch 인스턴스
 * @param taskName task 이름
 * @param body 실행할 함수
 */
suspend inline fun <T> StopWatch.suspendTask(
    taskName: String = TimebasedUuid.Epoch.nextIdAsString(),
    @BuilderInference body: suspend () -> T,
): T {
    check(!isRunning) { "StopWatch already started, please stop at first." }
    return try {
        start(taskName)
        body()
    } finally {
        stop()
    }
}
