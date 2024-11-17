package io.bluetape4k.support

import io.bluetape4k.LibraryName
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldEndWith
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.text.dropLast
import kotlin.text.takeLast

@RandomizedTest
class StringSupportTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    val nullValue: String? = null
    val emptyValue: String = ""
    val blankValue: String = " \t "
    val someValue: String = "debop"

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

    @RepeatedTest(REPEAT_SIZE)
    fun `convert to utf8`() {
        val origin = Fakers.randomString(1024)

        val bytes = origin.toUtf8Bytes()
        bytes.shouldNotBeEmpty()

        val actual = bytes.toUtf8String()
        actual shouldBeEqualTo origin
    }

    @Test
    fun `trim whitespace`() {
        blankValue.trimWhitespace().shouldBeEmpty()
        someValue.trimWhitespace() shouldBeEqualTo someValue

        " \t a \t ".trimWhitespace() shouldBeEqualTo "a"
    }

    @Test
    fun `trimStart whitespace`() {
        blankValue.trimStartWhitespace().shouldBeEmpty()
        someValue.trimStartWhitespace() shouldBeEqualTo someValue

        " \t a \t ".trimStartWhitespace() shouldBeEqualTo "a \t "
    }

    @Test
    fun `trimEnd whitespace`() {
        blankValue.trimEndWhitespace().shouldBeEmpty()
        someValue.trimEndWhitespace() shouldBeEqualTo someValue

        " \t a \t ".trimEndWhitespace() shouldBeEqualTo " \t a"
    }

    @Test
    fun `remove all whitespace`() {
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
        val str = Fakers.fixedString(1024)
        val origin = str.replicate(10)
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
    fun `iterable as string list`() {
        listOf("1", 1, "2").asStringList() shouldContainSame listOf("1", "1", "2")
    }

    @Test
    fun `replicate string`() {
        emptyValue.replicate(10) shouldBeEqualTo emptyValue
        "a".replicate(5) shouldBeEqualTo "aaaaa"
        "a1".replicate(3) shouldBeEqualTo "a1a1a1"
    }

    @Test
    fun `get word count`() {
        "debop is developer and architecture".wordCount("developer") shouldBeEqualTo 1
        "debop is developer and architecture, anyone can be developer.".wordCount("developer") shouldBeEqualTo 2
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get first line`(@RandomValue(type = String::class, size = 5) strs: List<String>) {
        val lines = strs.joinToString(LINE_SEPARATOR)

        lines.firstLine() shouldBeEqualTo strs[0]
    }

    @Test
    fun `sub string with string`() {
        val origin = "debop is developer and architecture"

        origin.between("developer", "architecture") shouldBeEqualTo " and "
        origin.between("debop", "developer") shouldBeEqualTo " is "

        origin.between("eb", "p is") shouldBeEqualTo "o"
    }

    @Test
    fun `drop charactors`() {
        emptyValue.dropFirst(3) shouldBeEqualTo emptyValue
        emptyValue.dropLast(3) shouldBeEqualTo emptyValue

        someValue.dropFirst(2) shouldBeEqualTo "bop"
        someValue.dropLast(2) shouldBeEqualTo "deb"

        someValue.dropFirst(100) shouldBeEqualTo ""
        someValue.dropLast(100) shouldBeEqualTo ""
    }

    @Test
    fun `take charactors`() {
        emptyValue.takeFirst(3) shouldBeEqualTo emptyValue
        emptyValue.takeLast(3) shouldBeEqualTo emptyValue

        someValue.takeFirst(2) shouldBeEqualTo "de"
        someValue.takeLast(2) shouldBeEqualTo "op"

        someValue.takeFirst(100) shouldBeEqualTo someValue
        someValue.takeLast(100) shouldBeEqualTo someValue
    }

    @Test
    fun `add prefix if absent`() {
        val prefix = "$LibraryName."
        val expected = "$LibraryName.version"

        "version".prefixIfAbsent(prefix, true) shouldBeEqualTo expected
        "$LibraryName.version".prefixIfAbsent(prefix, true) shouldBeEqualTo expected
        "$LibraryName.version".prefixIfAbsent(prefix, false) shouldBeEqualTo expected
    }

    @Test
    fun `add suffix if absent`() {
        val suffix = ".read"
        val expected = "version.read"

        "version".suffixIfAbsent(suffix, true) shouldBeEqualTo expected
        "version.read".suffixIfAbsent(suffix, true) shouldBeEqualTo expected
        "version".suffixIfAbsent(suffix, false) shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get unique characters`() {
        val str = Fakers.randomString(16, 32)
        val duplicated = str.repeat(3)

        val uniques = duplicated.uniqueChars()
        uniques.length shouldBeLessOrEqualTo str.length
        uniques.toSet().size shouldBeEqualTo uniques.length
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `redact string for password`() {
        val str = Fakers.randomString(16, 32)
        val redacted = str.redact()

        redacted.length shouldBeEqualTo str.length
        redacted shouldContainAll listOf("*")

        str.redact("#") shouldContainAll listOf("#")
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
}
