package io.bluetape4k.coroutines.support

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

class CoroutineContextSupportTest {

    @Test
    fun `empty context는 현재 coroutine context를 반환한다`() = runTest {
        val expected = currentCoroutineContext()
        EmptyCoroutineContext.getOrCurrent() shouldBeEqualTo expected
    }

    @Test
    fun `non-empty context는 자기 자신을 반환한다`() = runTest {
        val context = CoroutineName("test-context")
        context.getOrCurrent() shouldBeEqualTo context
    }
}

