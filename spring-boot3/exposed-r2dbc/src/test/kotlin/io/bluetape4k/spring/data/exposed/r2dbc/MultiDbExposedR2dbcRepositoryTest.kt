package io.bluetape4k.spring.data.exposed.r2dbc

import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.TestDBConfig
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.data.exposed.r2dbc.domain.User
import io.bluetape4k.spring.data.exposed.r2dbc.domain.Users
import io.bluetape4k.spring.data.exposed.r2dbc.repository.support.SimpleExposedR2dbcRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class MultiDbExposedR2dbcRepositoryTest: AbstractExposedR2dbcTest() {

    companion object: KLoggingChannel() {
        init {
            TestDBConfig.useFastDB = false  // H2 + PostgreSQL + MySQL_V8 모두 테스트
        }

        @JvmStatic
        fun enableDialects(): Set<TestDB> = TestDB.enabledDialects()
    }

    private fun createRepo() = SimpleExposedR2dbcRepository(
        table = Users,
        toDomainMapper = { row: ResultRow ->
            User(
                id = row[Users.id].value,
                name = row[Users.name],
                email = row[Users.email],
                age = row[Users.age],
            )
        },
        persistValuesProvider = { user: User ->
            mapOf(
                Users.name to user.name,
                Users.email to user.email,
                Users.age to user.age,
            )
        },
        idExtractor = { user: User -> user.id },
    )

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `save and findByIdOrNull`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val repo = createRepo()
            val saved = repo.save(User(id = null, name = "Alice", email = "alice@example.com", age = 30))
            saved.id.shouldNotBeNull()
            val found = repo.findByIdOrNull(saved.id)
            found.shouldNotBeNull()
            found.name shouldBeEqualTo "Alice"
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAllAsList returns all entities`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val repo = createRepo()
            repo.save(User(id = null, name = "Alice", email = "alice@example.com", age = 30))
            repo.save(User(id = null, name = "Bob", email = "bob@example.com", age = 25))
            repo.findAllAsList() shouldHaveSize 2
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `count and existsById`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val repo = createRepo()
            val saved = repo.save(User(id = null, name = "Alice", email = "alice@example.com", age = 30))
            repo.count() shouldBeEqualTo 1L
            repo.existsById(saved.id!!).shouldBeTrue()
            repo.existsById(-1L).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `deleteById removes entity`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val repo = createRepo()
            val saved = repo.save(User(id = null, name = "Alice", email = "alice@example.com", age = 30))
            repo.deleteById(saved.id!!)
            repo.findByIdOrNull(saved.id).let { it == null }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `deleteAll removes all entities`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val repo = createRepo()
            repo.save(User(id = null, name = "Alice", email = "alice@example.com", age = 30))
            repo.save(User(id = null, name = "Bob", email = "bob@example.com", age = 25))
            repo.deleteAll()
            repo.count() shouldBeEqualTo 0L
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAllById returns matching entities`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val repo = createRepo()
            val alice = repo.save(User(id = null, name = "Alice", email = "alice@example.com", age = 30))
            val bob = repo.save(User(id = null, name = "Bob", email = "bob@example.com", age = 25))
            repo.save(User(id = null, name = "Charlie", email = "charlie@example.com", age = 35))

            val found = repo.findAllById(listOf(alice.id!!, bob.id!!)).toList()
            found shouldHaveSize 2
            found.map { it.name }.toSet() shouldBeEqualTo setOf("Alice", "Bob")
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `deleteAllById removes specified entities`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val repo = createRepo()
            val alice = repo.save(User(id = null, name = "Alice", email = "alice@example.com", age = 30))
            val bob = repo.save(User(id = null, name = "Bob", email = "bob@example.com", age = 25))
            repo.save(User(id = null, name = "Charlie", email = "charlie@example.com", age = 35))

            repo.deleteAllById(listOf(alice.id!!, bob.id!!))
            repo.count() shouldBeEqualTo 1L
            repo.exists { Users.name eq "Charlie" }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAll with Sort returns sorted list`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val repo = createRepo()
            repo.save(User(id = null, name = "Charlie", email = "charlie@example.com", age = 35))
            repo.save(User(id = null, name = "Alice", email = "alice@example.com", age = 30))
            repo.save(User(id = null, name = "Bob", email = "bob@example.com", age = 25))

            val results = repo.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "age"))).content
            results.map { it.age } shouldBeEqualTo listOf(25, 30, 35)
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAll with Pageable returns page`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val repo = createRepo()
            repeat(5) { i ->
                repo.save(User(id = null, name = "User$i", email = "user$i@example.com", age = 20 + i))
            }

            val page = repo.findAll(PageRequest.of(0, 3))
            page.content shouldHaveSize 3
            page.totalElements shouldBeEqualTo 5L
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAll with DSL op filters correctly`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val repo = createRepo()
            repo.save(User(id = null, name = "Alice", email = "alice@example.com", age = 30))
            repo.save(User(id = null, name = "Bob", email = "bob@example.com", age = 17))

            val adults = repo.findAll { Users.age greaterEq 18 }.toList()
            adults shouldHaveSize 1
            adults[0].name shouldBeEqualTo "Alice"
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `count and exists with DSL op`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val repo = createRepo()
            repo.save(User(id = null, name = "Alice", email = "alice@example.com", age = 30))
            repo.save(User(id = null, name = "Bob", email = "bob@example.com", age = 17))

            repo.count { Users.age greaterEq 18 } shouldBeEqualTo 1L
            repo.exists { Users.name eq "Alice" }.shouldBeTrue()
            repo.exists { Users.name eq "Nobody" }.shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `streamAll streams all rows`(testDB: TestDB) = runTest {
        withTables(testDB, Users) {
            val repo = createRepo()
            repo.save(User(id = null, name = "Alice", email = "alice@example.com", age = 30))
            repo.save(User(id = null, name = "Bob", email = "bob@example.com", age = 25))

            val all = repo.streamAll().toList()
            all shouldHaveSize 2
        }
    }
}
