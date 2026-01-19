package io.bluetape4k.resilience4j

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay


class SuspendHelloWorldService {

    companion object: KLoggingChannel()

    private val _invocationCount = atomic(0)
    val invocationCount by _invocationCount

    private val sync: Channel<Unit> = Channel(Channel.UNLIMITED)

    suspend fun returnHelloWorld(): String {
        delay(1) // so tests are fast, but compiler agrees suspend modifier is required
        _invocationCount.incrementAndGet()
        return "Hello world"
    }

    suspend fun returnMessage(message: String): String {
        delay(1) // so tests are fast, but compiler agrees suspend modifier is required
        _invocationCount.incrementAndGet()
        return message
    }

    suspend fun throwException() {
        delay(1) // so tests are fast, but compiler agrees suspend modifier is required
        _invocationCount.incrementAndGet()
        error("test exception")
    }

    suspend fun throwExceptionWithMessage(message: String): String {
        delay(1)
        _invocationCount.incrementAndGet()
        error(message)
    }

    /**
     * Suspend until a matching [proceed] call.
     */
    suspend fun await() {
        _invocationCount.incrementAndGet()
        sync.receive()
    }

    /**
     * Allow a call into [await] to proceed.
     */
    fun proceed(): Boolean = sync.trySend(Unit).isSuccess
}
