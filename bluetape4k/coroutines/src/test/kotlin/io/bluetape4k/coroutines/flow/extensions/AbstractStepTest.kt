package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.AfterEach

abstract class AbstractStepTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    private var actionIndex = 0
    private var finished = false

    protected fun expect(index: Int) {
        val wasIndex = ++actionIndex
        check(index == wasIndex) {
            "Expecting action index[$index] but it is actually $wasIndex"
        }
    }

    protected fun expectUnreached() {
        error("Should not be reached, current action index is $actionIndex")
    }

    protected fun finish(index: Int) {
        expect(index)
        check(!finished) { "Should call `finish(...)` at most once" }
        finished = true
    }

    @AfterEach
    fun onComplete() {
        if (actionIndex != 0 && !finished) {
            error("Expecting that `finish(${actionIndex + 1})` was invoked, but it was not")
        }
    }
}
