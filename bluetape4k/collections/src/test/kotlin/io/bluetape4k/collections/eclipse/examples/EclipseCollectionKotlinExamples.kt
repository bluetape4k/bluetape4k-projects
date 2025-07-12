package io.bluetape4k.collections.eclipse.examples

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.primitives.asList
import io.bluetape4k.collections.eclipse.primitives.doubleArrayListOf
import io.bluetape4k.collections.eclipse.primitives.floatArrayListOf
import io.bluetape4k.collections.eclipse.primitives.intArrayListOf
import io.bluetape4k.collections.eclipse.primitives.longArrayListOf
import io.bluetape4k.collections.eclipse.unifiedMapOf
import io.bluetape4k.collections.eclipse.unifiedSetOf
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class EclipseCollectionKotlinExamples {

    companion object: KLogging()

    @Test
    fun `primitive array extension methods`() {
        val intArray = intArrayListOf(1, 2, 3, 4, 5)
        intArray.size() shouldBeEqualTo 5
        intArray.toArray() shouldBeEqualTo intArrayOf(1, 2, 3, 4, 5)

        val longArray = longArrayListOf(1, 2, 3, 4, 5)
        longArray.size() shouldBeEqualTo 5
        longArray.toArray() shouldBeEqualTo longArrayOf(1, 2, 3, 4, 5)

        val floatArray = floatArrayListOf(1F, 2F, 3F, 4F, 5F)
        floatArray.size() shouldBeEqualTo 5
        floatArray.toArray() shouldBeEqualTo floatArrayOf(1F, 2F, 3F, 4F, 5F)

        val doubleArray = doubleArrayListOf(1.0, 2.0, 3.0, 4.0, 5.0)
        doubleArray.size() shouldBeEqualTo 5
        doubleArray.toArray() shouldBeEqualTo doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
    }

    @Test
    fun `primitive array with asList`() {
        val ints = intArrayListOf(1, 2, 3, 4, 5).asList()
        ints.size shouldBeEqualTo 5
        ints shouldBeEqualTo listOf(1, 2, 3, 4, 5)

        val longs = longArrayListOf(1, 2, 3, 4, 5).asList()
        longs.size shouldBeEqualTo 5
        longs shouldBeEqualTo listOf(1, 2, 3, 4, 5)
    }

    @Test
    fun `verify fastListOf`() {
        val empty = fastListOf<Any>()
        empty.isEmpty.shouldBeTrue()

        val ints = fastListOf(1, 2, 3, 4, 5)
        ints.size shouldBeEqualTo 5
        ints shouldBeEqualTo listOf(1, 2, 3, 4, 5)
    }

    @Test
    fun `verify unifiedSetOf`() {
        val set = unifiedSetOf(1, 2, 2, 3, 3)
        set.size shouldBeEqualTo 3
        set shouldBeEqualTo setOf(1, 2, 3)
    }

    @Test
    fun `verify unifiedMapOf`() {
        val map = unifiedMapOf(
            1 to "a",
            2 to "b",
            3 to "c"
        )

        map.size shouldBeEqualTo 3
        map[1] shouldBeEqualTo "a"
        map[2] shouldBeEqualTo "b"
        map[3] shouldBeEqualTo "c"
        map[4].shouldBeNull()
    }
}
