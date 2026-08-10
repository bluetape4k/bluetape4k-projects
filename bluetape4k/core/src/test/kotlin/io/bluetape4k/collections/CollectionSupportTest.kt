package io.bluetape4k.collections

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.collections.eclipse.emptyFastList
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.padTo
import org.junit.jupiter.api.Test

class CollectionSupportTest {

    companion object: KLogging()

    private val emptyList = emptyFastList<Int>()
    private val tenList = fastList(10) { it + 1 }


    @Test
    fun `last or null`() {
        emptyList.lastOrNull().shouldBeNull()
        tenList.lastOrNull().shouldNotBeNull()
    }

    @Test
    fun `item prepend to list`() {
        val list = fastListOf<Int>()
        3.prependTo(list)
        list.size shouldBeEqualTo 1

        5.prependTo(list)
        list.size shouldBeEqualTo 2
        list shouldContainSame listOf(5, 3)
    }

    @Test
    fun `prepend item to list`() {
        val list = fastList(10) { it + 1 }

        list.prepend(-1)

        list.size shouldBeEqualTo 11
        list.first() shouldBeEqualTo -1
    }

    @Test
    fun `append item to list`() {
        val list = fastList(3) { it + 1 }
        list.append(4, 5)
        list shouldContainSame listOf(1, 2, 3, 4, 5)
    }

    @Test
    fun `swap list elements`() {
        val list = fastListOf(1, 2, 3)
        list.swap(0, 2)
        list shouldContainSame listOf(3, 2, 1)

        list.swap(1, 1)
        list shouldContainSame listOf(3, 2, 1)

        assertFailsWith<IllegalArgumentException> { list.swap(-1, 1) }
        assertFailsWith<IllegalArgumentException> { list.swap(0, 3) }
    }

    @Test
    fun `specific value pad to collection`() {
        val origin = fastList(10) { it + 1 }

        origin.padTo(10, 5) shouldBeEqualTo origin
        origin.padTo(0, 5) shouldBeEqualTo origin

        val list = origin.padTo(100, -1)
        list.size shouldBeEqualTo 100
        list.filter { it == -1 }.size shouldBeEqualTo 90
    }

    @Test
    fun `specific value pad to array`() {
        val origin = Array(10) { it + 1 }

        origin.padTo(10, 11) shouldBeEqualTo origin
        origin.padTo(0, 11) shouldBeEqualTo origin

        val array = origin.padTo(100, -1)
        array.size shouldBeEqualTo 100
        array.filter { it == -1 }.size shouldBeEqualTo 90
    }

    @Test
    fun `each count of list`() {
        val list = fastListOf(1, 2, 2, 3)
        val map = list.eachCount()
        map shouldBeEqualTo mapOf(1 to 1, 2 to 2, 3 to 1)
        map.keys.toList() shouldBeEqualTo listOf(1, 2, 3)
        emptyList<Int>().eachCount() shouldBeEqualTo emptyMap()
    }

    @Test
    fun `split iterable into chunks when predicate matches`() {
        listOf(1, 2, 3, 4, 5).chunkedBy { it % 3 == 0 } shouldBeEqualTo
                listOf(listOf(1, 2), listOf(3, 4, 5))
        listOf(3, 4).chunkedBy { it % 3 == 0 } shouldBeEqualTo listOf(listOf(3, 4))
        emptyList<Int>().chunkedBy { true }.shouldBeEmpty()
    }

    @Test
    fun `safely take a sub list within clamped bounds`() {
        val origin = listOf(1, 2, 3, 4, 5)

        origin.safeSubList(-1, 100) shouldBeEqualTo origin
        origin.safeSubList(1, 3) shouldBeEqualTo listOf(2, 3)
        origin.safeSubList(4, 2).shouldBeEmpty()
        emptyList<Int>().safeSubList(-1, 1).shouldBeEmpty()
    }

    @Test
    fun `zip iterable elements with their indexes`() {
        listOf("a", "b").zipWithIndex() shouldBeEqualTo listOf(
            IndexedValue(0, "a"),
            IndexedValue(1, "b"),
        )
        emptyList<String>().zipWithIndex().shouldBeEmpty()
    }
}
