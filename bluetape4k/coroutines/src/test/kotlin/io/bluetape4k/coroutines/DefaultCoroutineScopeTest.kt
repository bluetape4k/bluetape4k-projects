package io.bluetape4k.coroutines

import kotlinx.coroutines.CoroutineScope

class DefaultCoroutineScopeTest: AbstractCoroutineScopeTest() {

    override val coroutineScope: CoroutineScope = DefaultCoroutineScope()

}
