package io.bluetape4k.examples.coroutines.flow

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowBasicExamples {

    companion object: KLogging()

    private val sequencer = atomic(0L)

    private suspend fun computeNextValue(): Long {
        delay(10)
        return sequencer.incrementAndGet()
    }

    @BeforeEach
    fun setup() {
        sequencer.value = 0
    }

    /**
     * Channel 은 Hot Channel (라디오, TV) 로서 수신에 상관없이 전송해 버립니다.
     */
    @Test
    fun `hot channel vs cold flow - channel`() = runTest {
        val channel: ReceiveChannel<Long> = produce(capacity = Channel.UNLIMITED) {
            repeat(10) {
                val x = computeNextValue()
                log.debug { "send $x" }
                send(x)
            }
        }
        delay(100)
        for (elem in channel) {
            log.debug { "receive=$elem" }
        }
    }

    /**
     * Flow 는 Cold Channel (VOD, OTT) 로서 수신을 요청해야 전송을 수행합니다.
     */
    @Test
    fun `hot channel vs cold flow - flow`() = runTest {
        val flow = flow {
            repeat(10) {
                val x = computeNextValue()
                log.debug { "emit $x" }
                emit(x)
            }
        }
        delay(100) // 의미없다
        flow.collect { elem ->
            log.debug { "collect=$elem" }
        }
    }

    private fun makeFlow() = flow {
        log.debug { "Flow started" }
        for (i in 1..3) {
            delay(1000)
            emit(i)
        }
    }

    /**
     * `flow {}`는 builder 로서 실행할 때마다 실행됩니다.
     *
     */
    @Test
    fun `flow {} is statement 이므로 실행 시마다 flow가 생성된다 `() = runTest {
        val flow = makeFlow()

        delay(1000)
        log.debug { "Collect flow ... " }
        flow.collect { value ->
            log.debug { "collect $value" }
        }

        delay(1000)
        log.debug { "Collect again ..." }
        flow.collect { value ->
            log.debug { "consume $value" }
        }
    }

    /**
     * collect 시에 각자의 flow 를 통해 요소를 받게됩니다.
     */
    @Test
    fun `flow {} 는 빌더이므로 collect 시마다 새로 flow를 생성합니다`() = runTest {
        coroutineScope {
            val flow = makeFlow()
            launch {
                flow.collect { value ->
                    log.debug { "collect $value" }
                }
            }
            launch {
                flow.collect { value ->
                    log.debug { "consume $value" }
                }
            }
        }
    }
}
