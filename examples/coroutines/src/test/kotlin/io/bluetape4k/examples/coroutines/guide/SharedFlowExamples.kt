package io.bluetape4k.examples.coroutines.guide

import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeGreaterThan
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

@OptIn(DelicateCoroutinesApi::class)
class SharedFlowExamples {

    companion object: KLogging() {
        private val random = Fakers.random
    }

    class BroadcastEventBus {
        companion object: KLogging()

        /**
         * replay 는 새로운 subscriber에게 재실행 해줄 최근 item 갯수를 뜻합니다.
         *  extraBufferCapacity는 producer의 속도가 빠르고, consumer가 느릴 때 버퍼를 가져갈 수 있도록 합니다.
         * Buffer가 넘칠 경우 BufferOverflow 값에 따라 처리합니다 (대기, 무시 등)
         */
        private val _events = MutableSharedFlow<Event>(0, 16, BufferOverflow.SUSPEND)
        val events: SharedFlow<Event> = _events.asSharedFlow()

        suspend fun postEvent(event: Event) {
            _events.emit(event)
        }
    }

    private lateinit var producerDispatcher: ExecutorCoroutineDispatcher
    private lateinit var consumerDispatcher: ExecutorCoroutineDispatcher

    @BeforeEach
    fun setup() {
        producerDispatcher = newFixedThreadPoolContext(16, "producer")
        consumerDispatcher = newFixedThreadPoolContext(16, "consumer")
    }

    @AfterEach
    fun cleanup() {
        producerDispatcher.close()
        consumerDispatcher.close()
    }

    @Test
    fun `복수개의 Producer로 event 발송과 복수개의 Consumer로 수신 예제`() = runTest {
        val totalProduced = atomic(0L)
        val totalConsumed = atomic(0L)

        val eventBus = BroadcastEventBus()
        val producers = mutableListOf<Job>()
        val consumers = mutableListOf<Job>()

        // 5개의 Producer 가 [Created, Deleted] 를 번갈아가며 발송합니다.
        producers += List(5) { producerId ->
            launch(producerDispatcher) {
                while (isActive) {
                    log.debug { "Producer[$producerId] emit event ... ${Event.Created}" }
                    eventBus.postEvent(Event.Created)
                    totalProduced.incrementAndGet()
                    yield()

                    log.debug { "Producer[$producerId] emit event ... ${Event.Deleted}" }
                    eventBus.postEvent(Event.Deleted)
                    totalProduced.incrementAndGet()
                    yield()
                }
            }.log("P #$producerId")
        }
        yield()

        // 3개의 Consumer가 event를 수신합니다.
        consumers += List(3) { consumerId ->
            launch(consumerDispatcher) {
                eventBus.events
                    .log("consumer")
                    .onEach { totalConsumed.incrementAndGet() }
                    .collect()
            }.log("C #$consumerId")
        }
        yield()

        await atMost Duration.ofSeconds(5) until { totalConsumed.value > 0L }
        delay(10)
        producers.forEach { it.cancelAndJoin() }
        delay(100)
        consumers.forEach { it.cancelAndJoin() }

        log.debug { "produced=${totalProduced.value}, consumed=${totalConsumed.value}" }
        totalProduced.value shouldBeGreaterThan 0L
        totalConsumed.value shouldBeGreaterThan 0L
    }
}
