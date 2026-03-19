package io.bluetape4k.aws.dynamodb

/**
 * Constants and utility functions for DynamoDB.
 */
object DynamoDb {

    /**
     * DynamoDB의 BatchWriteItem 은 Batch당 최대 25개의 Item만 허용합니다.
     */
    const val MAX_BATCH_ITEM_SIZE = 25

}
