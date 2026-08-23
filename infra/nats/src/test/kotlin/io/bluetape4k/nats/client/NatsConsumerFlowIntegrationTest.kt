package io.bluetape4k.nats.client

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.api.consumerConfiguration
import io.bluetape4k.support.toUtf8String
import io.nats.client.api.AckPolicy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class NatsConsumerFlowIntegrationTest : AbstractNatsTest() {

    @Test
    fun `pull flow preserves order and caller acknowledges messages`() = runTest(timeout = 30.seconds) {
        val stream = "consumer-flow-pull"
        val subject = "consumer-flow.pull"
        val received = mutableListOf<String>()

        getConnection().use { connection ->
            connection.createOrReplaceStream(stream, subject)
            val jetStream = connection.jetStream()
            repeat(3) { index ->
                jetStream.publish(subject, "pull-$index")
            }

            val consumer = consumerContextOf(
                connection,
                stream,
                consumerConfiguration {
                    durable("consumer-flow-pull-durable")
                    ackPolicy(AckPolicy.Explicit)
                },
            )

            consumer
                .consumeAsFlow(capacity = 2)
                .take(3)
                .collect { message ->
                    received += message.data.toUtf8String()
                    message.ack()
                }
        }

        received shouldBeEqualTo listOf("pull-0", "pull-1", "pull-2")
    }

    @Test
    fun `push flow preserves order and caller acknowledges messages`() = runTest(timeout = 30.seconds) {
        val stream = "consumer-flow-push"
        val subject = "consumer-flow.push"
        val received = mutableListOf<String>()

        getConnection().use { connection ->
            connection.createOrReplaceStream(stream, subject)
            val jetStream = connection.jetStream()
            repeat(3) { index ->
                jetStream.publish(subject, "push-$index")
            }

            val pushOptions = pushSubscriptionOptions {
                stream(stream)
                pendingMessageLimit(1_024)
                pendingByteLimit(16L * 1024 * 1024)
            }
            jetStream
                .consumeAsFlow(subject, pushOptions, capacity = 2)
                .take(3)
                .collect { message ->
                    received += message.data.toUtf8String()
                    message.ack()
                }
        }

        received shouldBeEqualTo listOf("push-0", "push-1", "push-2")
    }

    @Test
    fun `push flow reports actual pending queue drops`() = runBlocking {
        val stream = "consumer-flow-push-drop"
        val subject = "consumer-flow.push-drop"

        getConnection().use { connection ->
            connection.createOrReplaceStream(stream, subject)
            val jetStream = connection.jetStream()
            val pushOptions = pushSubscriptionOptions {
                stream(stream)
                pendingMessageLimit(1)
                pendingByteLimit(64L * 1024)
            }
            supervisorScope {
                val firstDelivered = CompletableDeferred<Unit>()
                val flow = jetStream.consumeAsFlow(
                    subject,
                    pushOptions,
                    capacity = 1,
                    receiveTimeout = 500.milliseconds,
                )
                val collector = async(Dispatchers.Default) {
                    flow.collect {
                        if (firstDelivered.complete(Unit)) {
                            delay(1.seconds)
                        }
                    }
                }

                jetStream.publish(subject, "seed")
                withTimeout(5.seconds) {
                    firstDelivered.await()
                }
                repeat(500) { index ->
                    jetStream.publish(subject, "drop-$index")
                }

                val failure = try {
                    collector.await()
                    null
                } catch (exception: NatsConsumerFlowException) {
                    exception
                }

                check(failure != null) { "실제 pending queue drop이 Flow 예외로 보고되지 않았습니다." }
                check(failure.droppedMessages > 0) {
                    "실제 pending queue drop 수가 양수가 아닙니다: ${failure.droppedMessages}"
                }
            }
        }
    }

    @Test
    fun `unacknowledged pull message is redelivered and sequential collection reuses the flow`() =
        runBlocking {
            val stream = "consumer-flow-redelivery"
            val subject = "consumer-flow.redelivery"
            val deliveries = mutableListOf<String>()

            getConnection().use { connection ->
                connection.createOrReplaceStream(stream, subject)
                val jetStream = connection.jetStream()
                jetStream.publish(subject, "redeliver-me")

                val consumer = consumerContextOf(
                    connection,
                    stream,
                    consumerConfiguration {
                        durable("consumer-flow-redelivery-durable")
                        ackPolicy(AckPolicy.Explicit)
                        ackWait(Duration.ofMillis(250))
                        maxDeliver(2)
                        maxAckPending(1)
                    },
                )
                val flow = consumer.consumeAsFlow(capacity = 1, receiveTimeout = 500.milliseconds)

                flow.take(1).collect { message ->
                    deliveries += message.data.toUtf8String()
                    // Deliberately leave the first delivery unacknowledged.
                }

                withTimeout(5.seconds) {
                    flow.take(1).collect { message ->
                        deliveries += message.data.toUtf8String()
                        message.ack()
                    }
                }
            }

            deliveries shouldBeEqualTo listOf("redeliver-me", "redeliver-me")
            Unit
        }

    @Test
    fun `caller controls nak redelivery and term finalization`() = runBlocking {
        getConnection().use { connection ->
            val nakStream = "consumer-flow-manual-nak"
            val nakSubject = "consumer-flow.manual-nak"
            connection.createOrReplaceStream(nakStream, nakSubject)
            val jetStream = connection.jetStream()
            jetStream.publish(nakSubject, "nak-me")

            val nakConsumer = consumerContextOf(
                connection,
                nakStream,
                consumerConfiguration {
                    durable("consumer-flow-manual-nak-durable")
                    ackPolicy(AckPolicy.Explicit)
                    ackWait(Duration.ofMillis(500))
                    maxDeliver(2)
                    maxAckPending(1)
                },
            )
            val nakDeliveries = mutableListOf<String>()
            withTimeout(5.seconds) {
                nakConsumer.consumeAsFlow(capacity = 1, receiveTimeout = 500.milliseconds)
                    .take(2)
                    .collect { message ->
                        nakDeliveries += message.data.toUtf8String()
                        if (nakDeliveries.size == 1) {
                            message.nak()
                        } else {
                            message.ack()
                        }
                    }
            }
            nakDeliveries shouldBeEqualTo listOf("nak-me", "nak-me")

            val termStream = "consumer-flow-manual-term"
            val termSubject = "consumer-flow.manual-term"
            connection.createOrReplaceStream(termStream, termSubject)
            jetStream.publish(termSubject, "term-me")
            val termConsumer = consumerContextOf(
                connection,
                termStream,
                consumerConfiguration {
                    durable("consumer-flow-manual-term-durable")
                    ackPolicy(AckPolicy.Explicit)
                    ackWait(Duration.ofMillis(500))
                    maxDeliver(2)
                    maxAckPending(1)
                },
            )
            val termFlow = termConsumer.consumeAsFlow(capacity = 1, receiveTimeout = 500.milliseconds)
            termFlow.take(1).collect { message ->
                message.term()
            }

            val redelivered = withTimeoutOrNull(2.seconds) {
                termFlow.take(1).collect { message ->
                    message.ack()
                }
                true
            } ?: false
            check(!redelivered) { "term() 처리 후 message가 재전달되었습니다." }
        }
    }
}
