package io.bluetape4k.logging.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.logging.logMessageSafe
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.event.Level
import java.io.Serializable
import kotlin.concurrent.thread

/**
 * [MutableSharedFlow]를 버퍼로 이용하여 Coroutine 환경 하에서 로깅을 수행합니다.
 *
 * ```
 * class SomeClass {
 *     companion object: KLoggingChannel()
 *
 *     suspend fun someMethod() {
 *          log.debug { "someMethod" }
 *     }
 * }
 * ```
 */
open class KLoggingChannel: KLogging() {

    private val sharedFlow = MutableSharedFlow<LogEvent>()
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("logchannel"))
    private var job: Job? = null

    init {
        listen()

        Runtime.getRuntime().addShutdownHook(
            thread(start = false, isDaemon = true) {
                job?.let {
                    runBlocking { it.cancelAndJoin() }
                }
            }
        )
    }

    private fun listen() {
        if (job != null) {
            return
        }

        job = scope.launch {
            log.info { "Start logging channel." }

            sharedFlow
                .onEach { event ->
                    when (event.level) {
                        Level.TRACE -> log.trace(event.msg, event.error)
                        Level.DEBUG -> log.debug(event.msg, event.error)
                        Level.INFO  -> log.info(event.msg, event.error)
                        Level.WARN  -> log.warn(event.msg, event.error)
                        Level.ERROR -> log.error(event.msg, event.error)
                    }
                }
                .collect()
        }
    }

    suspend fun send(event: LogEvent) {
        sharedFlow.emit(event)
    }

    suspend inline fun trace(error: Throwable? = null, msg: () -> Any?) {
        if (log.isTraceEnabled) {
            send(LogEvent(Level.TRACE, logMessageSafe(msg), error))
        }
    }

    suspend inline fun debug(error: Throwable? = null, msg: () -> Any?) {
        if (log.isDebugEnabled) {
            send(LogEvent(Level.DEBUG, logMessageSafe(msg), error))
        }
    }

    suspend inline fun info(error: Throwable? = null, msg: () -> Any?) {
        if (log.isInfoEnabled) {
            send(LogEvent(Level.INFO, logMessageSafe(msg), error))
        }
    }

    suspend inline fun warn(error: Throwable? = null, msg: () -> Any?) {
        if (log.isWarnEnabled) {
            send(LogEvent(Level.WARN, logMessageSafe(msg), error))
        }
    }

    suspend inline fun error(error: Throwable? = null, msg: () -> Any?) {
        if (log.isErrorEnabled) {
            send(LogEvent(Level.ERROR, logMessageSafe(msg), error))
        }
    }

    data class LogEvent(
        val level: Level = Level.DEBUG,
        val msg: String? = null,
        val error: Throwable? = null,
    ): Serializable
}
