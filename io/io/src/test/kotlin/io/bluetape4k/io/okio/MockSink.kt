package io.bluetape4k.io.okio

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.unifiedMapOf
import io.bluetape4k.logging.KLogging
import okio.Buffer
import okio.Sink
import okio.Timeout
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain

/**
 * A scriptable sink. Like Mockito, but worse and requiring less configuration.
 */
class MockSink: Sink {

    companion object: KLogging()

    private val logs = fastListOf<String>()
    private val callThrows = unifiedMapOf<Int, Throwable>()

    fun assertLog(vararg messages: String) {
        messages.toFastList() shouldBeEqualTo logs
    }

    fun assertLogContains(message: String) {
        logs shouldContain message
    }

    fun scheduleThrow(call: Int, e: Exception) {
        callThrows[call] = e
    }

    private fun throwIfScheduled() {
        if (logs.isEmpty()) return

        val exception = callThrows[logs.size - 1]
        if (exception != null) throw exception
    }

    override fun write(source: Buffer, byteCount: Long) {
        logs.add("write($source, $byteCount)")
        source.skip(byteCount)
        throwIfScheduled()
    }

    override fun flush() {
        logs.add("flush()")
        throwIfScheduled()
    }

    override fun timeout(): Timeout {
        logs.add("timeout()")
        return Timeout.NONE
    }

    override fun close() {
        logs.add("close()")
        throwIfScheduled()
    }
}
