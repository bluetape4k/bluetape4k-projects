package io.bluetape4k.spring.data

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.AbstractSpringTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotContain
import org.junit.jupiter.api.Test

class ExampleMatcherSupportTest: AbstractSpringTest() {

    companion object: KLogging()

    data class User(
        val name: String = "",
        val email: String = "",
        val age: Int = 0,
        val city: String = "",
    )

    @Test
    fun `buildExampleMatcher는 문자열 필드로 정확 매칭 매처를 생성한다`() {
        val matcher = User::class.buildExampleMatcher("name", "email")

        // name과 email은 검색 대상이므로 ignoredPaths에 없어야 한다
        val ignored = matcher.ignoredPaths.toList()
        ignored shouldNotContain "name"
        ignored shouldNotContain "email"
        ignored shouldContain "age"
        ignored shouldContain "city"
    }

    @Test
    fun `buildExampleMatcher는 KProperty 필드로 정확 매칭 매처를 생성한다`() {
        val matcher = User::class.buildExampleMatcher(User::name, User::age)

        val ignored = matcher.ignoredPaths.toList()
        ignored shouldNotContain "name"
        ignored shouldNotContain "age"
        ignored shouldContain "email"
        ignored shouldContain "city"
    }

    @Test
    fun `buildExampleMatcher에 일부 필드만 지정하면 나머지가 무시된다`() {
        val matcher = User::class.buildExampleMatcher("name", "email", "age")

        val ignored = matcher.ignoredPaths.toList()
        ignored shouldNotContain "name"
        ignored shouldNotContain "email"
        ignored shouldNotContain "age"
        ignored shouldContain "city"
    }

    @Test
    fun `빈 검색 필드로 buildExampleMatcher를 호출하면 모든 프로퍼티가 무시된다`() {
        val searchFields = emptyArray<String>()
        val matcher = User::class.buildExampleMatcher(*searchFields)

        // 모든 프로퍼티가 ignoredPaths에 포함
        val ignored = matcher.ignoredPaths.toList()
        ignored shouldContain "name"
        ignored shouldContain "email"
        ignored shouldContain "age"
        ignored shouldContain "city"
    }

    @Test
    fun `ignoredProperties는 제외 목록에 없는 프로퍼티 이름만 반환한다`() {
        val ignored = User::class.ignoredProperties("name").toList()

        ignored shouldNotContain "name"
        ignored shouldContain "email"
        ignored shouldContain "age"
        ignored shouldContain "city"
    }

    @Test
    fun `ignoredProperties는 KProperty 제외 목록을 지원한다`() {
        val ignored = User::class.ignoredProperties(User::name, User::email).toList()

        ignored shouldNotContain "name"
        ignored shouldNotContain "email"
        ignored shouldContain "age"
        ignored shouldContain "city"
    }

    @Test
    fun `buildExampleMatcher의 매칭 모드는 MatchMode ANY가 아니라 ALL이다`() {
        val matcher = User::class.buildExampleMatcher("name")

        matcher.isAllMatching shouldBeEqualTo true
    }
}
