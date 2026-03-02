package io.bluetape4k.logging.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.logMessageSafe
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.event.Level
import java.io.Serializable
import kotlin.concurrent.thread

/**
 * `MutableSharedFlow` 기반 비동기 로깅 채널을 제공하는 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - `send(LogEvent)`로 전달된 이벤트를 백그라운드 코루틴이 순차 소비해 실제 로그로 기록합니다.
 * - 버퍼는 `extraBufferCapacity=64`, `BufferOverflow.SUSPEND` 정책을 사용합니다.
 * - JVM 종료 훅에서 로깅 잡의 자식 코루틴 취소를 시도합니다.
 * - 로그 소비 중 예외는 내부 catch에서 에러 로그로 남깁니다.
 *
 * ```kotlin
 * class Service {
 *   companion object : KLoggingChannel()
 * }
 * // suspend fun 안에서 trace/debug/... 호출
 * ```
 */
open class KLoggingChannel: KLogging() {

    private val sharedFlow = MutableSharedFlow<LogEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("logchannel"))
    private var job: Job? = null

    init {
        listen()

        Runtime.getRuntime().addShutdownHook(
            thread(start = false, isDaemon = true) {
                runBlocking {
                    job?.cancelChildren()
                }
            }
        )
    }

    private fun listen() {
        if (job != null) {
            return
        }

        job = scope.launch {
            sharedFlow
                .onEach { event ->
                    when (event.level) {
                        Level.TRACE -> log.trace(event.msg, event.error)
                        Level.DEBUG -> log.debug(event.msg, event.error)
                        Level.INFO -> log.info(event.msg, event.error)
                        Level.WARN -> log.warn("🔥" + event.msg, event.error)
                        Level.ERROR -> log.error("🔥" + event.msg, event.error)
                    }
                }
                .catch { error ->
                    log.error(error) { "🔥Error during logging channel." }
                }
                .collect()
        }
    }

    /**
     * 로그 이벤트를 내부 채널에 발행합니다.
     *
     * @param event 발행할 로그 이벤트입니다.
     */
    suspend fun send(event: LogEvent) {
        sharedFlow.emit(event)
    }

    /** TRACE 활성화 시 이벤트를 채널에 발행합니다. */
    suspend inline fun trace(error: Throwable? = null, msg: () -> Any?) {
        if (log.isTraceEnabled) {
            send(LogEvent(Level.TRACE, logMessageSafe(msg = msg), error))
        }
    }

    /** DEBUG 활성화 시 이벤트를 채널에 발행합니다. */
    suspend inline fun debug(error: Throwable? = null, msg: () -> Any?) {
        if (log.isDebugEnabled) {
            send(LogEvent(Level.DEBUG, logMessageSafe(msg = msg), error))
        }
    }

    /** INFO 활성화 시 이벤트를 채널에 발행합니다. */
    suspend inline fun info(error: Throwable? = null, msg: () -> Any?) {
        if (log.isInfoEnabled) {
            send(LogEvent(Level.INFO, logMessageSafe(msg = msg), error))
        }
    }

    /** WARN 활성화 시 이벤트를 채널에 발행합니다. */
    suspend inline fun warn(error: Throwable? = null, msg: () -> Any?) {
        if (log.isWarnEnabled) {
            send(LogEvent(Level.WARN, logMessageSafe(msg = msg), error))
        }
    }

    /** ERROR 활성화 시 이벤트를 채널에 발행합니다. */
    suspend inline fun error(error: Throwable? = null, msg: () -> Any?) {
        if (log.isErrorEnabled) {
            send(LogEvent(Level.ERROR, logMessageSafe(msg = msg), error))
        }
    }

    /**
     * 비동기 채널에 전달되는 로그 이벤트 모델입니다.
     *
     * @property level 로그 레벨입니다.
     * @property msg 로그 메시지입니다.
     * @property error 함께 기록할 예외입니다.
     */
    @JvmRecord
    data class LogEvent(
        val level: Level = Level.DEBUG,
        val msg: String? = null,
        val error: Throwable? = null,
    ): Serializable
}
