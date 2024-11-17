package io.bluetape4k.coroutines.flow.exceptions

import kotlinx.coroutines.flow.FlowCollector
import kotlin.coroutines.cancellation.CancellationException

/**
 * [FlowCollector] 가 더 이상 요소를 받지 않을 때 발생하는 예외입니다.
 */
@PublishedApi
internal class StopException(val owner: FlowCollector<*>):
    CancellationException("Flow was stopped, no more elements needed")

/**
 * [StopException]의 owner가 [FlowCollector]와 일치하지 않을 때 예외를 발생시킵니다.
 */
@PublishedApi
internal fun StopException.checkOwnership(owner: FlowCollector<*>) {
    if (this.owner !== owner) throw this
}
