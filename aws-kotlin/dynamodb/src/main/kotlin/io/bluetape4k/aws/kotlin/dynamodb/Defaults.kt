package io.bluetape4k.aws.kotlin.dynamodb

/**
 * DynamoDb를 위한 상수와 유틸리티 함수를 제공합니다.
 */
object Defaults {

    /**
     * DynamoDB의 BatchWriteItem 은 Batch당 최대 25개의 Item만 허용합니다.
     */
    const val MAX_BATCH_ITEM_SIZE = 25
}
