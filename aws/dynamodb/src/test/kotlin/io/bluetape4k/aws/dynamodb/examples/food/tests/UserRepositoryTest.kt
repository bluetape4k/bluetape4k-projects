package io.bluetape4k.aws.dynamodb.examples.food.tests

import io.bluetape4k.aws.dynamodb.examples.food.AbstractFoodApplicationTest
import io.bluetape4k.aws.dynamodb.examples.food.model.UserDocument
import io.bluetape4k.aws.dynamodb.examples.food.repository.UserRepository
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.random.Random

class UserRepositoryTest: AbstractFoodApplicationTest() {

    @Autowired
    private lateinit var repository: UserRepository

    companion object: KLoggingChannel()

    private fun createUser(): UserDocument {
        val index = Random.nextInt(UserDocument.UserStatus.entries.size)
        val status = UserDocument.UserStatus.entries[index]
        return UserDocument("matrix", TimebasedUuid.nextBase62String(), status)
    }

    @Test
    fun `save item and load`() = runTest {
        val user = createUser()
        repository.save(user)

        val loaded = repository.findByKey(user.key)
        loaded shouldBeEqualTo user
    }

    @Test
    fun `save item and delete`() = runTest {
        val user = createUser()
        repository.save(user)

        val loaded = repository.findByKey(user.key)
        loaded shouldBeEqualTo user

        repository.delete(user)
    }

    @Test
    fun `save item and update`() = runTest {
        val user = createUser()
        repository.save(user)

        val loaded = repository.findByKey(user.key)!!
        loaded shouldBeEqualTo user

        loaded.userStatus = UserDocument.UserStatus.INACTIVE
        val updated = repository.update(loaded)!!

        updated.userStatus shouldBeEqualTo UserDocument.UserStatus.INACTIVE
    }

    @Test
    fun `save many items`() = runTest {
        val users = List(100) { createUser() }

        val saved = repository.saveAll(users).toList()
        saved.all { it.unprocessedPutItemsForTable(repository.table).isEmpty() }.shouldBeTrue()

        val loaded = repository.findFirstByPartitionKey(users.first().partitionKey).toList()
        log.debug { "loaded size=${loaded.size}" }
        loaded.shouldNotBeEmpty()
    }
}
