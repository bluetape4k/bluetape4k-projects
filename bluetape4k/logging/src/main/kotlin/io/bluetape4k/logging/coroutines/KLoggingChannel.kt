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
 * Base logger that publishes log events to a background coroutine collector.
 *
 * ## Contract
 * - `send(LogEvent)` publishes to an internal `MutableSharedFlow`.
 * - The default constructor uses a shared IO coroutine scope and one JVM shutdown hook for all instances.
 * - `close()` cancels only this channel's collector job and is idempotent.
 * - `closeAndJoin()` is the suspend shutdown path for tests and lifecycle owners that need deterministic cleanup.
 * - A custom [CoroutineScope] remains owned by the caller; closing the channel does not cancel the injected scope.
 * - Events sent after close are dropped.
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
     * Whether this channel has been explicitly closed.
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
     * Publishes a log event to the internal channel.
     *
     * Events sent after [close] are ignored so callers do not block on a stopped collector.
     *
     * ```kotlin
     * send(LogEvent(Level.INFO, "service started"))
     * ```
     *
     * @param event event to publish.
     */
    suspend fun send(event: LogEvent) {
        if (!isClosed) {
            sharedFlow.emit(event)
        }
    }

    /**
     * Closes this channel and waits until the collector job has stopped.
     *
     * Use this from tests, application shutdown hooks, or lifecycle callbacks when the caller
     * needs deterministic evidence that the collector no longer runs.
     */
    suspend fun closeAndJoin() {
        if (closed.compareAndSet(false, true)) {
            job.cancelAndJoin()
        } else {
            job.join()
        }
    }

    /**
     * Cancels this channel's collector job.
     *
     * This method is idempotent. It does not cancel a custom [CoroutineScope] supplied to the
     * constructor because the caller owns that scope.
     */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            job.cancel()
        }
    }

    /**
     * Publishes a TRACE event when TRACE logging is enabled.
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
     * Publishes a DEBUG event when DEBUG logging is enabled.
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
     * Publishes an INFO event when INFO logging is enabled.
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
     * Publishes a WARN event when WARN logging is enabled.
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
     * Publishes an ERROR event when ERROR logging is enabled.
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
     * Log event passed through the asynchronous channel.
     *
     * ```kotlin
     * val event = LogEvent(Level.INFO, "server started", null)
     * // event.level == Level.INFO
     * // event.msg == "server started"
     * ```
     *
     * @property level log level.
     * @property msg log message.
     * @property error optional error to log with the message.
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
