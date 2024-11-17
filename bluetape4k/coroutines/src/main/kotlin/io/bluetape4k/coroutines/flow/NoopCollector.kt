package io.bluetape4k.coroutines.flow

import kotlinx.coroutines.flow.FlowCollector

/**
 * 아무 일도 하지 않는 [FlowCollector] 입니다.
 */
object NoopCollector: FlowCollector<Any?> {
    override suspend fun emit(value: Any?) {
        // Nothing to do
    }
}
