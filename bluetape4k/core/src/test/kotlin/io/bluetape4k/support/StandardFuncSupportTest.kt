package io.bluetape4k.support

import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test

class StandardFuncSupportTest {


    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
    }

    @Test
    fun `run safeLet with null`() {
        val p1: String? = null
        val p2: String? = null

        safeLet(p1, p2) { _, _ ->
            fail("p1, p2 둘 다 null 이므로 실행되어서는 안됩니다")
        }

        safeLet(null, "b") { _, _ ->
            fail("p1 이 null 이므로 실행되어서는 안됩니다")
        }
        safeLet("a", null) { _, _ ->
            fail("p2 이 null 이므로 실행되어서는 안됩니다")
        }
    }

    @Test
    fun `run safeLet with not nulls`() {
        safeLet("a", "b") { a, b ->
            a + b
        } shouldBeEqualTo "ab"

        safeLet("a", "b", "c") { a, b, c ->
            a + b + c
        } shouldBeEqualTo "abc"

        val chars = List(10) { ('a' + it).toString() }
        safeLet(*chars.toTypedArray()) { list ->
            list.size
        } shouldBeEqualTo 10
    }

    @Test
    fun `when all not null`() {
        listOf(4, null, 3).whenAllNotNull {
            fail("호출되면 안됩니다.")
        }

        listOf(4, 5, 7).whenAllNotNull {
            it shouldContainSame listOf(4, 5, 7)
        }

        whenAllNotNull(null, 1, null) {
            fail("실행되면 안됩니다.")
        }

        listOf(null, 1, null).whenAllNotNull {
            fail("실행되면 안됩니다.")
        }

        var called = false
        whenAllNotNull(1, 2, 3) {
            called = true
        }
        called.shouldBeTrue()

        val strs = List(10) { Base58.randomString(8) }
        called = false
        strs.whenAllNotNull {
            called = true
        }
        called.shouldBeTrue()
    }

    @Test
    fun `when any not null`() {
        listOf(null, null).whenAnyNotNull {
            fail("둘 다 null 이므로 실행되면 안됩니다.")
        }

        var called = false
        listOf(null, 1, null).whenAnyNotNull {
            called = true
            it shouldBeEqualTo listOf(null, 1, null)
        }
        called.shouldBeTrue()
    }

    @Test
    fun `coalesce - find first not null element`() {
        val pairs = listOf(null to null, null to 1, 1 to null, 1 to 1)

        pairs.forEach { (p1, p2) ->
            coalesce(p1, p2) shouldBeEqualTo when {
                p1 != null -> p1
                p2 != null -> p2
                else       -> null
            }
        }

        coalesce("a", null, "c") shouldBeEqualTo "a"
        coalesce(null, "b", null) shouldBeEqualTo "b"
        coalesce("a", "b", null) shouldBeEqualTo "a"
        coalesce<Any>(null, null).shouldBeNull()
    }

    @Test
    fun `coalesce of iterable - find first not null element`() {
        listOf(null, 4, null, 3).coalesce() shouldBeEqualTo 4
        listOf(null, 1, null, 3).coalesce() shouldBeEqualTo 1

        listOf(1, 2, 3, 4).coalesce() shouldBeEqualTo 1
        listOf(1, 2, null).coalesce() shouldBeEqualTo 1
    }
}
