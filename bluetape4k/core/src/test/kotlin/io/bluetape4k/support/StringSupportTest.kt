package io.bluetape4k.support

import io.bluetape4k.AbstractCoreTest
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEndWith
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.text.dropLast
import kotlin.text.takeLast

@RandomizedTest
class StringSupportTest: AbstractCoreTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    val nullValue: String? = null
    val emptyValue: String = EMPTY_STRING
    val blankValue: String = " \t "
    val someValue: String = "debop"
    val fallback: String = "fallback"

    @Test
    fun `is empty `() {
        nullValue.isNullOrEmpty().shouldBeTrue()

        emptyValue.isEmpty().shouldBeTrue()
        emptyValue.isNotEmpty().shouldBeFalse()

        blankValue.isEmpty().shouldBeFalse()
        blankValue.isNotEmpty().shouldBeTrue()

        someValue.isEmpty().shouldBeFalse()
        someValue.isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `is whitespace`() {
        nullValue.isWhitespace().shouldBeTrue()
        nullValue.isNotWhitespace().shouldBeFalse()

        emptyValue.isWhitespace().shouldBeTrue()
        emptyValue.isNotWhitespace().shouldBeFalse()

        blankValue.isWhitespace().shouldBeTrue()
        blankValue.isNotWhitespace().shouldBeFalse()

        someValue.isWhitespace().shouldBeFalse()
        someValue.isNotWhitespace().shouldBeTrue()
    }

    @Test
    fun `has length`() {
        nullValue.hasLength().shouldBeFalse()
        emptyValue.hasText().shouldBeFalse()

        blankValue.hasLength().shouldBeTrue()
        someValue.hasLength().shouldBeTrue()
    }

    @Test
    fun `has text`() {
        nullValue.hasText().shouldBeFalse()
        nullValue.noText().shouldBeTrue()

        emptyValue.hasText().shouldBeFalse()
        emptyValue.noText().shouldBeTrue()

        blankValue.hasText().shouldBeFalse()
        blankValue.noText().shouldBeTrue()

        someValue.hasText().shouldBeTrue()
        someValue.noText().shouldBeFalse()
    }

    @Test
    fun `as null if empty string`() {
        nullValue.asNullIfEmpty().shouldBeNull()
        emptyValue.asNullIfEmpty().shouldBeNull()
        blankValue.asNullIfEmpty().shouldNotBeNull()
        someValue.asNullIfEmpty().shouldNotBeNull()
    }

    @Test
    fun `as null if blank string`() {
        nullValue.asNullIfBlank().shouldBeNull()
        emptyValue.asNullIfBlank().shouldBeNull()
        blankValue.asNullIfBlank().shouldBeNull()
        someValue.asNullIfBlank().shouldNotBeNull()
    }

    @Test
    fun `convert string to utf8 byte array and back`() {
        emptyValue.toUtf8Bytes().toUtf8String() shouldBeEqualTo emptyValue
        blankValue.toUtf8Bytes().toUtf8String() shouldBeEqualTo blankValue

        repeat(REPEAT_SIZE) {
            val origin = Fakers.randomString(0, 1024)

            val bytes = origin.toUtf8Bytes().shouldNotBeEmpty()
            val actual = bytes.toUtf8String()

            actual shouldBeEqualTo origin
        }
    }

    @Test
    fun `convert string to utf8 byte buffer and back`() {
        emptyValue.toUtf8ByteBuffer().toUtf8String() shouldBeEqualTo emptyValue
        blankValue.toUtf8ByteBuffer().toUtf8String() shouldBeEqualTo blankValue

        repeat(REPEAT_SIZE) {
            val origin = Fakers.randomString(0, 1024)

            val byteBuffer = origin.toUtf8ByteBuffer()
            val actual = byteBuffer.toUtf8String()

            actual shouldBeEqualTo origin
        }
    }

    @Test
    fun `if string is empty return fallback`() {
        nullValue.ifNullOrEmpty { fallback } shouldBeEqualTo fallback
        emptyValue.ifNullOrEmpty { fallback } shouldBeEqualTo fallback

        blankValue.ifNullOrEmpty { fallback } shouldBeEqualTo blankValue
        someValue.ifNullOrEmpty { fallback } shouldBeEqualTo someValue
    }

    @Test
    fun `if string is blank return fallback`() {
        nullValue.ifNullOrBlank { fallback } shouldBeEqualTo fallback
        emptyValue.ifNullOrBlank { fallback } shouldBeEqualTo fallback

        blankValue.ifNullOrBlank { fallback } shouldBeEqualTo fallback
        someValue.ifNullOrBlank { fallback } shouldBeEqualTo someValue
    }

    @Test
    fun `trim whitespace`() {
        blankValue.trimWhitespace().shouldBeEmpty()
        someValue.trimWhitespace() shouldBeEqualTo someValue

        " \t a \t ".trimWhitespace() shouldBeEqualTo "a"
    }

    @Test
    fun `trim start whitespace`() {
        blankValue.trimStartWhitespace().shouldBeEmpty()
        someValue.trimStartWhitespace() shouldBeEqualTo someValue

        " \t a \t ".trimStartWhitespace() shouldBeEqualTo "a \t "
    }

    @Test
    fun `trim end whitespace`() {
        blankValue.trimEndWhitespace().shouldBeEmpty()
        someValue.trimEndWhitespace() shouldBeEqualTo someValue

        " \t a \t ".trimEndWhitespace() shouldBeEqualTo " \t a"
    }

    @Test
    fun `trim all whitespace`() {
        blankValue.trimAllWhitespace().shouldBeEmpty()
        someValue.trimAllWhitespace() shouldBeEqualTo someValue

        " a b\tc\t d".trimAllWhitespace() shouldBeEqualTo "abcd"
    }

    @Test
    fun `quote string`() {
        nullValue.quoted() shouldBeEqualTo "null"
        emptyValue.quoted() shouldBeEqualTo """''"""
        blankValue.quoted() shouldBeEqualTo """'$blankValue'"""
        someValue.quoted() shouldBeEqualTo "'$someValue'"

        "debop's book".quoted() shouldBeEqualTo """'debop''s book'"""
        """''""".quoted() shouldBeEqualTo """''''''"""
        """'abc'""".quoted() shouldBeEqualTo """'''abc'''"""
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `string ellipsis`() {
        val origin = Fakers.fixedString(1024).replicate(10)
        val length = origin.length

        origin.needEllipsis(length - 5).shouldBeTrue()
        origin.needEllipsis(length).shouldBeFalse()

        origin.ellipsisEnd(length - 5) shouldEndWith "..."
        origin.ellipsisEnd(length) shouldBeEqualTo origin

        origin.ellipsisStart(length - 5) shouldStartWith "..."
        origin.ellipsisStart(length) shouldBeEqualTo origin

        origin.ellipsisMid(length - 5) shouldContain "..."
        origin.ellipsisMid(length) shouldBeEqualTo origin
    }

    @Test
    fun `delete characters`() {
        val origin = "a.b.c.d/e.f"

        origin.deleteChars('.') shouldBeEqualTo "abcd/ef"
        origin.deleteChars('.', '/') shouldBeEqualTo "abcdef"
    }

    @Test
    fun `iterable map as string`() {
        emptyList<Any>().mapAsString() shouldBeEqualTo emptyList()
        listOf("1", 1, "2").mapAsString() shouldBeEqualTo listOf("1", "1", "2")
    }

    @Test
    fun `sequence map as string`() {
        emptySequence<Any>().mapAsString().toList() shouldBeEqualTo emptyList()
        sequenceOf("1", 1, "2").mapAsString().toList() shouldBeEqualTo listOf("1", "1", "2")
    }

    @Test
    fun `replicate string`() {
        nullValue.replicate(1) shouldBeEqualTo EMPTY_STRING
        emptyValue.replicate(10) shouldBeEqualTo emptyValue
        "a".replicate(5) shouldBeEqualTo "a".repeat(5)
        "a1".replicate(3) shouldBeEqualTo "a1".repeat(3)
    }

    @Test
    fun `get word count`() {
        nullValue.wordCount("a") shouldBeEqualTo 0
        emptyValue.wordCount("a") shouldBeEqualTo 0
        blankValue.wordCount(EMPTY_STRING) shouldBeEqualTo 0
        someValue.wordCount(EMPTY_STRING) shouldBeEqualTo 0

        "\t  \t  \t".wordCount("\t") shouldBeEqualTo 3

        "debop is developer and architecture".wordCount("developer") shouldBeEqualTo 1
        "debop is developer and architecture, anyone can be developer.".wordCount("developer") shouldBeEqualTo 2
    }

    @Test
    fun `get first line for empty string list`() {
        val strs = emptyList<String>()
        val lines = strs.joinToString(LINE_SEPARATOR)

        lines.firstLine() shouldBeEqualTo EMPTY_STRING
    }

    @Test
    fun `get first line`() {
        val strs = List(5) { faker.lorem().sentence() }
        val lines = strs.joinToString(LINE_SEPARATOR)
        lines.firstLine() shouldBeEqualTo strs[0]

        lines.firstLine(EMPTY_STRING) shouldBeEqualTo lines
    }

    @Test
    fun `between string`() {
        val origin = "debop is developer and architect"

        origin.between("developer", "architect") shouldBeEqualTo " and "
        origin.between("debop", "developer") shouldBeEqualTo " is "

        origin.between("eb", "p is") shouldBeEqualTo "o"

        // null, empty string 은 empty string 을 반환한다
        nullValue.between("d", "p") shouldBeEqualTo EMPTY_STRING
        emptyValue.between("d", "p") shouldBeEqualTo EMPTY_STRING

        origin.between("", "a") shouldBeEqualTo EMPTY_STRING
        origin.between("a", "") shouldBeEqualTo EMPTY_STRING

        // start == end 이면, EMPTY_STRING을 반환한다.
        origin.between("developer", "developer") shouldBeEqualTo EMPTY_STRING
    }

    @Test
    fun `drop first charactors`() {
        emptyValue.dropFirst(0) shouldBeEqualTo EMPTY_STRING
        emptyValue.dropFirst(3) shouldBeEqualTo EMPTY_STRING
        someValue.dropFirst(0) shouldBeEqualTo someValue

        someValue.dropFirst(2) shouldBeEqualTo "bop"

        someValue.dropFirst(someValue.length) shouldBeEqualTo EMPTY_STRING
    }

    @Test
    fun `drop last charactors`() {
        emptyValue.dropLast(0) shouldBeEqualTo EMPTY_STRING
        emptyValue.dropLast(3) shouldBeEqualTo EMPTY_STRING
        someValue.dropLast(0) shouldBeEqualTo someValue

        someValue.dropLast(2) shouldBeEqualTo "deb"

        someValue.dropLast(someValue.length) shouldBeEqualTo EMPTY_STRING
    }

    @Test
    fun `take first charactors`() {
        emptyValue.takeFirst(0) shouldBeEqualTo EMPTY_STRING
        emptyValue.takeFirst(3) shouldBeEqualTo EMPTY_STRING

        someValue.takeFirst(0) shouldBeEqualTo EMPTY_STRING
        someValue.takeFirst(2) shouldBeEqualTo "de"

        someValue.takeFirst(someValue.length) shouldBeEqualTo someValue
    }

    @Test
    fun `take last charactors`() {
        emptyValue.takeLast(0) shouldBeEqualTo EMPTY_STRING
        emptyValue.takeLast(3) shouldBeEqualTo EMPTY_STRING

        someValue.takeLast(0) shouldBeEqualTo EMPTY_STRING
        someValue.takeLast(2) shouldBeEqualTo "op"

        someValue.takeLast(someValue.length) shouldBeEqualTo someValue
    }

    @Test
    fun `add prefix if absent`() {
        val prefix = "bluetape4k."
        val expected = "bluetape4k.version"

        // version 앞에 LibraryName 을 붙인다.
        "version".prefixIfAbsent(prefix, true) shouldBeEqualTo expected
        "version".prefixIfAbsent(prefix, false) shouldBeEqualTo expected

        // 대소문자 구분 없을 때에는 prefix 가 존재한다.
        expected.prefixIfAbsent(prefix.uppercase(), true) shouldBeEqualTo expected

        // 대소문자 구분 있을 때에는 prefix.uppercase() 가 존재하지 않으므로 prepend 된다.
        expected.prefixIfAbsent(prefix.uppercase(), false) shouldNotBeEqualTo expected
    }

    @Test
    fun `add suffix if absent`() {
        val suffix = ".patch"
        val expected = "version.patch"

        "version".suffixIfAbsent(suffix, true) shouldBeEqualTo expected
        "version".suffixIfAbsent(suffix, false) shouldBeEqualTo expected

        expected.suffixIfAbsent(suffix.uppercase(), true) shouldBeEqualTo expected
        expected.suffixIfAbsent(suffix.uppercase(), false) shouldNotBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get unique characters`() {
        "abc".uniqueChars() shouldBeEqualTo "abc"
        "abcabccbaz".uniqueChars() shouldBeEqualTo "abcz"

        val str = Fakers.randomString(16, 32)
        val duplicated = str.repeat(3)

        val uniques = duplicated.uniqueChars()
        uniques.length shouldBeLessOrEqualTo str.length
        uniques.toSet().toCharArray() shouldBeEqualTo uniques.toCharArray()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `mask string for security`() {
        val str = Fakers.randomString(16, 32)

        val expected = buildString {
            repeat(str.length) {
                append("*")
            }
        }

        str.mask() shouldBeEqualTo expected

        str.mask('#') shouldBeEqualTo buildString {
            repeat(str.length) {
                append("#")
            }
        }
    }

    @Test
    fun `구분자로 구분된 문자열을 camel case 문자열로 변환`() {
        "server-id".toCamelcase() shouldBeEqualTo "serverId"
        "server-host-name".toCamelcase() shouldBeEqualTo "serverHostName"
        "Server-Name".toCamelcase() shouldBeEqualTo "serverName"
        "".toCamelcase() shouldBeEqualTo ""
        "바보-온달".toCamelcase() shouldBeEqualTo "바보온달"
    }

    @Test
    fun `camel case 를 구분자로 구분되는 문자열로 변환`() {
        "serverId".toDashedString() shouldBeEqualTo "server-id"
        "serverHostName".toDashedString() shouldBeEqualTo "server-host-name"
        "".toDashedString() shouldBeEqualTo ""
    }

    @Test
    fun `문자열 앞에 pad char 를 추가합니다`() {
        "007".padStart(3, 'X') shouldBeEqualTo "007"
        "2010".padStart(3, '0') shouldBeEqualTo "2010"

        "7".padStart(3, '0') shouldBeEqualTo "007"
        "09".padStart(3, '0') shouldBeEqualTo "009"
        "09".padStart(5, 'X') shouldBeEqualTo "XXX09"
    }

    @Test
    fun `문자열 뒤에 pad char 를 추가합니다`() {
        "4.".padEnd(3, '0') shouldBeEqualTo "4.0"
        "4.".padEnd(5, '0') shouldBeEqualTo "4.000"

        "2010".padEnd(3, '!') shouldBeEqualTo "2010"
        "2010".padEnd(5, '!') shouldBeEqualTo "2010!"
    }

    @Test
    fun `두 문자열의 공통된 prefix를 찾습니다`() {
        commonPrefix(someValue, someValue) shouldBeEqualTo someValue
        commonPrefix("debop", "de") shouldBeEqualTo "de"
        commonPrefix("debop", "bluetape4k") shouldBeEqualTo EMPTY_STRING

        someValue.commonPrefix(someValue) shouldBeEqualTo someValue
        "debop".commonPrefix("de") shouldBeEqualTo "de"
        "debop".commonPrefix("bluetape4k") shouldBeEqualTo EMPTY_STRING
    }

    @Test
    fun `두 문자열의 공통된 suffix 찾습니다`() {
        commonSuffix(someValue, someValue) shouldBeEqualTo someValue
        commonSuffix("debop", "op") shouldBeEqualTo "op"
        commonSuffix("debop", "bluetape4k") shouldBeEqualTo EMPTY_STRING

        someValue.commonSuffix(someValue) shouldBeEqualTo someValue
        "debop".commonSuffix("op") shouldBeEqualTo "op"
        "debop".commonSuffix("bluetape4k") shouldBeEqualTo EMPTY_STRING
    }

    @Test
    fun `두 문자열을 대소문자 구분없이 비교`() {
        nullValue.equalsIgnoreCase(nullValue).shouldBeTrue()

        emptyValue.equalsIgnoreCase(emptyValue).shouldBeTrue()
        emptyValue.equalsIgnoreCase(emptyValue.uppercase()).shouldBeTrue()
        emptyValue.equalsIgnoreCase(emptyValue.lowercase()).shouldBeTrue()

        blankValue.equalsIgnoreCase(blankValue).shouldBeTrue()
        blankValue.equalsIgnoreCase(blankValue.uppercase()).shouldBeTrue()
        blankValue.equalsIgnoreCase(blankValue.lowercase()).shouldBeTrue()

        someValue.equalsIgnoreCase(someValue).shouldBeTrue()
        someValue.equalsIgnoreCase(someValue.uppercase()).shouldBeTrue()
        someValue.equalsIgnoreCase(someValue.lowercase()).shouldBeTrue()

        "abc".equalsIgnoreCase(EMPTY_STRING).shouldBeFalse()
        emptyValue.equalsIgnoreCase("abc").shouldBeFalse()
    }
}
