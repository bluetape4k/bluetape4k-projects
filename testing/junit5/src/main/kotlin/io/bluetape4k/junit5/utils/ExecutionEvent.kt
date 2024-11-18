package io.bluetape4k.junit5.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.reporting.ReportEntry
import java.io.Serializable
import kotlin.reflect.KClass

/**
 * 테스트 실행 시 발생하는 이벤트를 나타내는 데이터 클래스입니다.
 *
 * @see RecordingExecutionListener
 */
data class ExecutionEvent(
    val type: EventType,
    val testDescriptor: TestDescriptor,
    val payload: Any? = null,
): Serializable {

    enum class EventType {
        DYNAMIC_TEST_REGISTERED,
        SKIPPED,
        STARTED,
        FINISHED,
        REPORTING_ENTRY_PUBLISHED
    }

    fun <T: Any> getPayload(payloadClass: Class<T>): T? = when {
        payload != null && payloadClass.isInstance(payload) -> payloadClass.cast(payload)
        else                                                -> null
    }

    companion object: KLogging() {
        fun reportingEntryPublished(testDescriptor: TestDescriptor, entry: ReportEntry): ExecutionEvent {
            log.trace { "reporting entry published. entry=$entry" }
            return ExecutionEvent(EventType.REPORTING_ENTRY_PUBLISHED, testDescriptor, entry)
        }

        fun dynamicTestRegistered(testDescriptor: TestDescriptor): ExecutionEvent {
            log.trace { "dynamic test registered. testDescriptor=$testDescriptor" }
            return ExecutionEvent(EventType.DYNAMIC_TEST_REGISTERED, testDescriptor)
        }

        fun executionSkipped(testDescriptor: TestDescriptor, reason: String? = null): ExecutionEvent {
            log.trace { "execution skipped. reason=$reason" }
            return ExecutionEvent(EventType.SKIPPED, testDescriptor, reason)
        }

        fun executionStarted(testDescriptor: TestDescriptor): ExecutionEvent {
            log.trace { "execution is started. testDescriptor=$testDescriptor" }
            return ExecutionEvent(EventType.STARTED, testDescriptor, null)
        }

        fun executionFinished(testDescriptor: TestDescriptor, result: TestExecutionResult): ExecutionEvent {
            if (result.throwable.isPresent) {
                log.error(result.throwable.get()) { "execution is failed. status=${result.status}" }
            } else {
                log.trace { "execution is finished. testDescriptor=$testDescriptor, result=$result" }
            }
            return ExecutionEvent(EventType.FINISHED, testDescriptor, result)
        }

        fun byEventType(type: EventType) = { evt: ExecutionEvent -> evt.type == type }

        fun byTestDescriptor(predicate: (TestDescriptor) -> Boolean) =
            { evt: ExecutionEvent -> predicate.invoke(evt.testDescriptor) }

        fun <T: Any> byPayload(payloadClass: KClass<T>, predicate: (T) -> Boolean) =
            { evt: ExecutionEvent -> evt.getPayload(payloadClass.java)?.run { predicate(this) } ?: false }

    }
}
