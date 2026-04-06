package io.bluetape4k.spring.data.exposed.r2dbc

import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.data.exposed.r2dbc.domain.User
import io.bluetape4k.spring.data.exposed.r2dbc.domain.Users
import io.bluetape4k.spring.data.exposed.r2dbc.repository.UserR2dbcRepository
import io.bluetape4k.support.requireNotNull
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
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class SimpleExposedR2dbcRepositoryTest: AbstractExposedR2dbcRepositoryTest() {

    companion object: KLoggingChannel()

    @Autowired
    private lateinit var userRepository: UserR2dbcRepository

    private suspend fun createUser(name: String, email: String, age: Int): User {
        val id = Users.insertAndGetId {
            it[Users.name] = name
            it[Users.email] = email
            it[Users.age] = age
        }.value

        return User(id = id, name = name, email = email, age = age)
    }


    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `findById returns entity`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val user = createUser("Alice", "alice@example.com", 30)
            val userId = user.id.requireNotNull("user.id")
            val found = userRepository.findByIdOrNull(userId)
            found.shouldNotBeNull()
            found.name shouldBeEqualTo "Alice"
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `findAll as Flow returns all entities`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            createUser("Alice", "alice@example.com", 30)
            createUser("Bob", "bob@example.com", 25)
            val all = userRepository.findAll().toList()
            all shouldHaveSize 2
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `save - SuspendedJobTester 경쟁 상황에서도 모든 엔티티를 저장한다`(testDB: TestDB) = runTest {
        Assumptions.assumeTrue { testDB in TestDB.ALL_H2 + TestDB.ALL_POSTGRES }
        
        withTables(testDB, Users) {
            val savedIds = ConcurrentLinkedQueue<Long>()
            val workerSize = 6

            SuspendedJobTester()
                .workers(workerSize)
                .rounds(1)
                .addAll(
                    (1..workerSize).map { index ->
                        suspend {
                            val user = User(
                                id = null,
                                name = "Concurrent-$index",
                                email = "concurrent-$index@example.com",
                                age = 20 + index,
                            )
                            val saved = userRepository.save(user)
                            saved.id.shouldNotBeNull().also(savedIds::add)
                        }
                    }
                )
                .run()

            savedIds.distinct().size shouldBeEqualTo workerSize
            userRepository.count() shouldBeEqualTo workerSize.toLong()
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `findAllList returns all entities`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, Users) {
            createUser("Alice", "alice@example.com", 30)
            createUser("Bob", "bob@example.com", 25)

            log.debug { "findAllList ..." }
            val all = userRepository.findAllAsList()

            all.forEach {
                log.debug { "User=$it" }
            }
            all shouldHaveSize 2
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `streamAll opens its own transaction and streams rows`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            createUser("Alice", "alice@example.com", 30)
            createUser("Bob", "bob@example.com", 25)

            val all = userRepository.streamAll(it.db).toList()
            all shouldHaveSize 2
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `findAllAsList - SuspendedJobTester 병렬 조회에서도 같은 개수를 본다`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, Users) {
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
    }

    @EnabledForJreRange(min = JRE.JAVA_21, max = JRE.JAVA_26)
    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `streamAll - StructuredTaskScopeTester 병렬 collector 에서도 전체 row 를 유지한다`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            repeat(3) { index ->
                createUser("Structured-$index", "structured-$index@example.com", 40 + index)
            }

            val collectedCounts = ConcurrentLinkedQueue<Int>()
            StructuredTaskScopeTester()
                .rounds(4)
                .add {
                    val users = runBlocking { userRepository.streamAll(it.db).toList() }
                    collectedCounts += users.size
                }
                .run()

            collectedCounts shouldHaveSize 4
            collectedCounts.all { it == 3 }.shouldBeTrue()
            // collectedCounts.forEach { it shouldBeEqualTo 3 }
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `count returns correct total`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            createUser("Alice", "alice@example.com", 30)
            createUser("Bob", "bob@example.com", 25)
            userRepository.count() shouldBeEqualTo 2L
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `existsById returns true when entity exists`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val user = createUser("Alice", "alice@example.com", 30)
            userRepository.existsById(user.id.requireNotNull("user.id")).shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `existsById returns false when entity does not exist`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            userRepository.existsById(-1L).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `deleteById removes entity`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val user = createUser("Alice", "alice@example.com", 30)
            val userId = user.id.requireNotNull("user.id")
            userRepository.deleteById(userId)
            userRepository.findByIdOrNull(userId).shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `deleteAll removes all entities`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            createUser("Alice", "alice@example.com", 30)
            createUser("Bob", "bob@example.com", 25)
            userRepository.deleteAll()
            userRepository.count() shouldBeEqualTo 0L
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `findAll with Sort returns sorted list`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            createUser("Charlie", "charlie@example.com", 35)
            createUser("Alice", "alice@example.com", 30)
            createUser("Bob", "bob@example.com", 25)

            val pageable = PageRequest.of(0, 10, Sort.by(Direction.ASC, "age"))
            val results = userRepository.findAll(pageable).content
            val ages = results.map { it.age }
            ages shouldBeEqualTo ages.sorted()
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `findAll with Pageable returns page`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            repeat(5) { i -> createUser("User$i", "user$i@example.com", 20 + i) }
            val page = userRepository.findAll(PageRequest.of(0, 3))
            page.content shouldHaveSize 3
            page.totalElements shouldBeEqualTo 5L
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `count with DSL op filters correctly`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            createUser("Alice", "alice@example.com", 30)
            createUser("Bob", "bob@example.com", 17)

            userRepository.count { Users.age greaterEq 18 } shouldBeEqualTo 1L
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `count with DSL op returns correct count`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            createUser("Alice", "alice@example.com", 30)
            createUser("Bob", "bob@example.com", 17)

            userRepository.count { Users.age greaterEq 18 } shouldBeEqualTo 1L
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `exists with DSL op returns true when found`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            createUser("Alice", "alice@example.com", 30)

            userRepository.exists { Users.name eq "Alice" }.shouldBeTrue()
            userRepository.exists { Users.name eq "Nobody" }.shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `saveAll with Iterable saves all entities`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val users = listOf(
                User(id = null, name = "Alice", email = "alice@example.com", age = 30),
                User(id = null, name = "Bob", email = "bob@example.com", age = 25),
            )

            val saved = userRepository.saveAll(users).toList()
            saved shouldHaveSize 2
            saved.all { it.id != null }.shouldBeTrue()
            userRepository.count() shouldBeEqualTo 2L
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `saveAll with Flow saves all entities`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val usersFlow = listOf(
                User(id = null, name = "Alice", email = "alice@example.com", age = 30),
                User(id = null, name = "Bob", email = "bob@example.com", age = 25),
            ).asFlow()

            val saved = userRepository.saveAll(usersFlow).toList()
            saved shouldHaveSize 2
            saved.all { it.id != null }.shouldBeTrue()
            userRepository.count() shouldBeEqualTo 2L
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `findAllById with Iterable returns matching entities`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val alice = createUser("Alice", "alice@example.com", 30)
            val bob = createUser("Bob", "bob@example.com", 25)
            createUser("Charlie", "charlie@example.com", 35)

            val found = userRepository.findAllById(listOf(alice.id!!, bob.id!!)).toList()
            found shouldHaveSize 2
            found.map { it.name }.toSet() shouldBeEqualTo setOf("Alice", "Bob")
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `findAllById with Flow returns matching entities`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val alice = createUser("Alice", "alice@example.com", 30)
            val bob = createUser("Bob", "bob@example.com", 25)
            createUser("Charlie", "charlie@example.com", 35)

            val found = userRepository.findAllById(flowOf(alice.id!!, bob.id!!)).toList()
            found shouldHaveSize 2
            found.map { it.name }.toSet() shouldBeEqualTo setOf("Alice", "Bob")
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `deleteAllById removes specified entities`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val alice = createUser("Alice", "alice@example.com", 30)
            val bob = createUser("Bob", "bob@example.com", 25)
            createUser("Charlie", "charlie@example.com", 35)

            userRepository.deleteAllById(listOf(alice.id!!, bob.id!!))
            userRepository.count() shouldBeEqualTo 1L
            userRepository.exists { Users.name eq "Charlie" }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `deleteAll with Iterable removes specified entities`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val alice = createUser("Alice", "alice@example.com", 30)
            val bob = createUser("Bob", "bob@example.com", 25)
            createUser("Charlie", "charlie@example.com", 35)

            userRepository.deleteAll(listOf(alice, bob))
            userRepository.count() shouldBeEqualTo 1L
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `deleteAll with Flow removes specified entities`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val alice = createUser("Alice", "alice@example.com", 30)
            val bob = createUser("Bob", "bob@example.com", 25)
            createUser("Charlie", "charlie@example.com", 35)

            userRepository.deleteAll(flowOf(alice, bob))
            userRepository.count() shouldBeEqualTo 1L
        }
    }

    @ParameterizedTest
    @MethodSource(AbstractExposedR2dbcTest.ENABLE_DIALECTS_METHOD)
    fun `findAll with DSL op filters correctly`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            createUser("Alice", "alice@example.com", 30)
            createUser("Bob", "bob@example.com", 17)

            val adults = userRepository.findAll { Users.age greaterEq 18 }.toList()
            adults shouldHaveSize 1
            adults[0].name shouldBeEqualTo "Alice"
        }
    }
}
