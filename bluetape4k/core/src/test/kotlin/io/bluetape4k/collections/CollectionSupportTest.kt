package io.bluetape4k.collections

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.padTo
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class CollectionSupportTest {

    companion object: KLogging()

    private val emptyList = emptyList<Int>()
    private val tenList = List(10) { it + 1 }


    @Test
    fun `last or null`() {
        emptyList.lastOrNull().shouldBeNull()
        tenList.lastOrNull().shouldNotBeNull()
    }

    @Test
    fun `item prepend to list`() {
        val list = mutableListOf<Int>()
        3.prependTo(list)
        list.size shouldBeEqualTo 1

        5.prependTo(list)
        list.size shouldBeEqualTo 2
        list shouldContainSame listOf(5, 3)
    }

    @Test
    fun `prepend item to list`() {
        val list = MutableList(10) { it + 1 }

        list.prepend(-1)

        list.size shouldBeEqualTo 11
        list.first() shouldBeEqualTo -1
    }

    @Test
    fun `specific value pad to collection`() {
        val origin = MutableList(10) { it + 1 }

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
        val list = listOf(1, 2, 2, 3)
        val map = list.eachCount()
        map shouldBeEqualTo mapOf(1 to 1, 2 to 2, 3 to 1)
    }
}
