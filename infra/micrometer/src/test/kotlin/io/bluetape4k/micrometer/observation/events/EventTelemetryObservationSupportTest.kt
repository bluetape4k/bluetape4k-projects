package io.bluetape4k.micrometer.observation.events

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.assertCancellationPropagates
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class EventTelemetryObservationSupportTest {

    private fun registry(handler: RecordingObservationHandler): ObservationRegistry =
        ObservationRegistry.create().apply {
            observationConfig().observationHandler(handler)
        }

    private fun orderTelemetry(): EventTelemetry =
        EventTelemetry(
            destination = EventDestination("spring", "orders"),
            eventType = "OrderCreated",
            correlation = EventCorrelation.sanitized(
                rawValue = " trace-123/<script> ",
                includeHighCardinalityId = true,
            ),
            batchMessageCount = 3,
            highCardinality = EventHighCardinality(
                messageId = "message-1",
                conversationId = "conversation-1",
                operationId = "operation-1",
                custom = mapOf("event.retry.id" to "retry-1"),
            ),
        )

    @Test
    fun `observeEventPublish should add shared event tags and stop observation`() {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        val result =
            registry.observeEventPublish(orderTelemetry()) { context ->
                context.name shouldBeEqualTo EventTelemetryKeys.PUBLISH_OBSERVATION_NAME
                context.contextualName shouldBeEqualTo "publish orders"
                "published"
            }

        result shouldBeEqualTo "published"
        handler.started shouldBeEqualTo 1
        handler.stopped shouldBeEqualTo 1
        handler.errors shouldBeEqualTo 0

        val context = handler.stoppedContexts.single()
        context.low(EventTelemetryKeys.EVENT_OPERATION) shouldBeEqualTo "publish"
        context.low(EventTelemetryKeys.MESSAGING_OPERATION_NAME) shouldBeEqualTo "publish"
        context.low(EventTelemetryKeys.MESSAGING_OPERATION_TYPE) shouldBeEqualTo "send"
        context.low(EventTelemetryKeys.MESSAGING_SYSTEM) shouldBeEqualTo "spring"
        context.low(EventTelemetryKeys.MESSAGING_DESTINATION_NAME) shouldBeEqualTo "orders"
        context.low(EventTelemetryKeys.EVENT_TYPE) shouldBeEqualTo "OrderCreated"
        context.low(EventTelemetryKeys.MESSAGING_BATCH_MESSAGE_COUNT) shouldBeEqualTo "3"
        context.low(EventTelemetryKeys.CORRELATION_PRESENT) shouldBeEqualTo "true"
        context.low(EventTelemetryKeys.OUTCOME) shouldBeEqualTo EventTelemetryOutcome.SUCCESS.name
        context.high(EventTelemetryKeys.CORRELATION_ID) shouldBeEqualTo "trace-123script"
        context.high(EventTelemetryKeys.MESSAGING_MESSAGE_ID) shouldBeEqualTo "message-1"
        context.high(EventTelemetryKeys.MESSAGING_CONVERSATION_ID) shouldBeEqualTo "conversation-1"
        context.high(EventTelemetryKeys.EVENT_OPERATION_ID) shouldBeEqualTo "operation-1"
        context.high("event.retry.id") shouldBeEqualTo "retry-1"
    }

    @Test
    fun `observeEventConsume should use consume operation tags`() {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        val result =
            registry.observeEventConsume(
                EventTelemetry(
                    destination = EventDestination.spring("application-events"),
                    eventType = "UserRegistered",
                    correlation = EventCorrelation.present,
                ),
            ) {
                "consumed"
            }

        result shouldBeEqualTo "consumed"
        handler.started shouldBeEqualTo 1
        handler.stopped shouldBeEqualTo 1

        val context = handler.stoppedContexts.single()
        context.name shouldBeEqualTo EventTelemetryKeys.CONSUME_OBSERVATION_NAME
        context.contextualName shouldBeEqualTo "consume application-events"
        context.low(EventTelemetryKeys.EVENT_OPERATION) shouldBeEqualTo "consume"
        context.low(EventTelemetryKeys.MESSAGING_OPERATION_TYPE) shouldBeEqualTo "process"
        context.low(EventTelemetryKeys.CORRELATION_PRESENT) shouldBeEqualTo "true"
        context.high(EventTelemetryKeys.CORRELATION_ID).shouldBeNull()
    }

    @Test
    fun `observeEventConsume should record error and rethrow exception`() {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        assertFailsWith<IllegalStateException> {
            registry.observeEventConsume(
                EventTelemetry(destination = EventDestination("kafka", "orders")),
            ) {
                throw IllegalStateException("boom")
            }
        }

        handler.errors shouldBeEqualTo 1
        handler.stopped shouldBeEqualTo 1

        val context = handler.stoppedContexts.single()
        context.error.shouldNotBeNull()
        context.low(EventTelemetryKeys.OUTCOME) shouldBeEqualTo EventTelemetryOutcome.ERROR.name
        context.low(EventTelemetryKeys.EXCEPTION) shouldBeEqualTo "IllegalStateException"
    }

    @Test
    fun `observeEventPublishSuspending should stop observation on success`() = runTest {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        val result =
            registry.observeEventPublishSuspending(orderTelemetry()) { context ->
                context.name shouldBeEqualTo EventTelemetryKeys.PUBLISH_OBSERVATION_NAME
                "published"
            }

        result shouldBeEqualTo "published"
        handler.started shouldBeEqualTo 1
        handler.stopped shouldBeEqualTo 1
        handler.errors shouldBeEqualTo 0

        val context = handler.stoppedContexts.single()
        context.low(EventTelemetryKeys.OUTCOME) shouldBeEqualTo EventTelemetryOutcome.SUCCESS.name
    }

    @Test
    fun `observeEventPublish should not record cancellation as error`() {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        assertFailsWith<CancellationException> {
            registry.observeEventPublish(
                EventTelemetry(destination = EventDestination("nats", "orders")),
            ) {
                throw CancellationException("cancelled")
            }
        }

        handler.errors shouldBeEqualTo 0
        handler.stopped shouldBeEqualTo 1

        val context = handler.stoppedContexts.single()
        context.error.shouldBeNull()
        context.low(EventTelemetryKeys.OUTCOME) shouldBeEqualTo EventTelemetryOutcome.CANCELLED.name
        context.low(EventTelemetryKeys.EXCEPTION).shouldBeNull()
    }

    @Test
    fun `correlation id sanitizer should cap and strip unsafe characters`() {
        sanitizeEventCorrelationId(" abc-123_./한글?<script> ", maxLength = 10) shouldBeEqualTo "abc-123_.s"
        sanitizeEventCorrelationId(" 한글 ") shouldBeEqualTo null
        sanitizeEventCorrelationId(null) shouldBeEqualTo null
    }

    @Test
    fun `sanitized correlation should preserve presence even when id is stripped`() {
        val correlation = EventCorrelation.sanitized(" 한글 ", includeHighCardinalityId = true)

        correlation.present.shouldBeTrue()
        correlation.sanitizedId.shouldBeNull()
        correlation.includeSanitizedId.shouldBeFalse()
    }

    @Test
    fun `observeEventConsumeSuspending should propagate cancellation without error`() = runTest {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        assertCancellationPropagates {
            registry.observeEventConsumeSuspending(
                EventTelemetry(destination = EventDestination("pulsar", "orders")),
            ) {
                delay(100.milliseconds)
                "never"
            }
        }

        handler.errors shouldBeEqualTo 0
        handler.stopped shouldBeEqualTo 1
        handler.stoppedContexts.single()
            .low(EventTelemetryKeys.OUTCOME) shouldBeEqualTo EventTelemetryOutcome.CANCELLED.name
    }

    private class RecordingObservationHandler: ObservationHandler<Observation.Context> {
        var started = 0
        var stopped = 0
        var errors = 0
        val stoppedContexts = mutableListOf<Observation.Context>()

        override fun onStart(context: Observation.Context) {
            started++
        }

        override fun onStop(context: Observation.Context) {
            stopped++
            stoppedContexts += context
        }

        override fun onError(context: Observation.Context) {
            errors++
        }

        override fun supportsContext(context: Observation.Context): Boolean = true
    }
}

private fun Observation.Context.low(key: String): String? =
    getLowCardinalityKeyValue(key)?.value

private fun Observation.Context.high(key: String): String? =
    getHighCardinalityKeyValue(key)?.value
