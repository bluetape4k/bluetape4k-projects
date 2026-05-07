package io.bluetape4k.spring.data.exposed.jdbc

import io.bluetape4k.spring.data.exposed.jdbc.domain.UserEntity
import io.bluetape4k.spring.data.exposed.jdbc.domain.Users
import io.bluetape4k.spring.data.exposed.jdbc.repository.UserJdbcRepository
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.transaction.annotation.Transactional

@Transactional
class PartTreeExposedJdbcQueryTest: AbstractExposedJdbcRepositoryTest() {

    @Autowired
    private lateinit var userJdbcRepository: UserJdbcRepository

    @AfterEach
    fun tearDown() {
        transaction { Users.deleteAll() }
    }

    private fun createUsers() {
        transaction {
            UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
            UserEntity.new { name = "Bob"; email = "bob@example.com"; age = 25 }
            UserEntity.new { name = "Charlie"; email = "charlie@example.com"; age = 35 }
            UserEntity.new { name = "Alice"; email = "alice2@example.com"; age = 20 }
        }
    }

    @Test
    fun `findByName returns matching entities`() {
        createUsers()
        val results = userJdbcRepository.findByName("Alice")
        results shouldHaveSize 2
        results.all { it.name == "Alice" }.shouldBeTrue()
    }

    @Test
    fun `findByAgeGreaterThan filters correctly`() {
        createUsers()
        val results = userJdbcRepository.findByAgeGreaterThan(25)
        results.all { it.age > 25 }.shouldBeTrue()
    }

    @Test
    fun `findByEmailContaining filters by substring`() {
        createUsers()
        val results = userJdbcRepository.findByEmailContaining("alice")
        results shouldHaveSize 2
    }

    @Test
    fun `findByNameAndAge returns single result`() {
        createUsers()
        val user = userJdbcRepository.findByNameAndAge("Alice", 30)
        user.shouldNotBeNull()
        user.email shouldBeEqualTo "alice@example.com"
    }

    @Test
    fun `countByAge returns correct count`() {
        createUsers()
        val count = userJdbcRepository.countByAge(30)
        count shouldBeEqualTo 1L
    }

    @Test
    fun `existsByEmail returns true when found`() {
        createUsers()
        userJdbcRepository.existsByEmail("alice@example.com").shouldBeTrue()
    }

    @Test
    fun `existsByEmail returns false when not found`() {
        createUsers()
        userJdbcRepository.existsByEmail("notexist@example.com").shouldBeFalse()
    }

    @Test
    fun `deleteByName removes matching entities`() {
        createUsers()
        val deleted = userJdbcRepository.deleteByName("Alice")
        deleted shouldBeEqualTo 2L
        userJdbcRepository.findByName("Alice").shouldBeEmpty()
    }

    @Test
    fun `findByAgeBetween returns entities in range`() {
        createUsers()
        val results = userJdbcRepository.findByAgeBetween(25, 35)
        results.all { it.age in 25..35 }.shouldBeTrue()
    }

    @Test
    fun `findTop3ByOrderByAgeDesc returns top 3 oldest`() {
        createUsers()
        val results = userJdbcRepository.findTop3ByOrderByAgeDesc()
        results shouldHaveSize 3
        results.map { it.age } shouldBeEqualTo listOf(35, 30, 25)
    }

    @Test
    fun `findByNameOrderByAgeDesc applies declared order`() {
        createUsers()
        val results = userJdbcRepository.findByNameOrderByAgeDesc("Alice")
        results.map { it.age } shouldBeEqualTo listOf(30, 20)
    }

    @Test
    fun `findFirstByNameOrderByAgeDesc applies declared order before limiting`() {
        createUsers()
        val user = userJdbcRepository.findFirstByNameOrderByAgeDesc("Alice")
        user.shouldNotBeNull()
        user.age shouldBeEqualTo 30
    }

    @Test
    fun `findAll with Sort`() {
        createUsers()
        val results = userJdbcRepository.findAll(Sort.by(Sort.Direction.ASC, "age"))
        results.shouldNotBeEmpty()
        val ages = results.map { it.age }
        ages shouldBeEqualTo ages.sorted()
    }

