package io.bluetape4k.examples.coroutines.channels

import io.bluetape4k.coroutines.support.log
import io.bluetape4k.examples.coroutines.massiveRun
import io.bluetape4k.logging.coroutines.KLoggingChannel
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

/**
 * Actor 패턴을 [Channel]로 구현하는 예제입니다.
 *
 * Actor 패턴은 상태를 하나의 코루틴 내부에 격리하고,
 * 메시지(sealed class)를 Channel로 주고받아 동시성을 안전하게 처리합니다.
 */
class ActorExamples {

    companion object: KLoggingChannel()

    /** Actor가 수신하는 메시지의 sealed 타입 */
    sealed class CounterMsg

    /** 카운터를 1 증가시키는 메시지 */
    data object IntCounter: CounterMsg()

    /** 현재 카운터 값을 요청하는 메시지. [response]에 결과를 전달합니다. */
    data class GetCounter(val response: CompletableDeferred<Int>): CounterMsg()

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
        }.log("👋receive job")

        channel.invokeOnClose {
            log.debug(it) { "✅ channel close." }
        }
        return channel
    }

    @Test
    fun `actor with channel`() = runTest {
        val counter: SendChannel<CounterMsg> = counterActor()
        val times = 100

        massiveRun(Dispatchers.IO, times) {
            counter.send(IntCounter)
        }

        val response = CompletableDeferred<Int>()
        counter.send(GetCounter(response))
        val result = response.await()
        log.debug { "result=$result" }
        response.getCompleted() shouldBeEqualTo times * times

        counter.close()
    }
}
