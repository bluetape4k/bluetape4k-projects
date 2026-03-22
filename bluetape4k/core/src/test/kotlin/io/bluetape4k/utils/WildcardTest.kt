package io.bluetape4k.utils

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class WildcardTest {

    // ---------------------------------------------------------------- match

    @Test
    fun `정확한 문자열 매칭`() {
        Wildcard.match("hello", "hello").shouldBeTrue()
        Wildcard.match("hello", "world").shouldBeFalse()
        Wildcard.match("", "").shouldBeTrue()
    }

    @Test
    fun `별표 와일드카드로 여러 문자 매칭`() {
        Wildcard.match("hello", "*").shouldBeTrue()
        Wildcard.match("hello", "h*").shouldBeTrue()
        Wildcard.match("hello", "*o").shouldBeTrue()
        Wildcard.match("hello", "h*o").shouldBeTrue()
        Wildcard.match("hello", "h*l*o").shouldBeTrue()
        Wildcard.match("hello", "h*x").shouldBeFalse()
        Wildcard.match("", "*").shouldBeTrue()
    }

    @Test
    fun `물음표 와일드카드로 단일 문자 매칭`() {
        Wildcard.match("hello", "hell?").shouldBeTrue()
        Wildcard.match("hello", "h?llo").shouldBeTrue()
        Wildcard.match("hello", "?????").shouldBeTrue()
        Wildcard.match("hello", "????").shouldBeFalse()
        Wildcard.match("hello", "??????").shouldBeFalse()
    }

    @Test
    fun `이스케이프된 와일드카드는 리터럴로 매칭`() {
        Wildcard.match("h*llo", "h\\*llo").shouldBeTrue()
        Wildcard.match("hello", "h\\*llo").shouldBeFalse()
        Wildcard.match("h?llo", "h\\?llo").shouldBeTrue()
        Wildcard.match("hello", "h\\?llo").shouldBeFalse()
    }

    @Test
    fun `빈 문자열과 빈 패턴 매칭`() {
        Wildcard.match("", "").shouldBeTrue()
        Wildcard.match("hello", "").shouldBeFalse()
        Wildcard.match("", "hello").shouldBeFalse()
        Wildcard.match("", "*").shouldBeTrue()
        Wildcard.match("", "?").shouldBeFalse()
    }

    @Test
    fun `패턴 끝의 별표 매칭`() {
        Wildcard.match("hello world", "hello*").shouldBeTrue()
        Wildcard.match("hello", "hello*").shouldBeTrue()
        Wildcard.match("hell", "hello*").shouldBeFalse()
    }

    @Test
    fun `연속 별표는 단일 별표와 동일`() {
        Wildcard.match("hello", "**").shouldBeTrue()
        Wildcard.match("hello", "h**o").shouldBeTrue()
        Wildcard.match("hello", "***").shouldBeTrue()
    }

    @Test
    fun `equalsOrMatch 동작 확인`() {
        Wildcard.equalsOrMatch("hello", "hello").shouldBeTrue()
        Wildcard.equalsOrMatch("hello", "h*").shouldBeTrue()
        Wildcard.equalsOrMatch("hello", "world").shouldBeFalse()
    }

    // ---------------------------------------------------------------- matchPath

    @Test
    fun `깊은 트리 와일드카드 매칭`() {
        Wildcard.matchPath("src/main/kotlin/Test.kt", "**/Test.kt").shouldBeTrue()
        Wildcard.matchPath("src/main/kotlin/Test.kt", "src/**/*.kt").shouldBeTrue()
        Wildcard.matchPath("src/main/kotlin/Test.kt", "**/*.kt").shouldBeTrue()
        Wildcard.matchPath("Test.kt", "**/*.kt").shouldBeTrue()
    }

    @Test
    fun `슬래시 경로 구분자 매칭`() {
        Wildcard.matchPath("src/main/Test.kt", "src/main/Test.kt").shouldBeTrue()
        Wildcard.matchPath("src/main/Test.kt", "src/*/Test.kt").shouldBeTrue()
        Wildcard.matchPath("src/main/Test.kt", "src/main/*.kt").shouldBeTrue()
    }

    @Test
    fun `백슬래시 Windows 경로 구분자 매칭`() {
        Wildcard.matchPath("src\\main\\Test.kt", "src\\main\\Test.kt").shouldBeTrue()
        Wildcard.matchPath("src\\main\\Test.kt", "src\\*\\Test.kt").shouldBeTrue()
    }

    @Test
    fun `복합 경로 패턴 매칭`() {
        Wildcard.matchPath("src/test/FooTest.kt", "**/test/*.kt").shouldBeTrue()
        Wildcard.matchPath("src/test/kotlin/FooTest.kt", "**/test/*.kt").shouldBeFalse()
        Wildcard.matchPath("src/test/kotlin/FooTest.kt", "**/test/**/*.kt").shouldBeTrue()
        Wildcard.matchPath("src/main/kotlin/Foo.kt", "**/test/*.kt").shouldBeFalse()
    }

    @Test
    fun `연속 깊은 트리 와일드카드 처리`() {
        Wildcard.matchPath("a/b/c", "**/**").shouldBeTrue()
        Wildcard.matchPath("a/b/c", "**/**/c").shouldBeTrue()
    }

    @Test
    fun `패턴만 있는 경우`() {
        Wildcard.matchPath("a", "**").shouldBeTrue()
        Wildcard.matchPath("a/b/c", "**").shouldBeTrue()
    }

    // ---------------------------------------------------------------- matchOne

    @Test
    fun `여러 패턴 중 하나라도 매칭되면 인덱스 반환`() {
        Wildcard.matchOne("hello.kt", listOf("*.java", "*.kt", "*.py")) shouldBeEqualTo 1
        Wildcard.matchOne("hello.rs", listOf("*.java", "*.kt", "*.py")) shouldBeEqualTo -1
        Wildcard.matchOne("test", listOf("t*")) shouldBeEqualTo 0
    }

    // ---------------------------------------------------------------- matchPathOne

    @Test
    fun `여러 경로 패턴 중 하나라도 매칭되면 인덱스 반환`() {
        Wildcard.matchPathOne(
            "src/test/Foo.kt",
            listOf("**/main/*.kt", "**/test/*.kt")
        ) shouldBeEqualTo 1

        Wildcard.matchPathOne(
            "build/classes/Foo.class",
            listOf("**/main/*.kt", "**/test/*.kt")
        ) shouldBeEqualTo -1
    }
}
