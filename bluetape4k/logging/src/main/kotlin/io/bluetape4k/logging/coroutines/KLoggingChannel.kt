package io.bluetape4k.logging.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.WARN_ERROR_PREFIX
import io.bluetape4k.logging.error
import io.bluetape4k.logging.logMessageSafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.slf4j.event.Level
import java.io.Serializable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * 로그 이벤트를 백그라운드 coroutine collector로 전달하는 기본 logger입니다.
 *
 * ## 계약
 * - `send(LogEvent)`는 내부 `MutableSharedFlow`로 이벤트를 발행합니다.
 * - 기본 생성자는 모든 인스턴스가 공유하는 IO coroutine scope와 하나의 JVM shutdown hook을 사용합니다.
 * - `close()`는 이 channel의 collector job만 취소하며, 여러 번 호출해도 같은 결과를 유지합니다.
 * - `closeAndJoin()`은 테스트나 lifecycle 소유자가 collector 종료를 확정적으로 확인할 때 사용하는 suspend 종료 경로입니다.
 * - 사용자 지정 [CoroutineScope]의 소유권은 호출자에게 남아 있으므로 channel을 닫아도 주입된 scope는 취소하지 않습니다.
 * - 닫힌 뒤 전송된 이벤트는 버립니다.
 *
 * ```kotlin
 * class Service {
 *   companion object : KLoggingChannel()
 * }
 *
 * suspend fun Service.load() {
 *     info { "loading" }
 * }
 * ```
 */
open class KLoggingChannel(
    private val channelScope: CoroutineScope = KLoggingChannelRuntime.scope,
): KLogging(), AutoCloseable {

    private companion object {
        private const val DEFAULT_BUFFER_CAPACITY = 64
    }

    private val sharedFlow = MutableSharedFlow<LogEvent>(
        replay = 0,
        extraBufferCapacity = DEFAULT_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    private val closed = AtomicBoolean(false)

    /**
     * 이 channel이 명시적으로 닫혔는지 여부입니다.
     */
    val isClosed: Boolean get() = closed.get()

    internal val collectorActive: Boolean get() = job.isActive

    private val job: Job by lazy {
        channelScope.launch(start = CoroutineStart.UNDISPATCHED) {
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
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        log.error(e) { "Failed to process a log event." }
                    }
                }
                .catch { error ->
                    if (error is CancellationException) {
                        throw error
                    }
                    log.error(error) { "Error during logging channel." }
                }
                .collect()
        }
    }

    init {
        job
    }

    /**
     * 로그 이벤트를 내부 channel로 발행합니다.
     *
     * [close] 이후 전송된 이벤트는 무시하여 호출자가 중지된 collector에서 대기하지 않도록 합니다.
     *
     * ```kotlin
     * send(LogEvent(Level.INFO, "service started"))
     * ```
     *
     * @param event 발행할 로그 이벤트입니다.
     */
    suspend fun send(event: LogEvent) {
        if (!isClosed) {
            sharedFlow.emit(event)
        }
    }

    /**
     * 이 channel을 닫고 collector job이 중지될 때까지 기다립니다.
     *
     * 테스트, application shutdown hook, lifecycle callback처럼 collector가 더 이상 실행되지 않는다는
     * 확정적 증거가 필요한 호출 지점에서 사용합니다.
     */
    suspend fun closeAndJoin() {
        if (closed.compareAndSet(false, true)) {
            job.cancelAndJoin()
        } else {
            job.join()
        }
    }

    /**
     * 이 channel의 collector job을 취소합니다.
     *
     * 여러 번 호출해도 같은 결과를 유지합니다. 생성자로 전달된 사용자 지정 [CoroutineScope]는 호출자가
     * 소유하므로 이 메서드에서 취소하지 않습니다.
     */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            job.cancel()
        }
    }

    /**
     * TRACE logging이 활성화되어 있을 때 TRACE 이벤트를 발행합니다.
     *
     * ```kotlin
     * trace { "trace event" }
     * ```
     */
    suspend inline fun trace(error: Throwable? = null, msg: () -> Any?) {
        if (log.isTraceEnabled) {
            send(LogEvent(Level.TRACE, logMessageSafe(msg = msg), error))
        }
    }

    /**
     * DEBUG logging이 활성화되어 있을 때 DEBUG 이벤트를 발행합니다.
     *
     * ```kotlin
     * debug { "debug event" }
     * ```
     */
    suspend inline fun debug(error: Throwable? = null, msg: () -> Any?) {
        if (log.isDebugEnabled) {
            send(LogEvent(Level.DEBUG, logMessageSafe(msg = msg), error))
        }
    }

    /**
     * INFO logging이 활성화되어 있을 때 INFO 이벤트를 발행합니다.
     *
     * ```kotlin
     * info { "info event" }
     * ```
     */
    suspend inline fun info(error: Throwable? = null, msg: () -> Any?) {
        if (log.isInfoEnabled) {
            send(LogEvent(Level.INFO, logMessageSafe(msg = msg), error))
        }
    }

    /**
     * WARN logging이 활성화되어 있을 때 WARN 이벤트를 발행합니다.
     *
     * ```kotlin
     * warn { "warn event" }
     * ```
     */
    suspend inline fun warn(error: Throwable? = null, msg: () -> Any?) {
        if (log.isWarnEnabled) {
            send(LogEvent(Level.WARN, logMessageSafe(msg = msg), error))
        }
    }

    /**
     * ERROR logging이 활성화되어 있을 때 ERROR 이벤트를 발행합니다.
     *
     * ```kotlin
     * error(exception) { "error event" }
     * ```
     */
    suspend inline fun error(error: Throwable? = null, msg: () -> Any?) {
        if (log.isErrorEnabled) {
            send(LogEvent(Level.ERROR, logMessageSafe(msg = msg), error))
        }
    }

    /**
     * 비동기 channel을 통해 전달되는 로그 이벤트입니다.
     *
     * ```kotlin
     * val event = LogEvent(Level.INFO, "server started", null)
     * // event.level == Level.INFO
     * // event.msg == "server started"
     * ```
     *
     * @property level 발행할 로그 수준입니다.
     * @property msg 로그에 남길 메시지입니다.
     * @property error 메시지와 함께 기록할 선택적 예외입니다.
     */
    @JvmRecord
    data class LogEvent(
        val level: Level = Level.DEBUG,
        val msg: String? = null,
        val error: Throwable? = null,
    ): Serializable {
        companion object {
            private const val serialVersionUID: Long = -581771429847270896L
        }
    }
}

private object KLoggingChannelRuntime {
    private val job = SupervisorJob()

    val scope: CoroutineScope = CoroutineScope(job + Dispatchers.IO + CoroutineName("logchannel"))

    init {
        try {
            Runtime.getRuntime().addShutdownHook(
                thread(start = false, isDaemon = true, name = "bluetape4k-logchannel-shutdown") {
                    job.cancel(CancellationException("JVM shutdown"))
                }
            )
        } catch (_: IllegalStateException) {
            job.cancel(CancellationException("JVM shutdown already in progress"))
        }
    }
}
