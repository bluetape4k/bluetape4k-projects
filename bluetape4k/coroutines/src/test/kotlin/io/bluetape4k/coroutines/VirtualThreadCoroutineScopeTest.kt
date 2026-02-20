package io.bluetape4k.coroutines

class VirtualThreadCoroutineScopeTest: AbstractCoroutineScopeTest() {

    override fun getCoroutineScope(): CloseableCoroutineScope =
        VirtualThreadCoroutineScope()

}
