package io.bluetape4k.coroutines.context

import io.bluetape4k.coroutines.support.log
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
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

        val snapshot = propCtx.properties
        snapshot.containsKey("key3").shouldBeTrue()
        propCtx["key4"] = 99
        snapshot.containsKey("key4").shouldBeFalse()
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