    @Test
    fun `findAll with Pageable and Sort`() {
        createUsers()
        val page = userJdbcRepository.findAll(PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "age")))
        page.content shouldHaveSize 2
        page.totalElements shouldBeEqualTo 4L
        (page.content[0].age >= page.content[1].age).shouldBeTrue()
    }

    @Test
    fun `@Query native - 위치 기반 파라미터 바인딩으로 단일 엔티티 조회`() {
        createUsers()
        val found = userJdbcRepository.findByEmailNative("alice@example.com")
        found shouldHaveSize 1
        found.first().name shouldBeEqualTo "Alice"
    }

    @Test
    fun `@Query native - 파라미터 순서가 역순이어도 올바르게 바인딩된다`() {
        createUsers()
        val found = userJdbcRepository.findByEmailAndAgeNative("alice@example.com", 30)
        found shouldHaveSize 1
        found.first().email shouldBeEqualTo "alice@example.com"
    }

    @Test
    fun `@Query native - SQL injection 문자열은 값으로 취급되어 우회되지 않는다`() {
        createUsers()
        val injected = "alice@example.com' OR 1=1 --"
        val found = userJdbcRepository.findByEmailNative(injected)
        found.shouldBeEmpty()
    }

    @Test
    fun `@Query native - 따옴표가 포함된 문자열도 안전하게 조회된다`() {
        transaction {
            UserEntity.new { name = "O'Hara"; email = "o'hara@example.com"; age = 41 }
        }
        val found = userJdbcRepository.findByEmailNative("o'hara@example.com")
        found shouldHaveSize 1
        found.first().name shouldBeEqualTo "O'Hara"
    }

    @Test
    fun `@Query native - placeholder 인덱스가 잘못되면 예외를 던진다`() {
        createUsers()
        assertFailsWith<IllegalArgumentException> {
            userJdbcRepository.findByEmailNativeBrokenPlaceholder("alice@example.com")
        }
    }

    @Test
    fun `@Query native - 동일 placeholder 재사용 시 같은 인자가 재사용된다`() {
        createUsers()
        val found = userJdbcRepository.findByEmailNativeDuplicatedPlaceholder("alice@example.com")
        found shouldHaveSize 1
        found.first().email shouldBeEqualTo "alice@example.com"
    }

    @Test
    fun `@Query native - Long 타입 숫자 파라미터도 정상 바인딩된다`() {
        createUsers()
        val found = userJdbcRepository.findByAgeNativeLong(30L)
        found shouldHaveSize 1
        found.first().name shouldBeEqualTo "Alice"
    }

    @Test
    fun `@Query native - 범위 조건 파라미터를 순서대로 바인딩한다`() {
        createUsers()
        val found = userJdbcRepository.findByAgeRangeNative(25, 30)
        found shouldHaveSize 2
        found.all { it.age in 25..30 }.shouldBeTrue()
    }

    @Test
    fun `@Query native - 10번째 placeholder 인덱스를 올바르게 해석한다`() {
        createUsers()
        val found = userJdbcRepository.findByEmailNativeTenthPlaceholder(
            "x1", "x2", "x3", "x4", "x5", "x6", "x7", "x8", "x9", "alice@example.com"
        )
        found shouldHaveSize 1
        found.first().name shouldBeEqualTo "Alice"
    }

    // ── 추가 쿼리 타입 커버리지 테스트 ────────────────────────────────────────

    @Test
    fun `findByNameNot - 특정 이름이 아닌 엔티티 조회`() {
        createUsers()
        val results = userJdbcRepository.findByNameNot("Alice")
        results.none { it.name == "Alice" }.shouldBeTrue()
        results shouldHaveSize 2
    }

    @Test
    fun `findByAgeGreaterThanEqual - 같거나 큰 나이 조회`() {
        createUsers()
        val results = userJdbcRepository.findByAgeGreaterThanEqual(30)
        results.all { it.age >= 30 }.shouldBeTrue()
        results shouldHaveSize 2
    }

    @Test
    fun `findByAgeLessThan - 작은 나이 조회`() {
        createUsers()
        val results = userJdbcRepository.findByAgeLessThan(30)
        results.all { it.age < 30 }.shouldBeTrue()
        results shouldHaveSize 2
    }

    @Test
    fun `findByAgeLessThanEqual - 같거나 작은 나이 조회`() {
        createUsers()
        val results = userJdbcRepository.findByAgeLessThanEqual(30)
        results.all { it.age <= 30 }.shouldBeTrue()
        results shouldHaveSize 3
    }

    @Test
    fun `findByNameStartingWith - 접두사로 시작하는 이름 조회`() {
        createUsers()
        val results = userJdbcRepository.findByNameStartingWith("Al")
        results.all { it.name.startsWith("Al") }.shouldBeTrue()
        results shouldHaveSize 2
    }

    @Test
    fun `findByNameEndingWith - 접미사로 끝나는 이름 조회`() {
        createUsers()
        val results = userJdbcRepository.findByNameEndingWith("e")
        results.all { it.name.endsWith("e") }.shouldBeTrue()
        results shouldHaveSize 3
    }

    @Test
    fun `findByNameNotContaining - 특정 문자열을 포함하지 않는 이름 조회`() {
        createUsers()
        val results = userJdbcRepository.findByNameNotContaining("Ali")
        results.none { it.name.contains("Ali") }.shouldBeTrue()
        results shouldHaveSize 2
    }

    @Test
    fun `findByNameLike - LIKE 패턴으로 이름 조회`() {
        createUsers()
        val results = userJdbcRepository.findByNameLike("A%")
        results.all { it.name.startsWith("A") }.shouldBeTrue()
        results shouldHaveSize 2
    }

    @Test
    fun `findByNameNotLike - NOT LIKE 패턴으로 이름 조회`() {
        createUsers()
        val results = userJdbcRepository.findByNameNotLike("A%")
        results.none { it.name.startsWith("A") }.shouldBeTrue()
        results shouldHaveSize 2
    }

    @Test
    fun `findByAgeIn - 특정 나이 목록에 포함된 엔티티 조회`() {
        createUsers()
        val results = userJdbcRepository.findByAgeIn(listOf(25, 35))
        results.all { it.age in listOf(25, 35) }.shouldBeTrue()
        results shouldHaveSize 2
    }

    @Test
    fun `findByAgeNotIn - 특정 나이 목록에 포함되지 않은 엔티티 조회`() {
        createUsers()
        val results = userJdbcRepository.findByAgeNotIn(listOf(25, 35))
        results.none { it.age in listOf(25, 35) }.shouldBeTrue()
        results shouldHaveSize 2
    }
}
