package io.bluetape4k.coroutines

import io.bluetape4k.utils.ShutdownQueue
import kotlinx.coroutines.CoroutineScope

class ThreadPoolCoroutineScopeTest: AbstractCoroutineScopeTest() {

    override val coroutineScope: CoroutineScope = ThreadPoolCoroutineScope()
        .apply {
            ShutdownQueue.register(this)
        }
}
