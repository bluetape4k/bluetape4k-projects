package io.bluetape4k.spring.data.exposed.r2dbc

import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.data.exposed.r2dbc.domain.User
import io.bluetape4k.spring.data.exposed.r2dbc.domain.Users
import io.bluetape4k.spring.data.exposed.r2dbc.repository.UserR2dbcRepository
import io.bluetape4k.support.requireNotNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.r2dbc.deleteAll
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class SimpleExposedR2dbcRepositoryTest : AbstractExposedR2dbcRepositoryTest() {

    companion object : KLoggingChannel()

    @Autowired
    private lateinit var userRepository: UserR2dbcRepository

    @AfterEach
    fun afterEach(): Unit = runBlocking {
        suspendTransaction(r2dbcDatabase) { Users.deleteAll() }
    }

    private suspend fun createUser(name: String, email: String, age: Int): User =
        suspendTransaction(r2dbcDatabase) {
            val id = Users.insertAndGetId {
                it[Users.name] = name
                it[Users.email] = email
                it[Users.age] = age
            }.value

            User(id = id, name = name, email = email, age = age)
        }

    @Test
    fun `findById returns entity`() = runTest {
        val user = createUser("Alice", "alice@example.com", 30)
        val userId = user.id.requireNotNull("user.id")
        val found = userRepository.findByIdOrNull(userId)
        found.shouldNotBeNull()
        found.name shouldBeEqualTo "Alice"
    }

    @Test
    fun `findAll as Flow returns all entities`() = runTest {
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 25)
        val all = userRepository.findAll().toList()
        all shouldHaveSize 2
    }

    @Test
    fun `save - SuspendedJobTester 경쟁 상황에서도 모든 엔티티를 저장한다`() = runTest {
        val savedIds = Collections.synchronizedList(mutableListOf<Long>())
        val workerSize = 6

        SuspendedJobTester()
            .workers(workerSize)
            .rounds(1)
            .addAll(
                (1..workerSize).map { index ->
                    suspend {
                        val saved = userRepository.save(
                            User(
                                id = null,
                                name = "Concurrent-$index",
                                email = "concurrent-$index@example.com",
                                age = 20 + index,
                            )
                        )
                        saved.id.shouldNotBeNull().also(savedIds::add)
                    }
                }
            )
            .run()

        savedIds.distinct().size shouldBeEqualTo workerSize
        userRepository.count() shouldBeEqualTo workerSize.toLong()
    }

    @Test
    fun `findAllList returns all entities`() = runSuspendIO {
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 25)

        log.debug { "findAllList ..." }
        val all = userRepository.findAllAsList()
        delay(100.milliseconds)
        all.forEach {
            log.debug { "User=$it" }
        }
        all shouldHaveSize 2
    }

    @Test
    fun `streamAll opens its own transaction and streams rows`() = runTest {
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 25)

        val all = userRepository.streamAll().toList()
        all shouldHaveSize 2
    }

    @Test
    fun `findAllAsList - SuspendedJobTester 병렬 조회에서도 같은 개수를 본다`() = runSuspendIO {
        repeat(4) { index ->
            createUser("Parallel-$index", "parallel-$index@example.com", 30 + index)
        }

        val readCount = AtomicInteger(0)
        SuspendedJobTester()
            .rounds(12)
            .add {
                val users = userRepository.findAllAsList()
                users shouldHaveSize 4
                readCount.incrementAndGet()
            }
            .run()

        readCount.get() shouldBeEqualTo 12
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `streamAll - StructuredTaskScopeTester 병렬 collector 에서도 전체 row 를 유지한다`() = runTest {
        repeat(3) { index ->
            createUser("Structured-$index", "structured-$index@example.com", 40 + index)
        }

        val collectedCounts = Collections.synchronizedList(mutableListOf<Int>())
        StructuredTaskScopeTester()
            .rounds(4)
            .add {
                val users = runBlocking { userRepository.streamAll().toList() }
                collectedCounts += users.size
            }
            .run()

        collectedCounts shouldHaveSize 4
        collectedCounts.forEach { it shouldBeEqualTo 3 }
    }

    @Test
    fun `count returns correct total`() = runTest {
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 25)
        userRepository.count() shouldBeEqualTo 2L
    }

    @Test
    fun `existsById returns true when entity exists`() = runTest {
        val user = createUser("Alice", "alice@example.com", 30)
        userRepository.existsById(user.id.requireNotNull("user.id")).shouldBeTrue()
    }

    @Test
    fun `existsById returns false when entity does not exist`() = runTest {
        userRepository.existsById(-1L).shouldBeFalse()
    }

    @Test
    fun `deleteById removes entity`() = runTest {
        val user = createUser("Alice", "alice@example.com", 30)
        val userId = user.id.requireNotNull("user.id")
        userRepository.deleteById(userId)
        userRepository.findByIdOrNull(userId).shouldBeNull()
    }

    @Test
    fun `deleteAll removes all entities`() = runTest {
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 25)
        userRepository.deleteAll()
        userRepository.count() shouldBeEqualTo 0L
    }

    @Test
    fun `findAll with Sort returns sorted list`() = runTest {
        createUser("Charlie", "charlie@example.com", 35)
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 25)
        val results =
            userRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "age"))).content
        val ages = results.map { it.age }
        ages shouldBeEqualTo ages.sorted()
    }

    @Test
    fun `findAll with Pageable returns page`() = runTest {
        repeat(5) { i -> createUser("User$i", "user$i@example.com", 20 + i) }
        val page = userRepository.findAll(PageRequest.of(0, 3))
        page.content shouldHaveSize 3
        page.totalElements shouldBeEqualTo 5L
    }

    @Test
    fun `count with DSL op filters correctly`() = runTest {
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 17)
        userRepository.count { Users.age greaterEq 18 } shouldBeEqualTo 1L
    }

    @Test
    fun `count with DSL op returns correct count`() = runTest {
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 17)
        userRepository.count { Users.age greaterEq 18 } shouldBeEqualTo 1L
    }

    @Test
    fun `exists with DSL op returns true when found`() = runTest {
        createUser("Alice", "alice@example.com", 30)
        userRepository.exists { Users.name eq "Alice" }.shouldBeTrue()
        userRepository.exists { Users.name eq "Nobody" }.shouldBeFalse()
    }

    @Test
    fun `saveAll with Iterable saves all entities`() = runTest {
        val users = listOf(
            User(id = null, name = "Alice", email = "alice@example.com", age = 30),
            User(id = null, name = "Bob", email = "bob@example.com", age = 25),
        )
        val saved = userRepository.saveAll(users).toList()
        saved shouldHaveSize 2
        saved.all { it.id != null }.shouldBeTrue()
        userRepository.count() shouldBeEqualTo 2L
    }

    @Test
    fun `saveAll with Flow saves all entities`() = runTest {
        val usersFlow = listOf(
            User(id = null, name = "Alice", email = "alice@example.com", age = 30),
            User(id = null, name = "Bob", email = "bob@example.com", age = 25),
        ).asFlow()
        val saved = userRepository.saveAll(usersFlow).toList()
        saved shouldHaveSize 2
        saved.all { it.id != null }.shouldBeTrue()
        userRepository.count() shouldBeEqualTo 2L
    }

    @Test
    fun `findAllById with Iterable returns matching entities`() = runTest {
        val alice = createUser("Alice", "alice@example.com", 30)
        val bob = createUser("Bob", "bob@example.com", 25)
        createUser("Charlie", "charlie@example.com", 35)

        val found = userRepository.findAllById(listOf(alice.id!!, bob.id!!)).toList()
        found shouldHaveSize 2
        found.map { it.name }.toSet() shouldBeEqualTo setOf("Alice", "Bob")
    }

    @Test
    fun `findAllById with Flow returns matching entities`() = runTest {
        val alice = createUser("Alice", "alice@example.com", 30)
        val bob = createUser("Bob", "bob@example.com", 25)
        createUser("Charlie", "charlie@example.com", 35)

        val found = userRepository.findAllById(flowOf(alice.id!!, bob.id!!)).toList()
        found shouldHaveSize 2
        found.map { it.name }.toSet() shouldBeEqualTo setOf("Alice", "Bob")
    }

    @Test
    fun `deleteAllById removes specified entities`() = runTest {
        val alice = createUser("Alice", "alice@example.com", 30)
        val bob = createUser("Bob", "bob@example.com", 25)
        createUser("Charlie", "charlie@example.com", 35)

        userRepository.deleteAllById(listOf(alice.id!!, bob.id!!))
        userRepository.count() shouldBeEqualTo 1L
        userRepository.exists { Users.name eq "Charlie" }.shouldBeTrue()
    }

    @Test
    fun `deleteAll with Iterable removes specified entities`() = runTest {
        val alice = createUser("Alice", "alice@example.com", 30)
        val bob = createUser("Bob", "bob@example.com", 25)
        createUser("Charlie", "charlie@example.com", 35)

        userRepository.deleteAll(listOf(alice, bob))
        userRepository.count() shouldBeEqualTo 1L
    }

    @Test
    fun `deleteAll with Flow removes specified entities`() = runTest {
        val alice = createUser("Alice", "alice@example.com", 30)
        val bob = createUser("Bob", "bob@example.com", 25)
        createUser("Charlie", "charlie@example.com", 35)

        userRepository.deleteAll(flowOf(alice, bob))
        userRepository.count() shouldBeEqualTo 1L
    }

    @Test
    fun `findAll with DSL op filters correctly`() = runTest {
        createUser("Alice", "alice@example.com", 30)
        createUser("Bob", "bob@example.com", 17)

        val adults = userRepository.findAll { Users.age greaterEq 18 }.toList()
        adults shouldHaveSize 1
        adults[0].name shouldBeEqualTo "Alice"
    }
}
