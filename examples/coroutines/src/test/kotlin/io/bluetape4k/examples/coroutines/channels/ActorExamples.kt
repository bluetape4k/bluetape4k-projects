package io.bluetape4k.examples.coroutines.channels

import io.bluetape4k.coroutines.support.log
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ActorExamples {

    companion object: KLogging()

    sealed class CounterMsg
    object IntCounter: CounterMsg()
    class GetCounter(val response: CompletableDeferred<Int>): CounterMsg()

    private fun CoroutineScope.counterActor(): Channel<CounterMsg> {
        val channel = Channel<CounterMsg>()
        launch {
            var counter = 0
            for (msg in channel) {
                when (msg) {
                    is IntCounter -> counter++
                    is GetCounter -> msg.response.complete(counter)
                }
            }
        }.log("receive job")

        channel.invokeOnClose {
            log.debug(it) { "channel close." }
        }
        return channel
    }

    @Test
    fun `actor with channel`() = runTest {
        val counter: SendChannel<CounterMsg> = counterActor()
        val times = 100

        io.bluetape4k.examples.coroutines.massiveRun(Dispatchers.IO, times) {
            counter.send(IntCounter)
        }

        val response = CompletableDeferred<Int>()
        counter.send(GetCounter(response))
        log.debug { "response=${response.await()}" }
        response.getCompleted() shouldBeEqualTo times * times

        counter.close()
    }
}
