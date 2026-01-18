package io.bluetape4k.aws.dynamodb.examples.food.repository

import io.bluetape4k.aws.dynamodb.enhanced.table
import io.bluetape4k.aws.dynamodb.examples.food.model.Schema
import io.bluetape4k.aws.dynamodb.examples.food.model.UserDocument
import io.bluetape4k.aws.dynamodb.repository.DynamoDbCoroutineRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient

@Repository
class UserRepository(
    @param:Autowired override val client: DynamoDbEnhancedAsyncClient,
    @Value($$"${aws.dynamodb.tablePrefix:local-}") tablePrefix: String,
): DynamoDbCoroutineRepository<UserDocument> {

    companion object: KLoggingChannel()

    override val itemClass: Class<UserDocument> = UserDocument::class.java

    override val table: DynamoDbAsyncTable<UserDocument> by lazy {
        client.table("$tablePrefix${Schema.TABLE_NAME}")
    }
}
