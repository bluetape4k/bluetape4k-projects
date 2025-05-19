package io.bluetape4k.quarkus.resteasy

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren

/**
 * [Dispatchers.Default]를 사용하는 [CoroutineScope] 환경에서 실행되는 REST Resource 입니다.
 */
abstract class AbstractCoroutineDefaultResource
    : CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()),
      AutoCloseable {

    companion object: KLoggingChannel()

    override fun close() {
        runCatching { coroutineContext.cancelChildren() }
        runCatching { coroutineContext.cancel() }
    }
}
