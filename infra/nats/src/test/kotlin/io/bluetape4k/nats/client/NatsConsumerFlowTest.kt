package io.bluetape4k.nats.client

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.nats.client.ConsumeOptions
import io.nats.client.ConsumerContext
import io.nats.client.IterableConsumer
import io.nats.client.JetStream
import io.nats.client.JetStreamSubscription
import io.nats.client.Message
import io.nats.client.PushSubscribeOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class NatsConsumerFlowTest {

    @Test
    fun `pull flow is cold preserves order closes consumer and does not ack`() = runTest {
        val context = mockk<ConsumerContext>()
        val consumer = mockk<IterableConsumer>()
        val first = mockk<Message>()
        val second = mockk<Message>()
        every { context.iterate(any<ConsumeOptions>()) } returns consumer
        every { consumer.nextMessage(any<Duration>()) } returnsMany listOf(first, second)
        every { consumer.close() } just runs

        val received = context
            .consumeAsFlow(capacity = 2)
            .take(2)
            .toList()

        received shouldBeEqualTo listOf(first, second)
        verify(exactly = 1) { context.iterate(any<ConsumeOptions>()) }
        verify(exactly = 1) { consumer.close() }
        verify(exactly = 0) { first.ack() }
        verify(exactly = 0) { second.ack() }
    }

    @Test
    fun `pull flow normalizes batch size to capacity plus receiver`() = runTest {
        val context = mockk<ConsumerContext>()
        val consumer = mockk<IterableConsumer>()
        val options = ConsumeOptions.builder().batchSize(500).build()
        val capturedOptions = slot<ConsumeOptions>()
        every { context.iterate(capture(capturedOptions)) } returns consumer
        every { consumer.nextMessage(any<Duration>()) } returns null
        every { consumer.isStopped } returns true
        every { consumer.isFinished } returns false
        every { consumer.close() } just runs

        context.consumeAsFlow(options, capacity = 2).toList()

        capturedOptions.captured.batchSize shouldBeEqualTo 3
    }

    @Test
    fun `push flow is cold preserves order unsubscribes and does not ack`() = runTest {
        val jetStream = mockk<JetStream>()
        val subscription = mockk<JetStreamSubscription>()
        val first = mockk<Message>()
        val second = mockk<Message>()
        val options = pushSubscriptionOptions {
            pendingMessageLimit(8)
            pendingByteLimit(1024)
        }
        every { jetStream.subscribe("events", options) } returns subscription
        every { subscription.pendingMessageLimit } returns 8
        every { subscription.pendingByteLimit } returns 1024
        every { subscription.droppedCount } returns 0
        every { subscription.nextMessage(any<Duration>()) } returnsMany listOf(first, second)
        every { subscription.unsubscribe() } just runs

        val received = jetStream
            .consumeAsFlow("events", options, capacity = 2)
            .take(2)
            .toList()

        received shouldBeEqualTo listOf(first, second)
        verify(exactly = 1) { jetStream.subscribe("events", options) }
        verify(exactly = 1) { subscription.unsubscribe() }
        verify(exactly = 0) { first.ack() }
        verify(exactly = 0) { second.ack() }
    }

    @Test
    fun `same flow instance rejects concurrent collectors before creating a handle`() = runBlocking {
        val context = mockk<ConsumerContext>()
        val consumer = mockk<IterableConsumer>()
        val receiveStarted = CountDownLatch(1)
        val receiveBarrier = CountDownLatch(1)
        val receiveInterrupted = CountDownLatch(1)
        every { context.iterate(any<ConsumeOptions>()) } returns consumer
        every { consumer.nextMessage(any<Duration>()) } answers {
            receiveStarted.countDown()
            try {
                check(receiveBarrier.await(5, TimeUnit.SECONDS)) {
                    "receive was not interrupted before the bounded barrier expired"
                }
            } catch (_: InterruptedException) {
                receiveInterrupted.countDown()
                Thread.currentThread().interrupt()
            }
            null
        }
        every { consumer.close() } just runs

        val flow = context.consumeAsFlow(capacity = 1)
        val first = async(Dispatchers.Default) { flow.collect() }
        check(receiveStarted.await(5, TimeUnit.SECONDS))

        assertFailsWith<IllegalStateException> {
            flow.take(1).toList()
        }

        first.cancel()
        withTimeout(5.seconds) {
            first.join()
        }
        check(receiveInterrupted.await(1, TimeUnit.SECONDS))
        verify(exactly = 1) { context.iterate(any<ConsumeOptions>()) }
        verify(exactly = 1) { consumer.close() }
    }

    @Test
    fun `push drop delta fails flow and reports dropped count`() = runTest {
        val jetStream = mockk<JetStream>()
        val subscription = mockk<JetStreamSubscription>()
        val options = pushSubscriptionOptions {
            pendingMessageLimit(8)
            pendingByteLimit(1024)
        }
        every { jetStream.subscribe("events", options) } returns subscription
        every { subscription.pendingMessageLimit } returns 8
        every { subscription.pendingByteLimit } returns 1024
        every { subscription.droppedCount } returnsMany listOf(0, 0, 2, 2)
        every { subscription.nextMessage(any<Duration>()) } returns null
        every { subscription.isActive } returns true
        every { subscription.unsubscribe() } just runs

        val failure = assertFailsWith<NatsConsumerFlowException> {
            jetStream.consumeAsFlow("events", options, capacity = 1).toList()
        }

        failure.droppedMessages shouldBeEqualTo 2L
        verify(exactly = 1) { subscription.unsubscribe() }
    }

    @Test
    fun `invalid flow and push limits fail before handle creation`() {
        val jetStream = mockk<JetStream>(relaxed = true)
        val context = mockk<ConsumerContext>(relaxed = true)
        val oversizedPush = pushSubscriptionOptions {
            pendingMessageLimit(65_537)
            pendingByteLimit(1024)
        }
        val oversizedBytesPush = pushSubscriptionOptions {
            pendingMessageLimit(1)
            pendingByteLimit(64L * 1024 * 1024 + 1)
        }
        val invalidPull = ConsumeOptions.builder().batchBytes(1).build()

        assertFailsWith<IllegalArgumentException> {
            jetStream.consumeAsFlow("events", capacity = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            context.consumeAsFlow(capacity = 1_025)
        }
        assertFailsWith<IllegalArgumentException> {
            context.consumeAsFlow(capacity = 1, receiveTimeout = 99.milliseconds)
        }
        assertFailsWith<IllegalArgumentException> {
            jetStream.consumeAsFlow("events", oversizedPush)
        }
        assertFailsWith<IllegalArgumentException> {
            jetStream.consumeAsFlow("events", oversizedBytesPush)
        }
        assertFailsWith<IllegalArgumentException> {
            context.consumeAsFlow(invalidPull)
        }

        jetStream.consumeAsFlow(
            "events",
            pushSubscriptionOptions {
                pendingMessageLimit(65_536)
                pendingByteLimit(64L * 1024 * 1024)
            },
            capacity = 1_024,
            receiveTimeout = 100.milliseconds,
        )
        context.consumeAsFlow(capacity = 1_024, receiveTimeout = 100.milliseconds)

        verify(exactly = 0) { jetStream.subscribe(any(), any<PushSubscribeOptions>()) }
        verify(exactly = 0) { context.iterate(any<ConsumeOptions>()) }
    }

    @Test
    fun `ordinary receive failure stays primary and cleanup failure is suppressed`() = runTest {
        val jetStream = mockk<JetStream>()
        val subscription = mockk<JetStreamSubscription>()
        val options = pushSubscriptionOptions {
            pendingMessageLimit(8)
            pendingByteLimit(1024)
        }
        val receiveFailure = IllegalStateException("receive failed")
        val cleanupFailure = IllegalArgumentException("cleanup failed")
        every { jetStream.subscribe("events", options) } returns subscription
        every { subscription.pendingMessageLimit } returns 8
        every { subscription.pendingByteLimit } returns 1024
        every { subscription.droppedCount } returns 0
        every { subscription.nextMessage(any<Duration>()) } throws receiveFailure
        every { subscription.unsubscribe() } throws cleanupFailure

        val failure = assertFailsWith<IllegalStateException> {
            jetStream.consumeAsFlow("events", options).toList()
        }

        failure.message shouldBeEqualTo receiveFailure.message
        verify(exactly = 1) { subscription.unsubscribe() }
        receiveFailure.suppressed.map { it.message } shouldBeEqualTo listOf(cleanupFailure.message)
        Unit
    }

    @Test
    fun `pending limit readback failure preserves its cause`() = runTest {
        val jetStream = mockk<JetStream>()
        val subscription = mockk<JetStreamSubscription>()
        val options = pushSubscriptionOptions {
            pendingMessageLimit(8)
            pendingByteLimit(1024)
        }
        val readbackFailure = IllegalStateException("pending limit unavailable")
        every { jetStream.subscribe("events", options) } returns subscription
        every { subscription.pendingMessageLimit } throws readbackFailure
        every { subscription.droppedCount } returns 0
        every { subscription.unsubscribe() } just runs

        val failure = assertFailsWith<NatsConsumerFlowException> {
            jetStream.consumeAsFlow("events", options).toList()
        }

        failure.droppedMessages shouldBeEqualTo 0L
        (failure.cause === readbackFailure).shouldBeTrue()
        verify(exactly = 1) { subscription.unsubscribe() }
    }

    @Test
    fun `null receives continue while active and stop when handle finishes`() = runTest {
        val context = mockk<ConsumerContext>()
        val consumer = mockk<IterableConsumer>()
        val message = mockk<Message>()
        every { context.iterate(any<ConsumeOptions>()) } returns consumer
        every { consumer.nextMessage(any<Duration>()) } returnsMany listOf(null, message, null)
        every { consumer.isStopped } returnsMany listOf(false, false, true)
        every { consumer.isFinished } returns false
        every { consumer.close() } just runs

        val received = context.consumeAsFlow(capacity = 1).toList()

        received shouldBeEqualTo listOf(message)
        verify(exactly = 1) { consumer.close() }
    }

    @Test
    fun `default push limits are finite and bounded`() {
        (defaultNatsFlowPushOptions.pendingMessageLimit <= 1_024L).shouldBeTrue()
        (defaultNatsFlowPushOptions.pendingByteLimit <= 16L * 1024 * 1024).shouldBeTrue()
    }
}
