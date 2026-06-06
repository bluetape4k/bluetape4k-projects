package io.bluetape4k.micrometer.observation.events

import io.bluetape4k.micrometer.observation.coroutines.withObservationContextSuspending
import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requireNotBlank
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.CancellationException
import java.io.Serializable

/**
 * Shared Observation names and key names for application event telemetry.
 *
 * ## Behaviour / Contract
 * - Publish operations use [PUBLISH_OBSERVATION_NAME].
 * - Consume operations use [CONSUME_OBSERVATION_NAME].
 * - Low-cardinality keys are bounded and safe for metrics.
 * - High-cardinality keys are recorded only when the caller explicitly supplies them.
 */
object EventTelemetryKeys {
    const val PUBLISH_OBSERVATION_NAME = "event.publish"
    const val CONSUME_OBSERVATION_NAME = "event.consume"

    const val EVENT_OPERATION = "event.operation"
    const val EVENT_TYPE = "event.type"
    const val OUTCOME = "outcome"
    const val EXCEPTION = "exception"
    const val CORRELATION_PRESENT = "correlation.present"

    const val MESSAGING_SYSTEM = "messaging.system"
    const val MESSAGING_DESTINATION_NAME = "messaging.destination.name"
    const val MESSAGING_OPERATION_NAME = "messaging.operation.name"
    const val MESSAGING_OPERATION_TYPE = "messaging.operation.type"
    const val MESSAGING_BATCH_MESSAGE_COUNT = "messaging.batch.message_count"

    const val CORRELATION_ID = "correlation.id"
    const val MESSAGING_MESSAGE_ID = "messaging.message.id"
    const val MESSAGING_CONVERSATION_ID = "messaging.conversation.id"
    const val EVENT_OPERATION_ID = "event.operation.id"
}

/**
 * Event telemetry operation mapped to the shared Observation contract.
 */
enum class EventTelemetryOperation(
    val observationName: String,
    val operationName: String,
    val operationType: String,
) {
    PUBLISH(EventTelemetryKeys.PUBLISH_OBSERVATION_NAME, "publish", "send"),
    CONSUME(EventTelemetryKeys.CONSUME_OBSERVATION_NAME, "consume", "process"),
}

/**
 * Bounded event observation outcome.
 */
enum class EventTelemetryOutcome {
    SUCCESS,
    ERROR,
    CANCELLED,
}

/**
 * Messaging or application-event destination metadata.
 *
 * ## Behaviour / Contract
 * - [messagingSystem] is a bounded system name such as `spring`, `kafka`, `nats`, or `pulsar`.
 * - [name] is a stable destination name such as a topic, stream, or application event channel.
 */
@ConsistentCopyVisibility
data class EventDestination private constructor(
    val messagingSystem: String,
    val name: String,
): Serializable {

    init {
        messagingSystem.requireNotBlank("messagingSystem")
        name.requireNotBlank("name")
    }

    companion object {
        private const val serialVersionUID: Long = 3744577912634927554L

        operator fun invoke(
            messagingSystem: String,
            name: String,
        ): EventDestination =
            EventDestination(
                messagingSystem = messagingSystem,
                name = name,
            )

        fun spring(name: String): EventDestination =
            EventDestination(messagingSystem = "spring", name = name)
    }
}

/**
 * Correlation metadata for event telemetry.
 *
 * ## Behaviour / Contract
 * - [present] is recorded as low-cardinality `correlation.present`.
 * - [sanitizedId] is recorded as high-cardinality `correlation.id` only when [includeSanitizedId] is true.
 * - Raw correlation headers or payload values must not be passed directly.
 */
