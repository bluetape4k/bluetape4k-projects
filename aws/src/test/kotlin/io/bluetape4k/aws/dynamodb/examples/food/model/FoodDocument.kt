package io.bluetape4k.aws.dynamodb.examples.food.model

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.aws.dynamodb.model.makeKeyString
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.hashOf
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import java.time.Instant

@Suppress("EqualsOrHashCode")
@DynamoDbBean
class FoodDocument: AbstractDynamoDocument() {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(
            id: String,
            restraurantId: String,
            state: FoodState = FoodState.UNKOWN,
            updatedAt: Instant? = null,
        ): FoodDocument {
            id.requireNotBlank("id")
            restraurantId.requireNotBlank("restruantId")

            return FoodDocument().apply {
                this.id = id
                this.restraurantId = restraurantId
                this.state = state
                this.updatedAt = updatedAt

                this.partitionKey = makeKeyString(restraurantId)
                this.sortKey = makeKeyString(restraurantId, id)
            }
        }
    }

    var id: String = ""
    var restraurantId: String = ""
    var state: FoodState = FoodState.UNKOWN

    override fun equalProperties(other: Any): Boolean {
        return other is FoodDocument && id == other.id && restraurantId == other.restraurantId
    }

    override fun hashCode(): Int = hashOf(id, restraurantId)

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("id", id)
            .add("restraurantId", restraurantId)
            .add("state", state)
            .add("updatedAt", updatedAt)
    }
}
