package io.bluetape4k.coroutines.flow.extensions.subject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

/**
 * 지연 가능한 push 신호인 emit, emitError 및 complete 의 기본 인터페이스입니다.
 */
interface SubjectApi<T>: FlowCollector<T>, Flow<T> {

    /**
     * collector 가 등록되었는지 여부를 반환합니다.
     */
    val hasCollectors: Boolean

    /**
     * 등록된 collector 의 수를 반환합니다.
     */
    val collectorCount: Int

    /**
     * Throwable 을 collector 에게 전달합니다.
     */
    suspend fun emitError(ex: Throwable?)

    /**
     * 더 이상의 아이템이 생성되지 않음을 알립니다.
     */
    suspend fun complete()
}
