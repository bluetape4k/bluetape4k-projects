package io.bluetape4k.coroutines

import kotlinx.coroutines.CoroutineScope

class VirtualThreadCoroutineScopeTest: AbstractCoroutineScopeTest() {

    override val coroutineScope: CoroutineScope = VirtualThreadCoroutineScope()
}
