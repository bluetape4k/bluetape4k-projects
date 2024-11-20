package io.bluetape4k.aws.kotlin.dynamodb.examples.user

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.deleteItem
import aws.sdk.kotlin.services.dynamodb.getItem
import aws.sdk.kotlin.services.dynamodb.model.AttributeAction
import aws.sdk.kotlin.services.dynamodb.model.DeleteItemResponse
import aws.sdk.kotlin.services.dynamodb.model.PutItemResponse
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import aws.sdk.kotlin.services.dynamodb.model.ReturnValue
import aws.sdk.kotlin.services.dynamodb.model.UpdateItemResponse
import aws.sdk.kotlin.services.dynamodb.putItem
import aws.sdk.kotlin.services.dynamodb.updateItem
import io.bluetape4k.aws.kotlin.dynamodb.model.attributeValueUpdateOf
import io.bluetape4k.aws.kotlin.dynamodb.model.toAttributeValue
import io.bluetape4k.logging.KLogging

class UserService(private val client: DynamoDbClient) {

    companion object: KLogging()

    suspend fun insert(user: User): PutItemResponse {

        // TODO: Reflection을 이용해 속성명과 값을 매핑하여 item을 생성하도록 할 수 있을 듯 
        val item = mapOf(
            User::userId.name to user.userId.toAttributeValue(),
            User::name.name to user.name.toAttributeValue(),
            User::email.name to user.email.toAttributeValue(),
            User::age.name to user.age.toAttributeValue()
        )
        return client.putItem {
            this.tableName = USER_TABLE_NAME
            this.item = item
            this.returnConsumedCapacity = ReturnConsumedCapacity.Total
            this.returnValues = ReturnValue.AllOld
        }
    }

    suspend fun getById(userId: String): User? {
        val key = mapOf("userId" to userId.toAttributeValue())

        val response = client.getItem {
            this.tableName = USER_TABLE_NAME
            this.key = key
            this.returnConsumedCapacity = ReturnConsumedCapacity.Total
        }
        val item = response.item

        return item?.toUser()
    }

    suspend fun updateUser(user: User): UpdateItemResponse {
        val key = mapOf("userId" to user.userId.toAttributeValue())

        val updateValues = mapOf(
            User::name.name to attributeValueUpdateOf(user.name, AttributeAction.Put),
            User::email.name to attributeValueUpdateOf(user.email, AttributeAction.Put),
            User::age.name to attributeValueUpdateOf(user.age, AttributeAction.Put)
        )

        return client.updateItem {
            tableName = USER_TABLE_NAME
            this.key = key
            attributeUpdates = updateValues

            returnConsumedCapacity = ReturnConsumedCapacity.Total
            returnValues = ReturnValue.AllOld
        }
    }

    suspend fun deleteById(userId: String): DeleteItemResponse {
        val key = mapOf("userId" to userId.toAttributeValue())

        return client.deleteItem {
            tableName = USER_TABLE_NAME
            this.key = key

            returnConsumedCapacity = ReturnConsumedCapacity.Total
            returnValues = ReturnValue.AllOld
        }
    }

    suspend fun existsById(userId: String): Boolean {
        return getById(userId) != null
    }
}
