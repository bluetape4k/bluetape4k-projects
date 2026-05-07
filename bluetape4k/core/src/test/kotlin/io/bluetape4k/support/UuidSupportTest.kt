package io.bluetape4k.support

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.*

class UuidSupportTest {

    companion object {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert UUID vs BigInt`() {
        val expected = UUID.randomUUID()
        val bigInt = expected.toBigInt()
        val actual = bigInt.toUuid()

        actual shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert UUID vs LongArray`() {
        val expected = UUID.randomUUID()
        val array = expected.toLongArray()
        val actual = array.toUUID()

        actual shouldBeEqualTo expected
    }

    @Test
    fun `isUuid - check string is uuid`() {
        val str = UUID.randomUUID().toString()
        str.isUuid().shouldBeTrue()

        val str2 = "not-uuid"
        str2.isUuid().shouldBeFalse()
    }
}
