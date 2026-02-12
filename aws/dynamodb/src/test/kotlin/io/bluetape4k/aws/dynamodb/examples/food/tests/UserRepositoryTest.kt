package io.bluetape4k.aws.dynamodb.examples.food.tests

import io.bluetape4k.aws.dynamodb.examples.food.AbstractFoodApplicationTest
import io.bluetape4k.aws.dynamodb.examples.food.model.UserDocument
import io.bluetape4k.aws.dynamodb.examples.food.repository.UserRepository
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.uninitialized
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserRepositoryTest: AbstractFoodApplicationTest() {

    companion object: KLoggingChannel() {
        private fun createUser(): UserDocument {
            val status = UserDocument.UserStatus.entries.random()
            return UserDocument(
                serviceId = "matrix",
                userId = TimebasedUuid.Epoch.nextIdAsString(),
                status = status
            )
        }
    }

    @Autowired
    private val repository: UserRepository = uninitialized()

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

        val loaded = repository.findByKey(user.key).shouldNotBeNull()
        loaded shouldBeEqualTo user

        loaded.userStatus = UserDocument.UserStatus.INACTIVE
        val updated = repository.update(loaded).shouldNotBeNull()

        updated.userStatus shouldBeEqualTo UserDocument.UserStatus.INACTIVE
    }

    @Test
    fun `save many items`() = runTest {
        val users = fastList(100) { createUser() }

        val saved = repository.saveAll(users).toList()
        saved.all {
            it.unprocessedPutItemsForTable(repository.table).isEmpty()
        }.shouldBeTrue()

        val loaded = repository.findFirstByPartitionKey(users.first().partitionKey)
        log.debug { "loaded size=${loaded.size}" }
        loaded.shouldNotBeEmpty()
    }
}
