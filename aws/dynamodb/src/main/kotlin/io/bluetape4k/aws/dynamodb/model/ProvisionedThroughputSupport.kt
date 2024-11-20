package io.bluetape4k.aws.dynamodb.model

import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput

/**
 * DynamoDB 테이블의 프로비저닝된 처리량 설정을 생성합니다.
 *
 * ```
 * val throughput = provisionedThroughput {
 *    readCapacityUnits(10)
 *    writeCapacityUnits(5)
 * }
 * ```
 *
 * @return [ProvisionedThroughput] 인스턴스
 */
inline fun ProvisionedThroughput(
    initializer: ProvisionedThroughput.Builder.() -> Unit,
): ProvisionedThroughput {
    return ProvisionedThroughput.builder().apply(initializer).build()
}

/**
 * DynamoDB 테이블의 프로비저닝된 처리량 설정을 생성합니다.
 *
 * ```
 * val throughput = provisionedThroughputOf(10, 5)
 * ```
 *
 * @param readCapacityUnits 읽기 처리량
 * @param writeCapacityUnits 쓰기 처리량
 *
 * @return [ProvisionedThroughput] 인스턴스
 */
fun provisionedThroughputOf(
    readCapacityUnits: Long? = null,
    writeCapacityUnits: Long? = null,
): ProvisionedThroughput = ProvisionedThroughput {
    readCapacityUnits(readCapacityUnits)
    writeCapacityUnits(writeCapacityUnits)
}
