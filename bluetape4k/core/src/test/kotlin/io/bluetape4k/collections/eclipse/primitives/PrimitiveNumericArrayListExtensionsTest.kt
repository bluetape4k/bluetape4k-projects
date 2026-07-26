package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.stream.toDoubleArrayList
import io.bluetape4k.collections.eclipse.stream.toIntArrayList
import io.bluetape4k.collections.eclipse.stream.toLongArrayList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.collections.toList
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.IntStream
import java.util.stream.LongStream

/**
 * Consolidated @TestFactory harness for numeric Eclipse Collections primitive ArrayList
 * extension tests. Replaces the five per-type test files
 * (Int/Long/Float/Double/Byte) with per-type [TypeDriver] objects that each
 * generate a stream of [DynamicTest] cases.
 */
class PrimitiveNumericArrayListExtensionsTest: AbstractCollectionTest() {

    companion object: KLogging()

    /**
     * Per-type driver — each generates a stream of [DynamicTest]
     * exercising the full extension-function surface for its primitive type.
     */
    private interface TypeDriver {
        val typeName: String
        fun tests(): List<DynamicTest>
    }

    private object IntDriver: TypeDriver {
        override val typeName: String = "Int"

        private val kotlinList = fastList(5) { it + 1 }
        private val kotlinSet = kotlinList.toUnifiedSet()
        private val expectedArray = intArrayOf(1, 2, 3, 4, 5)
        private val expectedArrayList = intArrayListOf(1, 2, 3, 4, 5)

        override fun tests(): List<DynamicTest> = listOf(
            dynamicTest("$typeName: kotlin array to eclipse array") {
                expectedArray.toIntArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: sequence to primitive array list") {
                kotlinList.take(5).toIntArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: iterable to primitive array list") {
                kotlinList.toIntArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: stream to primitive array list") {
                IntStream.range(1, 6).toIntArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: convert primitive array list") {
                val array = intArrayList(5) { it + 1 }
                array.size() shouldBeEqualTo 5
                array shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: primitive list asList") {
                val list = intArrayListOf(1, 2, 3, 4, 5).asList()
                list.size shouldBeEqualTo 5
                list shouldBeEqualTo kotlinList
            },
            dynamicTest("$typeName: primitive set asSet") {
                val set = intArrayListOf(1, 2, 2, 3, 3, 4, 5).asSet()
                set.size shouldBeEqualTo 5
                set shouldBeEqualTo kotlinSet
            },
            dynamicTest("$typeName: primitive array list to list") {
                val expected = listOf(1, 2, 3, 4, 4, 5)
                val array = intArrayListOf(1, 2, 3, 4, 4, 5)

                array.toArray() shouldBeEqualTo expected.toIntArray()
                array.asIterable().toList() shouldBeEqualTo expected
                array.asSequence().toList() shouldBeEqualTo expected
                array.asIterator().toList() shouldBeEqualTo expected
                array.asList() shouldBeEqualTo expected
                array.asSet() shouldBeEqualTo expected.toSet()
                array.toFastList() shouldBeEqualTo expected
                array.toUnifiedSet() shouldBeEqualTo expected.toSet()
            },
            dynamicTest("$typeName: get product") {
                intArrayListOf(1, 3, 5).product() shouldBeEqualTo (1 * 3 * 5).toDouble()
                intArrayListOf(-1, -3, -5).product() shouldBeEqualTo (-1 * -3 * -5).toDouble()
            },
        )
    }

    private object LongDriver: TypeDriver {
        override val typeName: String = "Long"

        private val kotlinList = fastList(5) { it + 1L }
        private val kotlinSet = kotlinList.toUnifiedSet()
        private val expectedArray = longArrayOf(1, 2, 3, 4, 5)
        private val expectedArrayList = longArrayListOf(1, 2, 3, 4, 5)

