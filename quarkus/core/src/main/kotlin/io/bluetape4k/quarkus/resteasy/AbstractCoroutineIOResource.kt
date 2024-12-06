package io.bluetape4k.quarkus.resteasy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren

/**
 * [Dispatchers.IO]를 사용하는 [CoroutineScope] 환경에서 실행되는 REST Resource 입니다.
 */
abstract class AbstractCoroutineIOResource
    : CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()),
      AutoCloseable {

    override fun close() {
        runCatching { coroutineContext.cancelChildren() }
        runCatching { coroutineContext.cancel() }
    }
}
