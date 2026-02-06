package io.bluetape4k.collections.eclipse.stream

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.primitives.doubleArrayListOf
import io.bluetape4k.collections.eclipse.primitives.floatArrayListOf
import io.bluetape4k.collections.eclipse.primitives.intArrayListOf
import io.bluetape4k.collections.eclipse.primitives.longArrayListOf
import io.bluetape4k.collections.eclipse.unifiedSet
import io.bluetape4k.collections.eclipse.unifiedSetOf
import io.bluetape4k.collections.toDoubleArray
import io.bluetape4k.collections.toIntArray
import io.bluetape4k.collections.toIntStream
import io.bluetape4k.collections.toLongArray
import io.bluetape4k.collections.toLongStream
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream

class JavaStreamSupportTest: AbstractCollectionTest() {

    companion object: KLogging() {
        private const val ELEMENT_SIZE = 10
    }

    @Test
    fun `Primitive Stream to FastList`() {
        IntStream.rangeClosed(1, 10).toFastList() shouldBeEqualTo fastList(10) { it + 1 }
        LongStream.rangeClosed(1, 10).toFastList() shouldBeEqualTo fastList(10) { it + 1L }
        DoubleStream.of(1.0, 2.0, 3.0).toFastList() shouldBeEqualTo fastListOf(1.0, 2.0, 3.0)
    }

    @Test
    fun `Primitive Stream to UnifiedSet`() {
        IntStream.rangeClosed(1, 10).toUnifiedSet() shouldBeEqualTo unifiedSet(10) { it + 1 }
        LongStream.rangeClosed(1, 10).toUnifiedSet() shouldBeEqualTo unifiedSet(10) { it + 1L }
        DoubleStream.of(1.0, 2.0, 2.0, 3.0).toUnifiedSet() shouldBeEqualTo unifiedSetOf(1.0, 2.0, 3.0)
    }

    @Test
    fun `Primitive Stream to Primitive ArrayList`() {
        IntStream.rangeClosed(1, 3).toIntArrayList() shouldBeEqualTo intArrayListOf(1, 2, 3)
        LongStream.rangeClosed(1, 3).toLongArrayList() shouldBeEqualTo longArrayListOf(1, 2, 3)
        DoubleStream.of(1.0, 3.0, 2.0).toFloatArrayList() shouldBeEqualTo floatArrayListOf(1.0F, 3.0F, 2.0F)
        DoubleStream.of(1.0, 3.0, 2.0).toDoubleArrayList() shouldBeEqualTo doubleArrayListOf(1.0, 3.0, 2.0)
    }

    @Test
    fun `Primitive ArrayList to primitive stream`() {
        intArrayListOf(1, 3, 2).toIntStream().toIntArray() shouldBeEqualTo intArrayOf(1, 3, 2)
        longArrayListOf(1, 3, 2).toLongStream().toLongArray() shouldBeEqualTo longArrayOf(1, 3, 2)
        floatArrayListOf(1.0F, 3.0F, 2.0F).toDoubleStream().toDoubleArray() shouldBeEqualTo doubleArrayOf(1.0, 3.0, 2.0)
        doubleArrayListOf(1.0, 3.0, 2.0).toDoubleStream().toDoubleArray() shouldBeEqualTo doubleArrayOf(1.0, 3.0, 2.0)
    }

    @Test
    fun `Closed range to stream`() {
        (1..3).toIntStream().toArray() shouldBeEqualTo intArrayOf(1, 2, 3)
        (1 until 3).toIntStream().toArray() shouldBeEqualTo intArrayOf(1, 2)

        (1..3L).toLongStream().toArray() shouldBeEqualTo longArrayOf(1, 2, 3)
        (1 until 3L).toLongStream().toArray() shouldBeEqualTo longArrayOf(1, 2)
    }
}