        override fun tests(): List<DynamicTest> = listOf(
            dynamicTest("$typeName: kotlin array to eclipse array") {
                expectedArray.toLongArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: sequence to primitive array list") {
                kotlinList.take(5).toLongArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: iterable to primitive array list") {
                kotlinList.toLongArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: stream to primitive array list") {
                LongStream.range(1, 6).toLongArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: convert primitive array list") {
                val array = longArrayList(5) { it + 1L }
                array.size() shouldBeEqualTo 5
                array shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: primitive list asList") {
                val list = longArrayListOf(1, 2, 3, 4, 5).asList()
                list.size shouldBeEqualTo 5
                list shouldBeEqualTo kotlinList
            },
            dynamicTest("$typeName: primitive set asSet") {
                val set = longArrayListOf(1, 2, 2, 3, 3, 4, 5).asSet()
                set.size shouldBeEqualTo 5
                set shouldBeEqualTo kotlinSet
            },
            dynamicTest("$typeName: primitive array list to list") {
                val expected = listOf<Long>(1, 2, 3, 4, 4, 5)
                val array = longArrayListOf(1, 2, 3, 4, 4, 5)

                array.toArray() shouldBeEqualTo expected.toLongArray()
                array.asIterable().toList() shouldBeEqualTo expected
                array.asSequence().toList() shouldBeEqualTo expected
                array.asIterator().toList() shouldBeEqualTo expected
                array.asList() shouldBeEqualTo expected
                array.asSet() shouldBeEqualTo expected.toSet()
                array.toFastList() shouldBeEqualTo expected
                array.toUnifiedSet() shouldBeEqualTo expected.toSet()
            },
            dynamicTest("$typeName: get product") {
                longArrayListOf(1, 3, 5).product() shouldBeEqualTo (1 * 3 * 5).toDouble()
                longArrayListOf(-1, -3, -5).product() shouldBeEqualTo (-1 * -3 * -5).toDouble()
            },
        )
    }

    private object FloatDriver: TypeDriver {
        override val typeName: String = "Float"

        private val kotlinList = fastList(5) { it + 1.0F }
        private val kotlinSet = kotlinList.toUnifiedSet()
        private val expectedArray = floatArrayOf(1.0F, 2.0F, 3.0F, 4.0F, 5.0F)
        private val expectedArrayList = floatArrayListOf(1.0F, 2.0F, 3.0F, 4.0F, 5.0F)

        override fun tests(): List<DynamicTest> = listOf(
            dynamicTest("$typeName: kotlin array to eclipse array") {
                expectedArray.toFloatArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: sequence to primitive array list") {
                kotlinList.toFloatArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: iterable to primitive array list") {
                kotlinList.toFloatArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: stream to primitive array list") {
                val array = IntStream.range(1, 6).asDoubleStream().toList().asFloatArrayList()
                array shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: convert primitive array list") {
                val array = floatArrayList(5) { it + 1.0F }
                array.size() shouldBeEqualTo 5
                array shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: primitive list asList") {
                val list = expectedArrayList.asList()
                list.size shouldBeEqualTo 5
                list shouldBeEqualTo kotlinList
            },
            dynamicTest("$typeName: primitive set asSet") {
                val set = floatArrayList(5) { it + 1.0F }.asSet()
                set.size shouldBeEqualTo 5
                set shouldBeEqualTo kotlinSet
            },
            dynamicTest("$typeName: primitive array list to list") {
                val expected = listOf(1.0F, 2.0F, 3.0F, 4.0F, 4.0F, 5.0F)
                val array = floatArrayListOf(1.0F, 2.0F, 3.0F, 4.0F, 4.0F, 5.0F)

                array.toArray() shouldBeEqualTo expected.toFloatArray()
                array.asIterable().toList() shouldBeEqualTo expected
                array.asSequence().toList() shouldBeEqualTo expected
                array.asIterator().toList() shouldBeEqualTo expected
                array.asList() shouldBeEqualTo expected
                array.asSet() shouldBeEqualTo expected.toSet()
                array.toFastList() shouldBeEqualTo expected
                array.toUnifiedSet() shouldBeEqualTo expected.toSet()
            },
        )
    }

    private object DoubleDriver: TypeDriver {
        override val typeName: String = "Double"

        private val kotlinList = fastList(5) { it + 1.0 }
        private val kotlinSet = kotlinList.toUnifiedSet()
        private val expectedArray = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        private val expectedArrayList = doubleArrayListOf(1.0, 2.0, 3.0, 4.0, 5.0)

