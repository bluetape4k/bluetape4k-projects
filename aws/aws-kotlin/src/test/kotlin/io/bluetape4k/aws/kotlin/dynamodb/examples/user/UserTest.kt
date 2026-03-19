package io.bluetape4k.aws.kotlin.dynamodb.examples.user

import io.bluetape4k.aws.kotlin.dynamodb.AbstractKotlinDynamoDbTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class UserTest: AbstractKotlinDynamoDbTest() {

    companion object: KLoggingChannel() {
        private fun newUser(): User = User(
            userId = faker.credentials().username(),
            name = faker.name().fullName(),
            email = faker.internet().emailAddress(),
            age = faker.number().numberBetween(20, 100)
        )
    }

    private val userService = UserService(client)
    private val userTableService = UserTableService(client)

    @BeforeAll
    fun beforeAll() = runSuspendIO {
        userTableService.createTable()
        userTableService.checkTableStatus()  // Table 상태를 확인한다.
    }

    @AfterAll
    fun afterAll() = runSuspendIO {
        userTableService.deleteTableIfExists()
    }

    @Test
    fun `insert new user`() = runSuspendIO {
        val user = newUser()

        val response = userService.insert(user)

        log.debug { "response attrs=${response.attributes}" }
        log.debug { "consumed capacity=${response.consumedCapacity}" }
        response.consumedCapacity?.tableName shouldBeEqualTo USER_TABLE_NAME
    }

    @Test
    fun `get user by id`() = runSuspendIO {
        val user = newUser()
        userService.insert(user)

        val savedUser = userService.getById(user.userId)

        log.debug { "saved user=$savedUser" }
        savedUser.shouldNotBeNull()
        savedUser shouldBeEqualTo user
    }

    @Test
    fun `update user property`() = runSuspendIO {
        val user = newUser()
        userService.insert(user)

        // Update
        val updatedUser = user.copy(name = faker.name().fullName())
        val response = userService.updateUser(updatedUser)

        log.debug { "response attrs=${response.attributes}" }
        log.debug { "consumed capacity=${response.consumedCapacity}" }

        val savedUser = userService.getById(updatedUser.userId)
        log.debug { "saved user=$savedUser" }
        savedUser shouldBeEqualTo updatedUser
        savedUser shouldNotBeEqualTo user
    }

    @Test
    fun `delete user by id`() = runSuspendIO {
        val user = newUser()
        userService.insert(user)
        userService.existsById(user.userId).shouldBeTrue()

        val response = userService.deleteById(user.userId)
        log.debug { "response attrs=${response.attributes}" }
        log.debug { "consumed capacity=${response.consumedCapacity}" }

        userService.existsById(user.userId).shouldBeFalse()
    }
}
