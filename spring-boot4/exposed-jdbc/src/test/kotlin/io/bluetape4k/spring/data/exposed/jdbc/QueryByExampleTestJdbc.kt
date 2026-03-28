package io.bluetape4k.spring.data.exposed.jdbc

import io.bluetape4k.spring.data.exposed.jdbc.domain.UserEntity
import io.bluetape4k.spring.data.exposed.jdbc.domain.Users
import io.bluetape4k.spring.data.exposed.jdbc.repository.UserJdbcRepository
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class QueryByExampleTestJdbc: AbstractExposedJdbcRepositoryTest() {

    @Autowired
    private lateinit var userJdbcRepository: UserJdbcRepository

    @AfterEach
    fun tearDown() {
        transaction { Users.deleteAll() }
    }

    @Test
    fun `findAll with DSL op for age 30`() {
        transaction {
            UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
            UserEntity.new { name = "Bob"; email = "bob@example.com"; age = 30 }
            UserEntity.new { name = "Charlie"; email = "charlie@example.com"; age = 25 }
        }

        val results = userJdbcRepository.findAll { Users.age eq 30 }
        results shouldHaveSize 2
    }

    @Test
    fun `count with DSL op`() {
        transaction {
            UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
            UserEntity.new { name = "Bob"; email = "bob@example.com"; age = 25 }
        }

        val count = userJdbcRepository.count { Users.age eq 30 }
        count shouldBeEqualTo 1L
    }

    @Test
    fun `exists with DSL op`() {
        transaction {
            UserEntity.new { name = "Alice"; email = "alice@example.com"; age = 30 }
        }

        userJdbcRepository.exists { Users.name eq "Alice" }.shouldBeTrue()
        userJdbcRepository.exists { Users.name eq "Nobody" }.shouldBeFalse()
    }
}
