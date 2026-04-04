package io.bluetape4k.aws.dynamodb

/**
 * DynamoDB 상수 및 유틸리티를 제공합니다.
 *
 * ```kotlin
 * val maxSize = DynamoDb.MAX_BATCH_ITEM_SIZE
 * // maxSize == 25
 * ```
 */
object DynamoDb {

    /**
     * DynamoDB의 BatchWriteItem 은 Batch당 최대 25개의 Item만 허용합니다.
     *
     * ```kotlin
     * val chunks = items.chunked(DynamoDb.MAX_BATCH_ITEM_SIZE)
     * // chunks.all { it.size <= 25 } == true
     * ```
     */
    const val MAX_BATCH_ITEM_SIZE = 25

}
