package io.bluetape4k.aws.dynamodb.examples.food.config

import io.bluetape4k.aws.dynamodb.examples.food.model.DynamoDbTableCreator
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

@Component
class DynamoDbSetupAfterStartupListener(
    private val dynamoDbTableCreator: DynamoDbTableCreator,
): ApplicationListener<ContextRefreshedEvent> {

    companion object: KLoggingChannel()

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        runBlocking(Dispatchers.IO) {
            dynamoDbTableCreator.createTable()
        }
    }
}
