package io.bluetape4k.examples.coroutines.guide

import io.bluetape4k.coroutines.support.log
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.random.Random

class ChannelExamples {

    companion object: KLoggingChannel()

    @Test
    fun `channel-01`() = runTest {
        val channel = Channel<Int>()

        launch {
            for (x in 1..5) {
                delay(Random.nextLong(100))
                log.trace { "Send value=${x * x}" }
                channel.send(x * x)
            }
        }.log("#1")

        yield()

        val received = mutableListOf<Int>()
        repeat(5) {
            val receivedItem = channel.receive()
            received.add(receivedItem)
            log.trace { "received item=$receivedItem" }
        }

        received shouldBeEqualTo listOf(1, 4, 9, 16, 25)
        log.trace { "Done!" }
    }

    @Test
    fun `channel-02`() = runTest {
        val channel = Channel<Int>()

        launch {
            for (x in 1..5) {
                delay(Random.nextLong(100))
                log.trace { "Send value=${x * x}" }
                channel.send(x * x)
            }
            // 접속 종료를 알린다 (reactive의 onCompletion)
            channel.close()
        }

        val received = mutableListOf<Int>()
        for (items in channel) {
            received.add(items)
            log.trace { "received item=$items" }
        }

        received shouldBeEqualTo listOf(1, 4, 9, 16, 25)
        log.trace { "Done!" }
    }

    @Test
    fun `channel-03`() = runTest {
        fun CoroutineScope.produceSquare(): ReceiveChannel<Int> = produce {
            for (x in 1..5) send(x * x)
        }

        val received = mutableListOf<Int>()
        val squares = produceSquare()
        squares.consumeEach {
            received.add(it)
            log.trace { "Received=$it" }
        }

        received shouldBeEqualTo listOf(1, 4, 9, 16, 25)
        log.trace("Done!")
    }
}
