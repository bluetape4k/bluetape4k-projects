package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ProvisionedThroughput
import io.bluetape4k.support.requirePositiveNumber

/**
 * DynamoDB [ProvisionedThroughput]을 생성합니다.
 *
 * ## 동작/계약
 * - [readCapacityUnits]가 null이 아닌 경우 양수여야 하며, 그렇지 않으면 예외를 던진다.
 * - [writeCapacityUnits]가 null이 아닌 경우 양수여야 하며, 그렇지 않으면 예외를 던진다.
 * - null로 전달된 값은 요청에 포함되지 않는다.
 *
 * ```kotlin
 * val tp = provisionedThroughputOf(readCapacityUnits = 5L, writeCapacityUnits = 5L)
 * // tp.readCapacityUnits == 5L
 * // tp.writeCapacityUnits == 5L
 * ```
 *
 * @param readCapacityUnits 읽기 용량 단위 (null이 아니면 양수여야 함)
 * @param writeCapacityUnits 쓰기 용량 단위 (null이 아니면 양수여야 함)
 */
fun provisionedThroughputOf(
    readCapacityUnits: Long? = null,
    writeCapacityUnits: Long? = null,
): ProvisionedThroughput {
    readCapacityUnits?.requirePositiveNumber("readCapacityUnits")
    writeCapacityUnits?.requirePositiveNumber("writeCapacityUnits")

    return ProvisionedThroughput {
        this.readCapacityUnits = readCapacityUnits
        this.writeCapacityUnits = writeCapacityUnits
    }
}
