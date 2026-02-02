package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.collections.toList
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.stream.IntStream

class FloatArrayListExtensionsTest: AbstractCollectionTest() {

    companion object: KLogging()

    private val kotlinList = fastList(5) { it + 1.0F }
    private val kotlinSet = kotlinList.toUnifiedSet()
    private val expectedArray = floatArrayOf(1.0F, 2.0F, 3.0F, 4.0F, 5.0F)
    private val expectedArrayList = floatArrayListOf(1.0F, 2.0F, 3.0F, 4.0F, 5.0F)

    @Test
    fun `kotlin array to eclopse array`() {
        val kotlinArray = expectedArray
        val eclipseArray = expectedArrayList

        kotlinArray.toFloatArrayList() shouldBeEqualTo eclipseArray
    }

    @Test
    fun `sequence to primitive array list`() {
        val array = kotlinList.toFloatArrayList()
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `iterable to primitive array list`() {
        val array = kotlinList.toFloatArrayList()
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `stream to primitive array list`() {
        val array = IntStream.range(1, 6).asDoubleStream().toList().asFloatArrayList()
        array shouldBeEqualTo expectedArrayList
    }

    @Test
    fun `convert primitive array list`() {
        val array = floatArrayList(5) { it + 1.0F }
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
        val set = floatArrayList(5) { it + 1.0F }.asSet()
        set.size shouldBeEqualTo 5
        set shouldBeEqualTo kotlinSet
    }

    @Test
    fun `primitive array list to list`() {

        val expected = listOf(1.0F, 2.0F, 3.0F, 4.0F, 4.0F, 5.0F)
        val array = floatArrayListOf(1.0F, 2.0F, 3.0F, 4.0F, 4.0F, 5.0F)

        array.toArray() shouldBeEqualTo expected.toFloatArray()

        array.asIterable().toList() shouldBeEqualTo expected
        array.asSequence().toList() shouldBeEqualTo expected

        array.asIterator().toList() shouldBeEqualTo expected

        array.asList() shouldBeEqualTo expected
        array.asSet() shouldBeEqualTo expected.toSet()
    }
}
