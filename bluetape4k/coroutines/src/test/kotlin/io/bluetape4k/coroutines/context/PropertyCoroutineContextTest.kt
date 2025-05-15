package io.bluetape4k.coroutines.context

import io.bluetape4k.coroutines.support.log
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class PropertyCoroutineContextTest {

    companion object: KLoggingChannel()

    @Test
    fun `속성을 가지는 CoroutineContext 사용하기`() = runTest {
        val props = mapOf("key1" to 1, "key2" to "two")
        val ctx = PropertyCoroutineContext(props)

        val propCtx: PropertyCoroutineContext = ctx[PropertyCoroutineContext]!!
        propCtx["key1"] shouldBeEqualTo 1
        propCtx["key2"] shouldBeEqualTo "two"

        propCtx["key3"] = 42L
        propCtx["key3"] shouldBeEqualTo 42L
    }

    @Test
    fun `속성을 가진 CoroutineContext 전달하기`() = runTest {
        val props = mapOf("key1" to 1, "key2" to "two")
        val ctx = PropertyCoroutineContext(props)

        val scope = CoroutineScope(ctx) + SupervisorJob()

        // Scope를 생성할 때 전달한 CoroutineContext를 사용한다.
        val job1 = scope.launch {
            val propCtx = coroutineContext[PropertyCoroutineContext]!!
            propCtx["key1"] shouldBeEqualTo 1
            propCtx["key2"] shouldBeEqualTo "two"
        }.log("#1")
        yield()

        // Scope를 생성할 때 전달한 CoroutineContext를 사용한다.
        val job2 = launch(scope.coroutineContext) {
            val propCtx = coroutineContext[PropertyCoroutineContext]!!
            propCtx["key1"] shouldBeEqualTo 1
            propCtx["key2"] shouldBeEqualTo "two"
        }.log("#2")
        yield()

        job1.join()
        job2.join()
    }
}
