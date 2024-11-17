package io.bluetape4k.collections

import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.streams.toList
import kotlin.test.assertFailsWith

class JavaStreamSupportTest {

    companion object: KLogging()

    @Test
    fun `Sequence as ParallelStream`() {
        val counter = atomic(0)
        (1..1000).asParallelStream().forEach { counter.incrementAndGet() }
        counter.value shouldBeEqualTo 1000
    }

    @Test
    fun `Iterable to Stream`() {
        listOf(1, 2, 3).asStream().count() shouldBeEqualTo 3
        listOf(1, 2, 3).asStream().max { o1, o2 -> o1.compareTo(o2) }.get() shouldBeEqualTo 3
    }

    @Test
    fun `Primitive Array to Primitive Stream`() {
        intArrayOf(1, 2, 3).toIntStream().count() shouldBeEqualTo 3
        intArrayOf(1, 2, 3).toIntStream().max().asInt shouldBeEqualTo 3

        longArrayOf(1, 2, 3).toLongStream().count() shouldBeEqualTo 3
        longArrayOf(1, 2, 3).toLongStream().max().asLong shouldBeEqualTo 3L

        doubleArrayOf(1.0, 2.0, 3.0).toDoubleStream().count() shouldBeEqualTo 3
        doubleArrayOf(1.0, 2.0, 3.0).toDoubleStream().max().asDouble shouldBeEqualTo 3.0
    }

    @Test
    fun `List to Stream`() {
        listOf("a", "b", "c").asStream().count() shouldBeEqualTo 3
        listOf("a", "b", "c").asStream().max { o1, o2 -> o1.compareTo(o2) }.get() shouldBeEqualTo "c"
    }

    @Test
    fun `IntStream to Sequence`() {
        var count = 0
        IntStream.range(0, 100).asSequence().forEach { count++ }
        count shouldBeEqualTo 100
    }

    @Test
    fun `IntStream to Iterable and List`() {
        IntStream.range(0, 100).asIterable().toList().size shouldBeEqualTo 100
    }

    @Test
    fun `IntStream to IntArray`() {
        IntStream.range(0, 100).toIntArray().size shouldBeEqualTo 100
    }

    @Test
    fun `Stream to Sequence`() {
        var count = 0
        val sequence = Stream.generate { count++ }.asSequence()
        val result = sequence.take(5).toList()

        result shouldBeEqualTo List(5) { it }

        val values = IntStream.range(0, 100).mapToObj { "value-$it" }.toList()
        values.size shouldBeEqualTo 100
        values shouldBeEqualTo List(100) { "value-$it" }
    }

    @Test
    fun `Sequence to IntStream`() {
        val sequence = generateSequence(0) { it + 1 }.take(10)
        val stream = sequence.toIntStream()

        // Sequence 는 여러번 호출해도 된다.
        sequence.count() shouldBeEqualTo 10
        sequence.toList() shouldBeEqualTo List(10) { it }

        // NOTE: Stream은 한번 사용하면 다시 사용할 수 없다!!!
        stream.toList() shouldBeEqualTo List(10) { it }

        assertFailsWith<IllegalStateException> {
            stream.count() shouldBeEqualTo 10
        }
    }

    @Test
    fun `FloatArray to DoubleStream`() {
        val floatArray = floatArrayOf(1.0f, 2.0f, 3.0f)
        floatArray.toDoubleStream().toList() shouldBeEqualTo listOf(1.0, 2.0, 3.0)
    }
}