        override fun tests(): List<DynamicTest> = listOf(
            dynamicTest("$typeName: kotlin array to eclipse array") {
                expectedArray.toDoubleArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: sequence to primitive array list") {
                kotlinList.toDoubleArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: iterable to primitive array list") {
                kotlinList.toDoubleArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: stream to primitive array list") {
                IntStream.range(1, 6).asDoubleStream().toDoubleArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: convert primitive array list") {
                val array = doubleArrayList(5) { it + 1.0 }
                array.size() shouldBeEqualTo 5
                array shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: primitive list asList") {
                val list = expectedArrayList.asList()
                list.size shouldBeEqualTo 5
                list shouldBeEqualTo kotlinList
            },
            dynamicTest("$typeName: primitive set asSet") {
                val set = doubleArrayListOf(1.0, 2.0, 2.0, 3.0, 3.0, 4.0, 5.0).asSet()
                set.size shouldBeEqualTo 5
                set shouldBeEqualTo kotlinSet
            },
            dynamicTest("$typeName: primitive array list to list") {
                val expected = listOf(1.0, 2.0, 3.0, 4.0, 4.0, 5.0)
                val array = doubleArrayListOf(1.0, 2.0, 3.0, 4.0, 4.0, 5.0)

                array.toArray() shouldBeEqualTo expected.toDoubleArray()
                array.asIterable().toList() shouldBeEqualTo expected
                array.asSequence().toList() shouldBeEqualTo expected
                array.asIterator().toList() shouldBeEqualTo expected
                array.asList() shouldBeEqualTo expected
                array.asSet() shouldBeEqualTo expected.toSet()
                array.toFastList() shouldBeEqualTo expected
                array.toUnifiedSet() shouldBeEqualTo expected.toSet()
            },
        )
    }

    private object ByteDriver: TypeDriver {
        override val typeName: String = "Byte"

        private val kotlinList = fastList(5) { (it + 1).toByte() }
        private val kotlinSet = kotlinList.toUnifiedSet()
        private val expectedArray = byteArrayOf(1, 2, 3, 4, 5)
        private val expectedArrayList = byteArrayListOf(1, 2, 3, 4, 5)

        override fun tests(): List<DynamicTest> = listOf(
            dynamicTest("$typeName: kotlin array to eclipse array") {
                expectedArray.toByteArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: sequence to primitive array list") {
                kotlinList.take(5).toByteArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: iterable to primitive array list") {
                kotlinList.toByteArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: stream to primitive array list") {
                IntStream.range(1, 6).toList().asByteArrayList() shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: convert primitive array list") {
                val array = byteArrayList(5) { (it + 1).toByte() }
                array.size() shouldBeEqualTo 5
                array shouldBeEqualTo expectedArrayList
            },
            dynamicTest("$typeName: primitive list asList") {
                val list = byteArrayListOf(1, 2, 3, 4, 5).asList()
                list.size shouldBeEqualTo 5
                list shouldBeEqualTo kotlinList
            },
            dynamicTest("$typeName: primitive set asSet") {
                val set = byteArrayListOf(1, 2, 2, 3, 3, 4, 5).asSet()
                set.size shouldBeEqualTo 5
                set shouldBeEqualTo kotlinSet
            },
            dynamicTest("$typeName: primitive array list to list") {
                val array = byteArrayListOf(1, 2, 3, 4, 4, 5)
                val expected = array.asList()

                array.toArray() shouldBeEqualTo expected.toByteArray()
                array.asIterable().toList() shouldBeEqualTo expected
                array.asSequence().toList() shouldBeEqualTo expected
                array.asIterator().toList() shouldBeEqualTo expected
                array.asList() shouldBeEqualTo expected
                array.asSet() shouldBeEqualTo expected.toSet()
                array.toFastList() shouldBeEqualTo expected
                array.toUnifiedSet() shouldBeEqualTo expected.toSet()
            },
            dynamicTest("$typeName: get product") {
                byteArrayListOf(1, 3, 5).product() shouldBeEqualTo (1 * 3 * 5).toDouble()
                byteArrayListOf(-1, -3, -5).product() shouldBeEqualTo (-1 * -3 * -5).toDouble()
            },
        )
    }

    private val drivers: List<TypeDriver> = listOf(
        IntDriver,
        LongDriver,
        FloatDriver,
        DoubleDriver,
        ByteDriver,
    )

    @TestFactory
    fun `primitive numeric ArrayList extension suite`(): List<DynamicTest> =
        drivers.flatMap { it.tests() }
}
