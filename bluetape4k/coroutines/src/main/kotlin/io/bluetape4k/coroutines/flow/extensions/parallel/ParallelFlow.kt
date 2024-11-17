package io.bluetape4k.coroutines.flow.extensions.parallel

import kotlinx.coroutines.flow.FlowCollector

/**
 * 복수의 수집을 병렬로 수행하는 Flow의 기본 인터페이스
 */
interface ParallelFlow<out T> {

    val parallelism: Int

    suspend fun collect(vararg collectors: FlowCollector<T>)
}
