package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.collections.toList
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.stream.IntStream

class ByteAraryListExtensionsTest: AbstractCollectionTest() {

    val kotlinList = fastList(5) { (it + 1).toByte() }
    val kotlinSet = kotlinList.toUnifiedSet()
    val expectedArray = byteArrayOf(1, 2, 3, 4, 5)
    val expectedArrayList = byteArrayListOf(1, 2, 3, 4, 5)

    @Test
    fun `kotlin array to eclopse array`() {
        val kotlinArray = expectedArray
        val eclipseArray = expectedArrayList

        kotlinArray.toByteArrayList() shouldBeEqualTo eclipseArray
    }

    @Test
    fun `sequence to primitive array list`() {
        val array = kotlinList.take(5).toByteArrayList()
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `iterable to primitive array list`() {
        val array = kotlinList.toByteArrayList()
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `stream to primitive array list`() {
        val array = IntStream.range(1, 6).toList().asByteArrayList()
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `convert primitive array list`() {
        val array = byteArrayList(5) { (it + 1).toByte() }
        array.size() shouldBeEqualTo 5
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `primitive list asList`() {
        val list = byteArrayListOf(1, 2, 3, 4, 5).asList()
        list.size shouldBeEqualTo 5
        list shouldBeEqualTo kotlinList
    }

    @Test
    fun `primitive set asSet`() {
        val set = byteArrayListOf(1, 2, 2, 3, 3, 4, 5).asSet()
        set.size shouldBeEqualTo 5
        set shouldBeEqualTo kotlinSet
    }

    @Test
    fun `primitive array list to list`() {

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
    }

    @Test
    fun `get product`() {
        byteArrayListOf(1, 3, 5).product() shouldBeEqualTo (1 * 3 * 5).toDouble()
        byteArrayListOf(-1, -3, -5).product() shouldBeEqualTo (-1 * -3 * -5).toDouble()
    }
}
