package io.bluetape4k.io.okio

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class BufferedSourceExtensionsTest: AbstractOkioTest() {

    @Test
    fun `readUtf8Lines returns all lines in order`() {
        val source = bufferOf("a\nb\nc\n").asBufferedSource()

        source.readUtf8Lines().toList() shouldBeEqualTo listOf("a", "b", "c")
    }

    @Test
    fun `readUtf8LinesAsFlow emits all lines in order`() {
        runBlocking {
            val source = bufferOf("a\nb\nc\n").asBufferedSource()

            source.readUtf8LinesAsFlow().toList() shouldBeEqualTo listOf("a", "b", "c")
        }
    }

    @Test
    fun `readUtf8LinesAsFlow emits empty when source is empty`() {
        runBlocking {
            val source = bufferOf("").asBufferedSource()

            source.readUtf8LinesAsFlow().toList() shouldBeEqualTo emptyList()
        }
    }
}
