package io.bluetape4k.examples.coroutines.guide

import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChannelAsFlowExamples {

    companion object: KLoggingChannel()

    class SingleShotEventBus {
        companion object: KLogging()

        private val _events = Channel<Event>(2048, onBufferOverflow = BufferOverflow.SUSPEND)

        val events: Flow<Event> = _events.receiveAsFlow()

        suspend fun postEvent(event: Event) {
            _events.send(event)
            log.trace { "[source] Send event. $event" }
        }
    }

    private lateinit var producerDispatcher: ExecutorCoroutineDispatcher
    private lateinit var consumerDispatcher: ExecutorCoroutineDispatcher

    @BeforeEach
    fun setup() {
        producerDispatcher = newFixedThreadPoolContext(8, "producer")
        consumerDispatcher = newFixedThreadPoolContext(16, "consumer")
    }

    @AfterEach
    fun cleanup() {
        producerDispatcher.close()
        consumerDispatcher.close()
    }

    @Test
    fun `send event to channel receive as flow`() = runTest {

        val eventBus = SingleShotEventBus()
        val jobs = mutableListOf<Job>()
        val jobSize = 5

        val totalProduced = atomic(0L)
        val totalConsumed = atomic(0L)

        jobs += List(jobSize) {
            launch(producerDispatcher) {
                while (isActive) {
                    delay(1)
                    totalProduced.incrementAndGet()
                    eventBus.postEvent(Event.Created)

                }
            }.log("producer1-$it")
        }
        jobs += List(jobSize) {
            launch(producerDispatcher) {
                while (isActive) {
                    delay(1)
                    totalProduced.incrementAndGet()
                    eventBus.postEvent(Event.Deleted)

                }
            }.log("producer2-$it")
        }
        val consumedJobs = List(jobSize * 2) {
            launch(consumerDispatcher) {
                yield()
                eventBus.events
                    .log("consumer")
                    .onEach { totalConsumed.incrementAndGet() }
                    .collect()
            }.log("consumer-$it")
        }
        delay(1000)
        jobs.forEach { it.cancelAndJoin() }

        delay(2000)
        consumedJobs.forEach { it.cancelAndJoin() }

        log.debug { "Produced: ${totalProduced.value}, Consumed: ${totalConsumed.value}" }
        totalProduced.value shouldBeEqualTo totalConsumed.value
    }
}
