package io.bluetape4k.coroutines.context

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContainKey
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldNotContainKey
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PropertyCoroutineContextTest {

    companion object: KLoggingChannel()

    @Test
    fun `속성을 가지는 CoroutineContext 사용하기`() = runTest {
        val props = mapOf("key1" to 1, "key2" to "two")
        val ctx = PropertyCoroutineContext(props)

        val propCtx: PropertyCoroutineContext = ctx[PropertyCoroutineContext].shouldNotBeNull()
        propCtx["key1"] shouldBeEqualTo 1
        propCtx["key2"] shouldBeEqualTo "two"

        propCtx["key3"] = 42L
        propCtx["key3"] shouldBeEqualTo 42L

        val snapshot = propCtx.properties
        snapshot.shouldContainKey("key3")
        propCtx["key4"] = 99
        snapshot.shouldNotContainKey("key4")
    }

    @Test
    fun `속성을 가진 CoroutineContext 전달하기`() = runTest {
        val props = mapOf("key1" to 1, "key2" to "two")
        val ctx = PropertyCoroutineContext(props)

        val scope = CoroutineScope(ctx) + SupervisorJob()

        val job1 = scope.launch {
            val propCtx = coroutineContext[PropertyCoroutineContext]!!
            propCtx["key1"] shouldBeEqualTo 1
            propCtx["key2"] shouldBeEqualTo "two"
        }.log("#1")

        val job2 = scope.launch {
            val propCtx = coroutineContext[PropertyCoroutineContext]!!
            propCtx["key1"] shouldBeEqualTo 1
            propCtx["key2"] shouldBeEqualTo "two"
        }.log("#2")

        job1.join()
        job2.join()
    }

    @Test
    fun `putAll로 속성을 한번에 추가할 수 있다`() = runTest {
        val ctx = PropertyCoroutineContext()
        ctx.putAll("a" to 1, "b" to "two")
        ctx.putAll(mapOf("c" to 3L))

        ctx["a"] shouldBeEqualTo 1
        ctx["b"] shouldBeEqualTo "two"
        ctx["c"] shouldBeEqualTo 3L
    }
}
