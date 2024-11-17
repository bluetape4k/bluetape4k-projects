package io.bluetape4k.coroutines

import kotlinx.coroutines.CoroutineScope

class IoCoroutineScopeTest: AbstractCoroutineScopeTest() {

    override val coroutineScope: CoroutineScope = IoCoroutineScope()

}
