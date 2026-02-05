package io.bluetape4k.aws.dynamodb.examples.food.tests

import io.bluetape4k.aws.dynamodb.examples.food.AbstractFoodApplicationTest
import io.bluetape4k.aws.dynamodb.examples.food.model.CustomerDocument
import io.bluetape4k.aws.dynamodb.examples.food.model.CustomerGrade
import io.bluetape4k.aws.dynamodb.examples.food.repository.CustomerRepository
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class CustomerRepositoryTest: AbstractFoodApplicationTest() {

    companion object: KLoggingChannel() {
        private fun createCustomer(): CustomerDocument =
            CustomerDocument(
                customerId = TimebasedUuid.Epoch.nextIdAsString(),
                nationId = faker.nation().nationality(),
                grade = CustomerGrade.entries.random(),
                updatedAt = Instant.now()
            )
    }

    @Autowired
    private val repository: CustomerRepository = uninitialized()

    @Test
    fun `save one customer and load`() = runSuspendIO {
        val customer = createCustomer()
        log.debug { "Save customer. $customer" }
        repository.save(customer)

        val loaded = repository.findByPartitionKey(customer.partitionKey)
        log.debug { "Loaded customer. $loaded" }
        loaded shouldBeEqualTo listOf(customer)
    }
}
