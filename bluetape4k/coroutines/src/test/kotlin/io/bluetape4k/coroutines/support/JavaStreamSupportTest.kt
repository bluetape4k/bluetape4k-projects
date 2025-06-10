package io.bluetape4k.coroutines.support

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.stream.IntStream
import kotlin.random.Random

class JavaStreamSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `int stream with coMap`() = runTest {
        val list = IntStream.range(1, 10)
            .coMap {
                delay(Random.nextLong(10))
                it
            }
            .toList()

        list shouldBeEqualTo List(9) { it + 1 }
    }

    @Test
    fun `int stream with coForEach`() = runTest {
        val list = mutableListOf<Int>()
        IntStream.range(1, 10).coForEach {
            delay(Random.nextLong(10))
            list.add(it)
        }
        list shouldBeEqualTo List(9) { it + 1 }
    }
}
