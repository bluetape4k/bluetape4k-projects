package io.bluetape4k.coroutines.flow.exceptions

import kotlinx.coroutines.flow.FlowCollector
import kotlin.coroutines.cancellation.CancellationException

@PublishedApi
internal class StopException(val owner: FlowCollector<*>):
    CancellationException("Flow was stopped, no more elements needed")

@PublishedApi
internal fun StopException.checkOwnership(owner: FlowCollector<*>) {
    if (this.owner !== owner) throw this
}
