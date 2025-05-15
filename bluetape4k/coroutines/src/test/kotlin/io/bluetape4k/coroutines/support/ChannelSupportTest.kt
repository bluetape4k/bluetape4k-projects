package io.bluetape4k.coroutines.support

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import kotlin.time.Duration.Companion.milliseconds

class ChannelSupportTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `distinct until changed`() = runTest {
        val channel = produce {
            send(1)
            send(1)
            send(2)
            send(2)
            send(3)
            send(1)
        }
        yield()

        val distinct = channel.distinctUntilChanged()
        distinct.toList() shouldBeEqualTo listOf(1, 2, 3, 1)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `distinct until changed by equal operator`() = runTest {
        val channel = produce {
            send(1.1)
            send(1.2)
            send(2.1)
            send(2.6)
            send(3.1)
            send(1.2)
        }
        yield()

        val distinct = channel.distinctUntilChanged { a, b ->
            a.toInt() == b.toInt()
        }

        distinct.toList() shouldBeEqualTo listOf(1.1, 2.1, 3.1, 1.2)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `recude received element`() = runTest {
        val channel = produce {
            send(1)
            send(2)
            send(3)
        }

        val reduced = channel.reduce { acc, item -> acc + item }
        reduced.receive() shouldBeEqualTo 6
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `recude received element with initial value`() = runTest {
        val channel = produce {
            send(1)
            send(2)
            send(3)
        }

        val reduced = channel.reduce(0) { acc, item -> acc + item }
        reduced.receive() shouldBeEqualTo 6
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `concat channels`() = runTest {
        val channel1 = produce {
            send(1)
            send(2)
            send(3)
        }
        val channel2 = produce {
            send(4)
            send(5)
            send(6)
        }

        concat(channel1, channel2).toList() shouldBeEqualTo listOf(1, 2, 3, 4, 5, 6)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `debounce channel elements`() = runSuspendTest(Dispatchers.Default) {
        val channel = produce {
            send(1)
            delay(50)
            send(2)
            delay(10)
            send(3)
            delay(150)
            send(4)
            delay(10)
            send(5)
        }

        val debounced = channel.debounce(100.milliseconds).toList()
        debounced shouldBeEqualTo listOf(1, 3, 4, 5)
    }
}