@ConsistentCopyVisibility
data class EventCorrelation private constructor(
    val present: Boolean,
    val sanitizedId: String? = null,
    val includeSanitizedId: Boolean = false,
): Serializable {

    init {
        sanitizedId?.requireNotBlank("sanitizedId")
        require(!includeSanitizedId || sanitizedId != null) {
            "sanitizedId is required when includeSanitizedId is true"
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1413799775854572414L

        val none: EventCorrelation =
            EventCorrelation(present = false)

        val present: EventCorrelation =
            EventCorrelation(present = true)

        operator fun invoke(
            present: Boolean,
            sanitizedId: String? = null,
            includeSanitizedId: Boolean = false,
        ): EventCorrelation =
            EventCorrelation(
                present = present,
                sanitizedId = sanitizedId,
                includeSanitizedId = includeSanitizedId,
            )

        fun sanitized(
            rawValue: String?,
            includeHighCardinalityId: Boolean = false,
            maxLength: Int = DEFAULT_CORRELATION_ID_MAX_LENGTH,
        ): EventCorrelation {
            val present = !rawValue.isNullOrBlank()
            val sanitizedId = sanitizeEventCorrelationId(rawValue, maxLength)
            return EventCorrelation(
                present = present,
                sanitizedId = sanitizedId,
                includeSanitizedId = includeHighCardinalityId && sanitizedId != null,
            )
        }
    }
}

/**
 * Optional high-cardinality event identifiers.
 *
 * ## Behaviour / Contract
 * These values are never created from payload bodies by this module. Callers must pass
 * already-sanitized identifiers and opt in deliberately because these keys can create
 * high-cardinality series.
 */
@ConsistentCopyVisibility
data class EventHighCardinality private constructor(
    val messageId: String? = null,
    val conversationId: String? = null,
    val operationId: String? = null,
    val custom: Map<String, String> = emptyMap(),
): Serializable {

    init {
        messageId?.requireNotBlank("messageId")
        conversationId?.requireNotBlank("conversationId")
        operationId?.requireNotBlank("operationId")
        custom.forEach { (key, value) ->
            key.requireNotBlank("custom.key")
            value.requireNotBlank("custom[$key]")
        }
    }

    companion object {
        private const val serialVersionUID: Long = -3461162596749467723L

        operator fun invoke(
            messageId: String? = null,
            conversationId: String? = null,
            operationId: String? = null,
            custom: Map<String, String> = emptyMap(),
        ): EventHighCardinality =
            EventHighCardinality(
                messageId = messageId,
                conversationId = conversationId,
                operationId = operationId,
                custom = custom,
            )
    }
}

/**
 * Event telemetry description applied to publish and consume Observation wrappers.
 *
 * ## Behaviour / Contract
 * - [eventType] should be a bounded application type, not a raw class name containing tenant/user data.
 * - [batchMessageCount] is recorded only when known and positive.
 * - [highCardinality] and [correlation] identifiers are explicit opt-ins.
 */
@ConsistentCopyVisibility
data class EventTelemetry private constructor(
    val destination: EventDestination,
    val eventType: String? = null,
    val correlation: EventCorrelation = EventCorrelation.none,
    val batchMessageCount: Int? = null,
    val highCardinality: EventHighCardinality = EventHighCardinality(),
): Serializable {

    init {
        eventType?.requireNotBlank("eventType")
        batchMessageCount?.requireInRange(1, Int.MAX_VALUE, "batchMessageCount")
    }

    companion object {
        private const val serialVersionUID: Long = -7009783647532555450L

        operator fun invoke(
            destination: EventDestination,
            eventType: String? = null,
            correlation: EventCorrelation = EventCorrelation.none,
            batchMessageCount: Int? = null,
            highCardinality: EventHighCardinality = EventHighCardinality(),
        ): EventTelemetry =
            EventTelemetry(
                destination = destination,
                eventType = eventType,
                correlation = correlation,
                batchMessageCount = batchMessageCount,
                highCardinality = highCardinality,
            )
    }
}

/**
 * Sanitizes an event correlation identifier for optional high-cardinality telemetry.
 *
 * ## Behaviour / Contract
 * - Keeps ASCII letters, digits, `_`, `-`, and `.` only.
 * - Trims and caps the result to [maxLength].
 * - Returns `null` for blank or fully stripped values.
 */
fun sanitizeEventCorrelationId(
    rawValue: String?,
    maxLength: Int = DEFAULT_CORRELATION_ID_MAX_LENGTH,
): String? {
    maxLength.requireInRange(1, 512, "maxLength")

    val value = rawValue?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return value
        .asSequence()
        .filter(::isSafeCorrelationIdChar)
        .take(maxLength)
        .joinToString("")
        .takeIf { it.isNotBlank() }
}

/**
 * Wraps an application event publish path in an `event.publish` Observation.
 */
fun <T> ObservationRegistry.observeEventPublish(
    telemetry: EventTelemetry,
    block: (Observation.Context) -> T,
): T =
    observeEvent(EventTelemetryOperation.PUBLISH, telemetry, block)

/**
 * Wraps an application event consume or handler path in an `event.consume` Observation.
 */
fun <T> ObservationRegistry.observeEventConsume(
    telemetry: EventTelemetry,
    block: (Observation.Context) -> T,
): T =
    observeEvent(EventTelemetryOperation.CONSUME, telemetry, block)

/**
 * Wraps a suspending application event publish path in an `event.publish` Observation.
 */
suspend fun <T: Any> ObservationRegistry.observeEventPublishSuspending(
    telemetry: EventTelemetry,
    block: suspend (Observation.Context) -> T,
): T =
    observeEventSuspending(EventTelemetryOperation.PUBLISH, telemetry, block)

/**
 * Wraps a suspending application event consume or handler path in an `event.consume` Observation.
 */
suspend fun <T: Any> ObservationRegistry.observeEventConsumeSuspending(
    telemetry: EventTelemetry,
    block: suspend (Observation.Context) -> T,
): T =
    observeEventSuspending(EventTelemetryOperation.CONSUME, telemetry, block)

private const val DEFAULT_CORRELATION_ID_MAX_LENGTH = 128
private const val VALID_CORRELATION_ID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_.-"

private fun <T> ObservationRegistry.observeEvent(
    operation: EventTelemetryOperation,
    telemetry: EventTelemetry,
    block: (Observation.Context) -> T,
): T {
    val observation = createEventObservation(operation, telemetry)
    val context = observation.context

    observation.start()
    return try {
        observation.openScope().use {
            block(context).also {
                observation.recordOutcome(EventTelemetryOutcome.SUCCESS)
            }
        }
    } catch (e: CancellationException) {
        observation.recordOutcome(EventTelemetryOutcome.CANCELLED)
        throw e
    } catch (e: Throwable) {
        observation.recordOutcome(EventTelemetryOutcome.ERROR)
        observation.lowCardinalityKeyValue(
            EventTelemetryKeys.EXCEPTION,
            e.javaClass.simpleName.ifBlank { e.javaClass.name },
        )
        observation.error(e)
        throw e
    } finally {
        observation.stop()
    }
}

private suspend fun <T: Any> ObservationRegistry.observeEventSuspending(
    operation: EventTelemetryOperation,
    telemetry: EventTelemetry,
    block: suspend (Observation.Context) -> T,
): T {
    val observation = createEventObservation(operation, telemetry)

    return observation.withObservationContextSuspending { context ->
        try {
            block(context).also {
                observation.recordOutcome(EventTelemetryOutcome.SUCCESS)
            }
        } catch (e: CancellationException) {
            observation.recordOutcome(EventTelemetryOutcome.CANCELLED)
            throw e
        } catch (e: Throwable) {
            observation.recordOutcome(EventTelemetryOutcome.ERROR)
            observation.lowCardinalityKeyValue(
                EventTelemetryKeys.EXCEPTION,
                e.javaClass.simpleName.ifBlank { e.javaClass.name },
            )
            throw e
        }
    } ?: throw NoSuchElementException("event observation block returned null")
}

private fun ObservationRegistry.createEventObservation(
    operation: EventTelemetryOperation,
    telemetry: EventTelemetry,
): Observation =
    Observation
        .createNotStarted(operation.observationName, this)
        .contextualName("${operation.operationName} ${telemetry.destination.name}")
        .lowCardinalityKeyValue(EventTelemetryKeys.EVENT_OPERATION, operation.operationName)
        .lowCardinalityKeyValue(EventTelemetryKeys.MESSAGING_OPERATION_NAME, operation.operationName)
        .lowCardinalityKeyValue(EventTelemetryKeys.MESSAGING_OPERATION_TYPE, operation.operationType)
        .lowCardinalityKeyValue(EventTelemetryKeys.MESSAGING_SYSTEM, telemetry.destination.messagingSystem)
        .lowCardinalityKeyValue(EventTelemetryKeys.MESSAGING_DESTINATION_NAME, telemetry.destination.name)
        .lowCardinalityKeyValue(EventTelemetryKeys.CORRELATION_PRESENT, telemetry.correlation.present.toString())
        .also { observation ->
            telemetry.eventType?.let {
                observation.lowCardinalityKeyValue(EventTelemetryKeys.EVENT_TYPE, it)
            }
            telemetry.batchMessageCount?.let {
                observation.lowCardinalityKeyValue(EventTelemetryKeys.MESSAGING_BATCH_MESSAGE_COUNT, it.toString())
            }
            telemetry.correlation.sanitizedId
                ?.takeIf { telemetry.correlation.includeSanitizedId }
                ?.let {
                    observation.highCardinalityKeyValue(EventTelemetryKeys.CORRELATION_ID, it)
                }
            telemetry.highCardinality.messageId?.let {
                observation.highCardinalityKeyValue(EventTelemetryKeys.MESSAGING_MESSAGE_ID, it)
            }
            telemetry.highCardinality.conversationId?.let {
                observation.highCardinalityKeyValue(EventTelemetryKeys.MESSAGING_CONVERSATION_ID, it)
            }
            telemetry.highCardinality.operationId?.let {
                observation.highCardinalityKeyValue(EventTelemetryKeys.EVENT_OPERATION_ID, it)
            }
            telemetry.highCardinality.custom.forEach { (key, value) ->
                observation.highCardinalityKeyValue(key, value)
            }
        }

private fun Observation.recordOutcome(outcome: EventTelemetryOutcome) {
    lowCardinalityKeyValue(EventTelemetryKeys.OUTCOME, outcome.name)
}

private fun isSafeCorrelationIdChar(char: Char): Boolean =
    char in VALID_CORRELATION_ID_CHARS
