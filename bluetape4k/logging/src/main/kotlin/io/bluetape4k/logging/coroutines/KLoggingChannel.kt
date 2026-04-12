package io.bluetape4k.logging.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.WARN_ERROR_PREFIX
import io.bluetape4k.logging.error
import io.bluetape4k.logging.logMessageSafe
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.slf4j.event.Level
import kotlin.concurrent.thread

/**
 * `MutableSharedFlow` 기반 비동기 로깅 채널을 제공하는 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - `send(LogEvent)`로 전달된 이벤트를 백그라운드 코루틴이 순차 소비해 실제 로그로 기록합니다.
 * - 버퍼는 `extraBufferCapacity=64`, `BufferOverflow.SUSPEND` 정책을 사용합니다.
 * - JVM 종료 훅에서 로깅 잡을 취소합니다.
 * - 로그 이벤트 처리 중 예외는 개별적으로 포착되어 Flow 전체가 중단되지 않습니다.
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("logchannel"))

    /**
     * 로그 이벤트를 소비하는 백그라운드 Job입니다.
     * 최초 접근 시 한 번만 thread-safe하게 초기화됩니다.
     */
    private val job: Job by lazy {
        scope.launch {
            sharedFlow
                .onEach { event ->
                    try {
                        when (event.level) {
                            Level.TRACE -> log.trace(event.msg, event.error)
                            Level.DEBUG -> log.debug(event.msg, event.error)
                            Level.INFO  -> log.info(event.msg, event.error)
                            Level.WARN  -> log.warn(WARN_ERROR_PREFIX + event.msg, event.error)
                            Level.ERROR -> log.error(WARN_ERROR_PREFIX + event.msg, event.error)
                        }
                    } catch (e: Throwable) {
                        log.error(e) { "로그 이벤트 처리 중 오류가 발생했습니다." }
                    }
                }
                .catch { error ->
                    log.error(error) { "Error during logging channel." }
                }
                .collect()
        }
    }

    init {
        job // lazy 초기화를 트리거합니다.
        try {
            Runtime.getRuntime().addShutdownHook(
                thread(start = false, isDaemon = true) {
                    job.cancel()
                }
            )
        } catch (_: IllegalStateException) {
            job.cancel()
        }
    }

    /**
     * 로그 이벤트를 내부 채널에 발행합니다.
     *
     * ```kotlin
     * send(LogEvent(Level.INFO, "직접 이벤트 발행"))
     * ```
     *
     * @param event 발행할 로그 이벤트입니다.
     */
    suspend fun send(event: LogEvent) {
        sharedFlow.emit(event)
    }

    /**
     * TRACE 활성화 시 이벤트를 채널에 발행합니다.
     *
     * ```kotlin
     * trace { "TRACE 이벤트" }
     * ```
     */
    suspend inline fun trace(error: Throwable? = null, msg: () -> Any?) {
        if (log.isTraceEnabled) {
            send(LogEvent(Level.TRACE, logMessageSafe(msg = msg), error))
        }
    }

    /**
     * DEBUG 활성화 시 이벤트를 채널에 발행합니다.
     *
     * ```kotlin
     * debug { "DEBUG 이벤트" }
     * ```
     */
    suspend inline fun debug(error: Throwable? = null, msg: () -> Any?) {
        if (log.isDebugEnabled) {
            send(LogEvent(Level.DEBUG, logMessageSafe(msg = msg), error))
        }
    }

    /**
     * INFO 활성화 시 이벤트를 채널에 발행합니다.
     *
     * ```kotlin
     * info { "INFO 이벤트" }
     * ```
     */
    suspend inline fun info(error: Throwable? = null, msg: () -> Any?) {
        if (log.isInfoEnabled) {
            send(LogEvent(Level.INFO, logMessageSafe(msg = msg), error))
        }
    }

    /**
     * WARN 활성화 시 이벤트를 채널에 발행합니다.
     *
     * ```kotlin
     * warn { "WARN 이벤트" }
     * ```
     */
    suspend inline fun warn(error: Throwable? = null, msg: () -> Any?) {
        if (log.isWarnEnabled) {
            send(LogEvent(Level.WARN, logMessageSafe(msg = msg), error))
        }
    }

    /**
     * ERROR 활성화 시 이벤트를 채널에 발행합니다.
     *
     * ```kotlin
     * error(exception) { "ERROR 이벤트" }
     * ```
     */
    suspend inline fun error(error: Throwable? = null, msg: () -> Any?) {
        if (log.isErrorEnabled) {
            send(LogEvent(Level.ERROR, logMessageSafe(msg = msg), error))
        }
    }

    /**
     * 비동기 채널에 전달되는 로그 이벤트 모델입니다.
     *
     * ```kotlin
     * val event = LogEvent(Level.INFO, "서버 시작", null)
     * // event.level == Level.INFO
     * // event.msg == "서버 시작"
     * ```
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
    )
}
