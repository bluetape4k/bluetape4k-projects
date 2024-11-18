package io.bluetape4k.spring.util

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.KotlinLogging
import org.springframework.util.StopWatch
import java.util.*

private val log by lazy { KotlinLogging.logger { } }

/**
 * 지정한 body를 실행할 때, [StopWatch]를 이용하여 실행시간을 측정합니다.
 *
 * ```kotlin
 * val sw = withStopWatch("coroutines") {
 *     delay(100)
 * }
 * println(sw.prettyPrint())
 * ```
 *
 * @param id StopWatch의 Id 값
 * @param body 실행할 함수
 * @return StopWatch 인스턴스
 */
@JvmOverloads
inline fun withStopWatch(id: String = UUID.randomUUID().encodeBase62(), body: () -> Unit): StopWatch {
    return StopWatch(id).apply {
        start()
        try {
            body()
        } finally {
            stop()
        }
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
 * @param name task 이름
 * @param body 실행할 함수
 */
inline fun <T> StopWatch.task(name: String = UUID.randomUUID().encodeBase62(), body: () -> T): T {
    check(!isRunning) { "StopWatch already started, please stop at first." }
    return try {
        start(name)
        body()
    } finally {
        stop()
    }
}
