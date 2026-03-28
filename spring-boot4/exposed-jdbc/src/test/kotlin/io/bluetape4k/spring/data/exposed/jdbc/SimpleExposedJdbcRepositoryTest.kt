package io.bluetape4k.spring.data.exposed.jdbc

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.data.exposed.jdbc.domain.UserEntity
import io.bluetape4k.spring.data.exposed.jdbc.domain.Users
import io.bluetape4k.spring.data.exposed.jdbc.repository.UserJdbcRepository
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Transactional
class SimpleExposedJdbcRepositoryTest: AbstractExposedJdbcRepositoryTest() {

    companion object: KLogging()

    @Autowired
    private lateinit var userJdbcRepository: UserJdbcRepository

    @AfterEach
    fun tearDown() {
        transaction { Users.deleteAll() }
    }

    private fun createUser(name: String, email: String, age: Int): UserEntity =
        transaction {
            UserEntity.new {
                this.name = name
                this.email = email
                this.age = age
            }
        }


    @Test
    fun `save and findById`() {
        val user = createUser("Alice", "alice@example.com", 30)
        val found = userJdbcRepository.findById(user.id.value)
        found.isPresent.shouldBeTrue()
        found.get().name shouldBeEqualTo "Alice"
    }

    @Test
    fun `findAll returns all entities`() {
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 25)
        val all = userJdbcRepository.findAll()
        all shouldHaveSize 2
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `findAll - MultithreadingTester 병렬 조회에서도 같은 개수를 반환한다`() {
        repeat(5) { i -> createUser("Parallel$i", "parallel$i@example.com", 20 + i) }
        val readCount = AtomicInteger(0)

        MultithreadingTester()
            .workers(4)
            .rounds(3)
            .add {
                userJdbcRepository.findAll() shouldHaveSize 5
                readCount.incrementAndGet()
            }
            .run()

        readCount.get() shouldBeEqualTo 12
    }

    @Test
    fun `count returns total count`() {
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 25)
        val count = userJdbcRepository.count()
        count shouldBeEqualTo 2L
    }

    @Test
    fun `existsById returns true when entity exists`() {
        val user = createUser("Alice", "alice@example.com", 30)
        val exists = userJdbcRepository.existsById(user.id.value)
        exists.shouldBeTrue()
    }

    @Test
    fun `existsById returns false when entity does not exist`() {
        userJdbcRepository.existsById(-1L).shouldBeFalse()
    }

    @Test
    fun `deleteById removes entity`() {
        val user = createUser("Alice", "alice@example.com", 30)
        userJdbcRepository.deleteById(user.id.value)
        val found = userJdbcRepository.findById(user.id.value)
        found.isPresent.shouldBeFalse()
    }

    @Test
    fun `deleteAll removes all entities`() {
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 25)
        userJdbcRepository.deleteAll()
        val count = userJdbcRepository.count()
        count shouldBeEqualTo 0L
    }

    @Test
    fun `deleteAllById removes specified entities`() {
        val alice = createUser("Alice", "alice@example.com", 30)
        val bob = createUser("Bob", "bob@example.com", 25)
        createUser("Charlie", "charlie@example.com", 35)

        userJdbcRepository.deleteAllById(listOf(alice.id.value, bob.id.value))
        userJdbcRepository.count() shouldBeEqualTo 1L
        userJdbcRepository.exists { Users.name eq "Charlie" }.shouldBeTrue()
    }

    @Test
    fun `deleteAll with entities removes specified entities`() {
        val alice = createUser("Alice", "alice@example.com", 30)
        val bob = createUser("Bob", "bob@example.com", 25)
        createUser("Charlie", "charlie@example.com", 35)

        userJdbcRepository.deleteAll(listOf(alice, bob))
        userJdbcRepository.count() shouldBeEqualTo 1L
    }

    @Test
    fun `findAllById returns matching entities`() {
        val alice = createUser("Alice", "alice@example.com", 30)
        val bob = createUser("Bob", "bob@example.com", 25)
        createUser("Charlie", "charlie@example.com", 35)

        val found = userJdbcRepository.findAllById(listOf(alice.id.value, bob.id.value))
        found shouldHaveSize 2
        found.map { it.name }.toSet() shouldBeEqualTo setOf("Alice", "Bob")
    }

    @Test
    fun `saveAll with Iterable returns all entities`() {
        // Exposed DAO에서 saveAll은 이미 트랜잭션 내에서 생성된 엔티티를 그대로 반환합니다.
        val alice = createUser("Alice", "alice@example.com", 30)
        val bob = createUser("Bob", "bob@example.com", 25)

        val saved = userJdbcRepository.saveAll(listOf(alice, bob))
        saved shouldHaveSize 2
        userJdbcRepository.count() shouldBeEqualTo 2L
    }

    @Test
    fun `findAll with DSL op`() {
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 17)

        val adults = userJdbcRepository.findAll { Users.age greaterEq 18 }

        adults shouldHaveSize 1
        adults[0].name shouldBeEqualTo "Alice"
    }

    @Test
    fun `exists with DSL op`() {
        createUser("Alice", "alice@example.com", 30)
        val exists = userJdbcRepository.exists { Users.name eq "Alice" }

        exists.shouldBeTrue()
    }

    @Test
    fun `findAll with paging`() {
        repeat(10) { i -> createUser("User$i", "user$i@example.com", 20 + i) }
        val page = userJdbcRepository.findAll(PageRequest.of(0, 3))

        page.content shouldHaveSize 3
        page.totalElements shouldBeEqualTo 10L
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `findAll with paging - StructuredTaskScopeTester 병렬 조회에서도 totalElements 가 유지된다`() {
        repeat(6) { i -> createUser("Structured$i", "structured$i@example.com", 30 + i) }
        val totals = Collections.synchronizedList(mutableListOf<Long>())

        StructuredTaskScopeTester()
            .rounds(4)
            .add {
                val page = userJdbcRepository.findAll(PageRequest.of(0, 2))
                totals += page.totalElements
            }
            .run()

        totals shouldHaveSize 4
        totals.forEach { it shouldBeEqualTo 6L }
    }

    @Test
    fun `findAll with Sort returns sorted list`() {
        createUser("Charlie", "charlie@example.com", 35)
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 25)

        val sorted = userJdbcRepository.findAll(Sort.by(Sort.Direction.ASC, "age"))
        sorted.map { it.age } shouldBeEqualTo listOf(25, 30, 35)
    }

    @Test
    fun `extractId returns value for existing entity`() {
        val user = createUser("Alice", "alice@example.com", 30)
        val id = userJdbcRepository.extractId(user)
        id.shouldNotBeNull()
        id shouldBeEqualTo user.id.value
    }
}
