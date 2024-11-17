package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

/**
 * 아무것도 emit 하지 않는 [Flow] 입니다.
 */
sealed interface NeverFlow: Flow<Nothing> {

    // Singleton object
    companion object: NeverFlow {
        override suspend fun collect(collector: FlowCollector<Nothing>) = awaitCancellation()
    }

    override suspend fun collect(collector: FlowCollector<Nothing>)
}

/**
 * 아무것도 emit 하지 않는 [Flow]를 반환합니다
 *
 * ```
 * val job = launch(start = CoroutineStart.UNDISPATCHED) {
 *        neverFlow().toList(list)
 * }
 * ```
 */
fun neverFlow(): NeverFlow = NeverFlow
