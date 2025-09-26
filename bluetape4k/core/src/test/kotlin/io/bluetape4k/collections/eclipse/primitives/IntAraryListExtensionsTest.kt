package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.collections.eclipse.stream.toIntArrayList
import io.bluetape4k.collections.toList
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.stream.IntStream

class IntAraryListExtensionsTest: AbstractCollectionTest() {

    val kotlinList = List(5) { it + 1 }
    val kotlinSet = kotlinList.toSet()
    val expectedArray = intArrayOf(1, 2, 3, 4, 5)
    val expectedArrayList = intArrayListOf(1, 2, 3, 4, 5)

    @Test
    fun `kotlin array to eclopse array`() {
        val kotlinArray = expectedArray
        val eclipseArray = expectedArrayList

        kotlinArray.toIntArrayList() shouldBeEqualTo eclipseArray
    }

    @Test
    fun `sequence to primitive array list`() {
        val array = kotlinList.take(5).toIntArrayList()
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `iterable to primitive array list`() {
        val array = kotlinList.toIntArrayList()
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `stream to primitive array list`() {
        val array = IntStream.range(1, 6).toIntArrayList()
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `convert primitive array list`() {
        val array = intArrayList(5) { it + 1 }
        array.size() shouldBeEqualTo 5
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `primitive list asList`() {
        val list = intArrayListOf(1, 2, 3, 4, 5).asList()
        list.size shouldBeEqualTo 5
        list shouldBeEqualTo kotlinList
    }

    @Test
    fun `primitive set asSet`() {
        val set = intArrayListOf(1, 2, 2, 3, 3, 4, 5).asSet()
        set.size shouldBeEqualTo 5
        set shouldBeEqualTo kotlinSet
    }

    @Test
    fun `primitive array list to list`() {

        val expected = listOf(1, 2, 3, 4, 4, 5)
        val array = intArrayListOf(1, 2, 3, 4, 4, 5)

        array.toArray() shouldBeEqualTo expected.toIntArray()

        array.asIterable().toList() shouldBeEqualTo expected
        array.asSequence().toList() shouldBeEqualTo expected

        array.asIterator().toList() shouldBeEqualTo expected

        array.asList() shouldBeEqualTo expected
        array.asSet() shouldBeEqualTo expected.toSet()
    }

    @Test
    fun `get product`() {
        intArrayListOf(1, 3, 5).product() shouldBeEqualTo (1 * 3 * 5).toDouble()
        intArrayListOf(-1, -3, -5).product() shouldBeEqualTo (-1 * -3 * -5).toDouble()
    }
}
