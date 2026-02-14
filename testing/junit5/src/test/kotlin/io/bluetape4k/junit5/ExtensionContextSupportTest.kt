package io.bluetape4k.junit5

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext

class ExtensionContextSupportTest {

    @Test
    fun `class 와 kclass namespace 는 동일하다`() {
        val context = mockk<ExtensionContext>()

        val nsByClass = context.namespace(Sample::class.java)
        val nsByKClass = context.namespace(Sample::class)

        nsByKClass shouldBeEqualTo nsByClass
    }

    @Test
    fun `store 유틸은 namespace 기반 getStore 를 호출한다`() {
        val context = mockk<ExtensionContext>()
        val store = mockk<ExtensionContext.Store>()
        every { context.getStore(any()) } returns store

        context.store(Sample::class.java)
        context.store(Sample::class)

        verify(exactly = 2) { context.getStore(any()) }
    }

    class Sample
}
