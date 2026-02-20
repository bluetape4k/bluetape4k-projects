package io.bluetape4k.coroutines

class IoCoroutineScopeTest: AbstractCoroutineScopeTest() {

    override fun getCoroutineScope(): CloseableCoroutineScope =
        IoCoroutineScope()

}
