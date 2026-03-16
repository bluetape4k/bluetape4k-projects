package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.Capacity
import io.bluetape4k.support.requirePositiveNumber

/**
 * DSL 블록으로 DynamoDB [Capacity]를 빌드합니다.
 *
 * ## 동작/계약
 * - [capacityUnits], [readCapacityUnits], [writeCapacityUnits]가 null이 아닌 경우 양수여야 하며, 그렇지 않으면 예외를 던진다.
 * - null로 전달된 값은 요청에 포함되지 않는다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val cap = capacityOf(capacityUnits = 5.0, readCapacityUnits = 3.0)
 * // cap.capacityUnits == 5.0
 * // cap.readCapacityUnits == 3.0
 * ```
 *
 * @param capacityUnits 총 소비된 용량 단위 (null이 아니면 양수여야 함)
 * @param readCapacityUnits 읽기 용량 단위 (null이 아니면 양수여야 함)
 * @param writeCapacityUnits 쓰기 용량 단위 (null이 아니면 양수여야 함)
 */
inline fun capacityOf(
    capacityUnits: Double? = null,
    readCapacityUnits: Double? = null,
    writeCapacityUnits: Double? = null,
    @BuilderInference crossinline builder: Capacity.Builder.() -> Unit = {},
): Capacity {
    capacityUnits?.requirePositiveNumber("capacityUnits")
    readCapacityUnits?.requirePositiveNumber("readCapacityUnits")
    writeCapacityUnits?.requirePositiveNumber("writeCapacityUnits")

    return Capacity {
        this.capacityUnits = capacityUnits
        this.readCapacityUnits = readCapacityUnits
        this.writeCapacityUnits = writeCapacityUnits

        builder()
    }
}
