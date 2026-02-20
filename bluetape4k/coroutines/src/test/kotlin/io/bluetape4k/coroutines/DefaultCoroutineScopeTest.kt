package io.bluetape4k.coroutines

class DefaultCoroutineScopeTest: AbstractCoroutineScopeTest() {

    override fun getCoroutineScope(): CloseableCoroutineScope =
        DefaultCoroutineScope()

}
