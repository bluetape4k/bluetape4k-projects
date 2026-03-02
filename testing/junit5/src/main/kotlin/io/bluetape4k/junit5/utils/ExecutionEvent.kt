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
 * ## 동작/계약
 * - 이벤트 종류([EventType]), 대상 [TestDescriptor], 선택 payload를 함께 보관합니다.
 * - payload는 nullable이며, [getPayload]로 타입 안전 조회를 지원합니다.
 * - companion factory 함수로 각 JUnit 엔진 콜백 이벤트를 일관된 구조로 생성합니다.
 *
 * ```kotlin
 * val evt = ExecutionEvent.executionStarted(descriptor)
 * // evt.type == ExecutionEvent.EventType.STARTED
 * ```
 *
 * @see RecordingExecutionListener
 */
data class ExecutionEvent(
    val type: EventType,
    val testDescriptor: TestDescriptor,
    val payload: Any? = null,
): Serializable {

    /**
     * 엔진 실행 이벤트 종류입니다.
     *
     * ## 동작/계약
     * - JUnit EngineExecutionListener 콜백 이름과 1:1 대응합니다.
     */
    enum class EventType {
        /** 동적 테스트가 등록됨 */
        DYNAMIC_TEST_REGISTERED,
        /** 테스트가 스킵됨 */
        SKIPPED,
        /** 테스트 실행이 시작됨 */
        STARTED,
        /** 테스트 실행이 종료됨 */
        FINISHED,
        /** 리포팅 엔트리가 발행됨 */
        REPORTING_ENTRY_PUBLISHED
    }

    /**
     * payload를 지정 타입으로 조회합니다.
     *
     * ## 동작/계약
     * - payload가 null이거나 타입이 다르면 null을 반환합니다.
     * - 타입이 일치하면 안전 캐스팅한 값을 반환합니다.
     *
     * @param payloadClass 기대하는 payload 런타임 타입
     */
    fun <T: Any> getPayload(payloadClass: Class<T>): T? = when {
        payload != null && payloadClass.isInstance(payload) -> payloadClass.cast(payload)
        else                                                -> null
    }

    companion object: KLogging() {
        /** REPORTING_ENTRY_PUBLISHED 이벤트를 생성합니다. */
        fun reportingEntryPublished(testDescriptor: TestDescriptor, entry: ReportEntry): ExecutionEvent {
            log.trace { "reporting entry published. entry=$entry" }
            return ExecutionEvent(EventType.REPORTING_ENTRY_PUBLISHED, testDescriptor, entry)
        }

        /** DYNAMIC_TEST_REGISTERED 이벤트를 생성합니다. */
        fun dynamicTestRegistered(testDescriptor: TestDescriptor): ExecutionEvent {
            log.trace { "dynamic test registered. testDescriptor=$testDescriptor" }
            return ExecutionEvent(EventType.DYNAMIC_TEST_REGISTERED, testDescriptor)
        }

        /** SKIPPED 이벤트를 생성합니다. */
        fun executionSkipped(testDescriptor: TestDescriptor, reason: String? = null): ExecutionEvent {
            log.trace { "execution skipped. reason=$reason" }
            return ExecutionEvent(EventType.SKIPPED, testDescriptor, reason)
        }

        /** STARTED 이벤트를 생성합니다. */
        fun executionStarted(testDescriptor: TestDescriptor): ExecutionEvent {
            log.trace { "execution is started. testDescriptor=$testDescriptor" }
            return ExecutionEvent(EventType.STARTED, testDescriptor, null)
        }

        /** FINISHED 이벤트를 생성합니다. */
        fun executionFinished(testDescriptor: TestDescriptor, result: TestExecutionResult): ExecutionEvent {
            if (result.throwable.isPresent) {
                log.error(result.throwable.get()) { "execution is failed. status=${result.status}" }
            } else {
                log.trace { "execution is finished. testDescriptor=$testDescriptor, result=$result" }
            }
            return ExecutionEvent(EventType.FINISHED, testDescriptor, result)
        }

        /**
         * 특정 [EventType]과 일치하는지 판별하는 predicate를 반환합니다.
         */
        fun byEventType(type: EventType) = { evt: ExecutionEvent -> evt.type == type }

        /**
         * [TestDescriptor] predicate를 적용하는 이벤트 predicate를 반환합니다.
         */
        fun byTestDescriptor(predicate: (TestDescriptor) -> Boolean) =
            { evt: ExecutionEvent -> predicate.invoke(evt.testDescriptor) }

        /**
         * payload 타입/값 predicate를 적용하는 이벤트 predicate를 반환합니다.
         */
        fun <T: Any> byPayload(payloadClass: KClass<T>, predicate: (T) -> Boolean) =
            { evt: ExecutionEvent -> evt.getPayload(payloadClass.java)?.run { predicate(this) } ?: false }

    }
}
