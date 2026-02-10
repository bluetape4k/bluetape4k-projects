package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.*

@Suppress(
    "DEPRECATION",
    "SENSELESS_COMPARISON",
    "SimplifyBooleanWithConstants",
    "ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS"
)
class AnySupportTest {

    companion object: KLogging()

    enum class TestEnum { FIRST, SECOND }

    @Test
    fun `Any 를 Optional로 변환하기`() {
        null.toOptional() shouldBeEqualTo Optional.empty()
    }


    @Test
    fun `두 값 비교하기`() {
        (null == null).shouldBeTrue()
        (null == "").shouldBeFalse()
        ("" == "").shouldBeTrue()
        ("a" == "a").shouldBeTrue()
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
        areEquals(emptyIntArray, emptyIntArray).shouldBeTrue()
        areEquals(byteArrayOf(1), byteArrayOf(1)).shouldBeFalse()
    }

    @Test
    fun `두 array 비교하기`() {
        arrayEquals(byteArrayOf(1), byteArrayOf(1)).shouldBeTrue()
        arrayEquals(byteArrayOf(1), byteArrayOf(2)).shouldBeFalse()
        arrayEquals(byteArrayOf(1), byteArrayOf(1, 2)).shouldBeFalse()
        arrayEquals(byteArrayOf(1, 2), byteArrayOf(1, 2)).shouldBeTrue()

        arrayEquals(arrayOf("1"), arrayOf("1")).shouldBeTrue()
        arrayEquals(arrayOf("1"), arrayOf("2")).shouldBeFalse()
        arrayEquals(arrayOf("1"), arrayOf("1", "2")).shouldBeFalse()
        arrayEquals(arrayOf("1", "2"), arrayOf("1", "2")).shouldBeTrue()
    }


    @Test
    fun `hashCodeSafe 함수`() {
        null.hashCodeSafe() shouldBeEqualTo 0
        "test".hashCodeSafe() shouldBeEqualTo "test".hashCode()
    }

    @Test
    fun `array의 content를 이용하는 hashCode 계산`() {
        val array = intArrayOf(1, 2, 3)
        array.hashCodeSafe() shouldBeEqualTo array.contentHashCode()

        arrayOf("a", "b", "c").hashCodeSafe() shouldBeEqualTo arrayOf("a", "b", "c").contentHashCode()
    }

    @Test
    fun `다양한 수형에 대한 toStr() 함수`() {
        // null
        null.toStr() shouldBeEqualTo "null"

        // primitive & String
        1.toStr() shouldBeEqualTo "1"
        true.toStr() shouldBeEqualTo "true"
        "hello".toStr() shouldBeEqualTo "hello"

        // primitive arrays
        intArrayOf(1, 2, 3).toStr() shouldBeEqualTo "[1, 2, 3]"
        byteArrayOf(1, 2).toStr() shouldBeEqualTo "[1, 2]"

        // object arrays
        arrayOf("a", "b").toStr() shouldBeEqualTo "[a, b]"
        arrayOf(arrayOf(1, 2), arrayOf(3)).toStr() shouldBeEqualTo "[[1, 2], [3]]"

        // collections
        listOf(1, 2, 3).toStr() shouldBeEqualTo "[1, 2, 3]"
        setOf("a", "b").toStr() shouldBeEqualTo "[a, b]"

        // map
        mapOf("a" to 1, "b" to 2).toStr() shouldBeEqualTo "{a=1, b=2}"

        // pair / triple
        Pair(1, "a").toStr() shouldBeEqualTo "(1, a)"
        Triple(1, 2, 3).toStr() shouldBeEqualTo "(1, 2, 3)"

        // enum
        TestEnum.FIRST.toStr() shouldBeEqualTo "FIRST"

        // optional
        Optional.of("x").toStr() shouldBeEqualTo "Optional[x]"
        Optional.empty<String>().toStr() shouldBeEqualTo "Optional.empty"
    }

    @Test
    fun `unwrapOptional 동작 테스트`() {
        "a".unwrapOptional() shouldBeEqualTo "a"
        Optional.of("x").unwrapOptional() shouldBeEqualTo "x"
        Optional.empty<String>().unwrapOptional() shouldBeEqualTo null
    }

    @Test
    fun `isArray 확장 프로퍼티 테스트`() {
        arrayOf(1, 2, 3).isArray.shouldBeTrue()
        intArrayOf(1, 2).isArray.shouldBeTrue()
        "test".isArray.shouldBeFalse()
    }

    @Test
    fun `areEqualsSafe 비교 테스트`() {
        areEqualsSafe(null, null).shouldBeTrue()
        areEqualsSafe("a", "a").shouldBeTrue()
        areEqualsSafe("a", "b").shouldBeFalse()

        val a1 = arrayOf(1, 2)
        val a2 = arrayOf(1, 2)
        val a3 = arrayOf(2, 3)

        areEqualsSafe(a1, a2).shouldBeTrue()
        areEqualsSafe(a1, a3).shouldBeFalse()
    }

    @Test
    fun `identityToString 과 identityHexString 테스트`() {
        val obj = Any()
        obj.identityHexString().isNotBlank().shouldBeTrue()
        obj.identityToString().contains(obj.identityHexString()).shouldBeTrue()

        val x: Any? = null
        x.identityToString() shouldBeEqualTo ""
    }

    @Test
    fun `hashOf 함수 테스트`() {
        val a = "a"
        val b = 1
        hashOf(a, b) shouldBeEqualTo Objects.hash(a, b)
    }
}
