package io.bluetape4k.aws.dynamodb.examples.food.tests

import io.bluetape4k.aws.dynamodb.examples.food.AbstractFoodApplicationTest
import io.bluetape4k.aws.dynamodb.examples.food.model.CustomerDocument
import io.bluetape4k.aws.dynamodb.examples.food.model.CustomerGrade
import io.bluetape4k.aws.dynamodb.examples.food.repository.CustomerRepository
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import kotlin.random.Random

class CustomerRepositoryTest: AbstractFoodApplicationTest() {

    companion object: KLoggingChannel()

    @Autowired
    private lateinit var repository: CustomerRepository

    private fun createCustomer(): CustomerDocument =
        CustomerDocument(
            customerId = TimebasedUuid.Reordered.nextIdAsString(),
            nationId = faker.nation().nationality(),
            grade = CustomerGrade.entries[Random.nextInt(CustomerGrade.entries.size)],
            updatedAt = Instant.now()
        )

    @Test
    fun `save one customer and load`() = runSuspendTest(Dispatchers.IO) {
        val customer = createCustomer()
        log.debug { "Save customer. $customer" }
        repository.save(customer)

        val loaded = repository.findByPartitionKey(customer.partitionKey)
        log.debug { "Loaded customer. $loaded" }
        loaded shouldBeEqualTo listOf(customer)
    }
}
