package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.*

class AnySupportTest {

    companion object: KLogging()

    @Test
    fun `Any 를 Optional로 변환하기`() {
        null.toOptional() shouldBeEqualTo Optional.empty()
    }

    @Test
    fun `두 값 비교하기`() {
        val a: String? = null
        val b: Int? = null
        areEquals(a, b).shouldBeTrue()

        areEquals(null, null).shouldBeTrue()
        areEquals(null, "").shouldBeFalse()
        areEquals("", "").shouldBeTrue()
        areEquals("a", "a").shouldBeTrue()
    }

    @Test
    fun `두 값 비교하기 with null safe`() {
        val a: String? = null
        val b: Int? = null
        areEquals(a, b).shouldBeTrue()

        areEquals(null, null).shouldBeTrue()
        areEquals(null, "").shouldBeFalse()
        areEquals("", "").shouldBeTrue()
        areEquals("a", "a").shouldBeTrue()

        // Array 도 비교할 수 있다
        areEquals(emptyByteArray, emptyIntArray).shouldBeFalse()
    }

    @Test
    fun `두 array 비교하기`() {
        arrayEquals(byteArrayOf(1), byteArrayOf(1)).shouldBeTrue()
        arrayEquals(byteArrayOf(1), byteArrayOf(2)).shouldBeFalse()
        arrayEquals(byteArrayOf(1), byteArrayOf(1, 2)).shouldBeFalse()
    }

    @Test
    fun `when not null`() {
        listOf(4, null, 3) whenAllNotNull { fail("호출되면 안됩니다.") }
        listOf(4, 5, 7) whenAllNotNull { it shouldContainSame listOf(4, 5, 7) }
    }
}
