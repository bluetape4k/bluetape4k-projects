package io.bluetape4k.spring.data.exposed.jdbc

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.data.exposed.jdbc.domain.UserEntity
import io.bluetape4k.spring.data.exposed.jdbc.domain.Users
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.ExposedEntityInformationImpl
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.SimpleExposedJdbcRepository
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class MultiDbExposedJdbcRepositoryTest : AbstractExposedTest() {

    companion object : KLogging() {
        @JvmStatic
        fun enableDialects(): Set<TestDB> = TestDB.enabledDialects()
    }

    private fun createRepo(): SimpleExposedJdbcRepository<UserEntity, Long> =
        SimpleExposedJdbcRepository(ExposedEntityInformationImpl(UserEntity::class.java))

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `save and findById`(testDB: TestDB) {
        withTables(testDB, Users) {
            val repo = createRepo()
            val user = UserEntity.new {
                name = "Alice"
                email = "alice@example.com"
                age = 30
            }
            val found = repo.findById(user.id.value)
            found.isPresent.shouldBeTrue()
            found.get().name shouldBeEqualTo "Alice"
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAll returns all entities`(testDB: TestDB) {
        withTables(testDB, Users) {
            val repo = createRepo()
            UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
            UserEntity.new { name = "Bob"; email = "bob@example.com"; age = 25 }
            repo.findAll() shouldHaveSize 2
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `count and existsById`(testDB: TestDB) {
        withTables(testDB, Users) {
            val repo = createRepo()
            val user = UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
            repo.count() shouldBeEqualTo 1L
            repo.existsById(user.id.value).shouldBeTrue()
            repo.existsById(-1L).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `deleteById removes entity`(testDB: TestDB) {
        withTables(testDB, Users) {
            val repo = createRepo()
            val user = UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
            repo.deleteById(user.id.value)
            repo.findById(user.id.value).isPresent.shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `deleteAll removes all entities`(testDB: TestDB) {
        withTables(testDB, Users) {
            val repo = createRepo()
            UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
            UserEntity.new { name = "Bob"; email = "bob@example.com"; age = 25 }
            repo.deleteAll()
            repo.count() shouldBeEqualTo 0L
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAllById returns matching entities`(testDB: TestDB) {
        withTables(testDB, Users) {
            val repo = createRepo()
            val alice = UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
            val bob = UserEntity.new { name = "Bob"; email = "bob@example.com"; age = 25 }
            UserEntity.new { name = "Charlie"; email = "charlie@example.com"; age = 35 }

            val found = repo.findAllById(listOf(alice.id.value, bob.id.value))
            found shouldHaveSize 2
            found.map { it.name }.toSet() shouldBeEqualTo setOf("Alice", "Bob")
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `deleteAllById removes specified entities`(testDB: TestDB) {
        withTables(testDB, Users) {
            val repo = createRepo()
            val alice = UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
            val bob = UserEntity.new { name = "Bob"; email = "bob@example.com"; age = 25 }
            UserEntity.new { name = "Charlie"; email = "charlie@example.com"; age = 35 }

            repo.deleteAllById(listOf(alice.id.value, bob.id.value))
            repo.count() shouldBeEqualTo 1L
            repo.exists { Users.name eq "Charlie" }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAll with Sort returns sorted list`(testDB: TestDB) {
        withTables(testDB, Users) {
            val repo = createRepo()
            UserEntity.new { name = "Charlie"; email = "charlie@example.com"; age = 35 }
            UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
            UserEntity.new { name = "Bob"; email = "bob@example.com"; age = 25 }

            val sorted = repo.findAll(Sort.by(Sort.Direction.ASC, "age"))
            sorted.map { it.age } shouldBeEqualTo listOf(25, 30, 35)
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAll with Pageable returns page`(testDB: TestDB) {
        withTables(testDB, Users) {
            val repo = createRepo()
            repeat(5) { i ->
                UserEntity.new { name = "User$i"; email = "user$i@example.com"; age = 20 + i }
            }

            val page = repo.findAll(PageRequest.of(0, 3))
            page.content shouldHaveSize 3
            page.totalElements shouldBeEqualTo 5L
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAll with DSL op filters correctly`(testDB: TestDB) {
        withTables(testDB, Users) {
            val repo = createRepo()
            UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
            UserEntity.new { name = "Bob"; email = "bob@example.com"; age = 17 }

            val adults = repo.findAll { Users.age greaterEq 18 }
            adults shouldHaveSize 1
            adults[0].name shouldBeEqualTo "Alice"
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `count and exists with DSL op`(testDB: TestDB) {
        withTables(testDB, Users) {
            val repo = createRepo()
            UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
            UserEntity.new { name = "Bob"; email = "bob@example.com"; age = 17 }

            repo.count { Users.age greaterEq 18 } shouldBeEqualTo 1L
            repo.exists { Users.name eq "Alice" }.shouldBeTrue()
            repo.exists { Users.name eq "Nobody" }.shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `extractId returns value for existing entity`(testDB: TestDB) {
        withTables(testDB, Users) {
            val repo = createRepo()
            val user = UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
            val id = repo.extractId(user)
            id.shouldNotBeNull()
            id shouldBeEqualTo user.id.value
        }
    }
}
