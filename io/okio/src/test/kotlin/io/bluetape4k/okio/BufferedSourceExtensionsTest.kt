package io.bluetape4k.okio

import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.coroutines.flow.toList
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

class BufferedSourceExtensionsTest: AbstractOkioTest() {
    @Test
    fun `readUtf8Lines returns all lines in order`() {
        val source = bufferOf("a\nb\nc\n").asBufferedSource()

        source.readUtf8Lines().toList() shouldBeEqualTo listOf("a", "b", "c")
    }

    @Test
    fun `readUtf8LinesAsFlow emits all lines in order`() =
        runSuspendIO {
            val source = bufferOf("a\nb\nc\n").asBufferedSource()

            source.readUtf8LinesAsFlow().toList() shouldBeEqualTo listOf("a", "b", "c")
        }

    @Test
    fun `readUtf8LinesAsFlow emits empty when source is empty`() =
        runSuspendIO {
            val source = bufferOf("").asBufferedSource()

            source.readUtf8LinesAsFlow().toList() shouldBeEqualTo emptyList()
        }
}
