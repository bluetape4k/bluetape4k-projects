package io.bluetape4k.junit5.utils

import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.reporting.ReportEntry
import java.util.concurrent.CopyOnWriteArrayList

/**
 * JUnit 엔진 실행 이벤트를 메모리에 기록하는 [EngineExecutionListener] 구현체입니다.
 *
 * ## 동작/계약
 * - 각 콜백에서 [ExecutionEvent]를 생성해 내부 리스트에 순차 추가합니다.
 * - 내부 저장소는 `CopyOnWriteArrayList`여서 읽기 중심 조회에 안전합니다.
 * - 조회 함수는 저장된 이벤트 목록을 필터링해 새 리스트로 반환합니다.
 *
 * ```kotlin
 * val listener = RecordingExecutionListener()
 * // 실행 후 listener.countEventsByType(ExecutionEvent.EventType.STARTED) >= 0
 * ```
 */
class RecordingExecutionListener: EngineExecutionListener {

    private val events = CopyOnWriteArrayList<ExecutionEvent>()

    override fun dynamicTestRegistered(testDescriptor: TestDescriptor) {
        addEvent(ExecutionEvent.dynamicTestRegistered(testDescriptor))
    }

    override fun executionSkipped(testDescriptor: TestDescriptor, reason: String) {
        addEvent(ExecutionEvent.executionSkipped(testDescriptor, reason))
    }

    override fun executionStarted(testDescriptor: TestDescriptor) {
        addEvent(ExecutionEvent.executionStarted(testDescriptor))
    }

    override fun executionFinished(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult) {
        addEvent(ExecutionEvent.executionFinished(testDescriptor, testExecutionResult))
    }

    override fun reportingEntryPublished(testDescriptor: TestDescriptor, entry: ReportEntry) {
        addEvent(ExecutionEvent.reportingEntryPublished(testDescriptor, entry))
    }

    /** 주어진 이벤트 타입과 일치하는 이벤트를 반환합니다. */
    fun getEventsByType(type: ExecutionEvent.EventType): List<ExecutionEvent> {
        return events.filter { ExecutionEvent.byEventType(type).invoke(it) }
    }

    /** 이벤트 타입과 테스트 디스크립터 조건을 함께 만족하는 이벤트를 반환합니다. */
    fun getEventsByTypeAndTestDescriptor(
        type: ExecutionEvent.EventType,
        predicate: (TestDescriptor) -> Boolean,
    ): List<ExecutionEvent> {
        return events.filter {
            ExecutionEvent.byEventType(type).invoke(it) && ExecutionEvent.byTestDescriptor(predicate).invoke(it)
        }
    }

    /** 특정 이벤트 타입의 발생 횟수를 반환합니다. */
    fun countEventsByType(type: ExecutionEvent.EventType): Int = getEventsByType(type).count()

    /** FINISHED 이벤트 중 지정한 실행 상태와 일치하는 이벤트만 반환합니다. */
    fun getFinishedEventsByStatus(status: TestExecutionResult.Status): List<ExecutionEvent> {
        return getEventsByType(ExecutionEvent.EventType.FINISHED)
            .filter { evt ->
                ExecutionEvent.byPayload(TestExecutionResult::class) { it.status == status }.invoke(evt)
            }
    }

    private fun addEvent(event: ExecutionEvent) {
        events.add(event)
    }
}
