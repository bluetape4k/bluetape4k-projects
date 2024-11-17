package io.bluetape4k.coroutines

import kotlinx.coroutines.CoroutineScope

class ThreadPoolCoroutineScopeTest: AbstractCoroutineScopeTest() {

    override val coroutineScope: CoroutineScope = ThreadPoolCoroutineScope()
}
