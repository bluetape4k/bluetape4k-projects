package io.bluetape4k.aws.dynamodb.examples.food.model

import io.bluetape4k.aws.dynamodb.model.makeKeyString
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import java.time.Instant


@DynamoDbBean
class CustomerDocument: AbstractDynamoDocument() {

    companion object: KLogging() {

        @JvmStatic
        operator fun invoke(
            customerId: String,
            nationId: String,
            grade: CustomerGrade = CustomerGrade.NEW,
            updatedAt: Instant? = Instant.now(),
        ): CustomerDocument {
            customerId.requireNotBlank("customerId")
            nationId.requireNotBlank("nationId")

            return CustomerDocument().apply {
                this.customerId = customerId
                this.nationId = nationId
                this.grade = grade
                this.updatedAt = updatedAt
                this.partitionKey = makeKeyString(nationId)
                this.sortKey = makeKeyString(nationId, customerId)
            }
        }
    }

    var customerId: String = ""
    var nationId: String = ""
    var grade: CustomerGrade = CustomerGrade.NEW

}
