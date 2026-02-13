package io.bluetape4k.coroutines

import kotlinx.coroutines.CoroutineScope
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import kotlin.coroutines.cancellation.CancellationException

class VirtualThreadCoroutineScopeTest: AbstractCoroutineScopeTest() {

    override val coroutineScope: CoroutineScope = VirtualThreadCoroutineScope()

    @Test
    fun `clearJobs는 취소 원인을 전달한다`() {
        val scope = VirtualThreadCoroutineScope()
        scope.clearJobs(CancellationException("by-test"))

        scope.coroutineContext[kotlinx.coroutines.Job]
            ?.getCancellationException()
            ?.message
            .orEmpty() shouldContain "by-test"
    }
}
