package io.bluetape4k.aws.dynamodb.examples.food.model

import io.bluetape4k.aws.dynamodb.model.makeKeyString
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import java.time.Instant

@DynamoDbBean
class UserDocument: AbstractDynamoDocument() {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(
            serviceId: String,
            userId: String,
            status: UserStatus = UserStatus.UNKNOWN,
        ): UserDocument {
            serviceId.requireNotBlank("serviceId")
            userId.requireNotBlank("userId")

            return UserDocument().apply {
                this.serviceId = serviceId
                this.userId = userId
                this.userStatus = status
                this.updatedAt = Instant.now()

                this.partitionKey = makeKeyString(serviceId)
                this.sortKey = makeKeyString(serviceId, userId)
            }
        }
    }

    var serviceId: String = ""
    var userId: String = ""
    var userStatus: UserStatus = UserStatus.UNKNOWN

    enum class UserStatus {
        UNKNOWN,
        ACTIVE,
        INACTIVE,
        ABANDON,
        DELETED
    }
}
