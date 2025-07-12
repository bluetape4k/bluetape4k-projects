package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.collections.eclipse.stream.toDoubleArrayList
import io.bluetape4k.collections.toList
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.stream.IntStream

class DoubleArrayListExtensionsTest: AbstractCollectionTest() {

    companion object: KLogging()

    private val kotlinList = List(5) { it + 1.0 }
    private val kotlinSet = kotlinList.toSet()
    private val expectedArray = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
    private val expectedArrayList = doubleArrayListOf(1.0, 2.0, 3.0, 4.0, 5.0)

    @Test
    fun `kotlin array to eclopse array`() {
        val kotlinArray = expectedArray
        val eclipseArray = expectedArrayList

        kotlinArray.toDoubleArrayList() shouldBeEqualTo eclipseArray
    }

    @Test
    fun `sequence to primitive array list`() {
        val array = kotlinList.toDoubleArrayList()
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `iterable to primitive array list`() {
        val array = kotlinList.toDoubleArrayList()
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `stream to primitive array list`() {
        val array = IntStream.range(1, 6).asDoubleStream().toDoubleArrayList()
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `convert primitive array list`() {
        val array = doubleArrayList(5) { it + 1.0 }
        array.size() shouldBeEqualTo 5
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `primitive list asList`() {
        val list = expectedArrayList.asList()
        list.size shouldBeEqualTo 5
        list shouldBeEqualTo kotlinList
    }

    @Test
    fun `primitive set asSet`() {
        val set = doubleArrayListOf(1.0, 2.0, 2.0, 3.0, 3.0, 4.0, 5.0).asSet()
        set.size shouldBeEqualTo 5
        set shouldBeEqualTo kotlinSet
    }

    @Test
    fun `primitive array list to list`() {

        val expected = listOf(1.0, 2.0, 3.0, 4.0, 4.0, 5.0)
        val array = doubleArrayListOf(1.0, 2.0, 3.0, 4.0, 4.0, 5.0)

        array.toArray() shouldBeEqualTo expected.toDoubleArray()

        array.asIterable().toList() shouldBeEqualTo expected
        array.asSequence().toList() shouldBeEqualTo expected

        array.asIterator().toList() shouldBeEqualTo expected

        array.asList() shouldBeEqualTo expected
        array.asSet() shouldBeEqualTo expected.toSet()
    }
}
