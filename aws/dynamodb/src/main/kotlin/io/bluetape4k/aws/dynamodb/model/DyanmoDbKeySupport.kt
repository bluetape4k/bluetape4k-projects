package io.bluetape4k.aws.dynamodb.model

import io.bluetape4k.aws.core.toSdkBytes
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

/**
 * DynamoDB의 [Key]를 생성합니다.
 *
 * ```
 * val key = Key {
 *  partitionValue("Hello, World!")
 *  sortValue(42)
 * }
 * ```
 * @param initializer [Key.Builder]를 초기화하는 람다 함수입니다.
 * @return [Key] 객체를 반환합니다.
 */
inline fun Key(initializer: Key.Builder.() -> Unit): Key {
    return Key.builder().apply(initializer).build()
}

/**
 * DynamoDB의 [Key]를 생성합니다.
 *
 * ```
 * val key = dynamoDbKeyOf("Hello, World!", 42)
 * ```
 *
 * @param partitionKey 파티션 키의 값입니다.
 * @param sortValue 정렬 키의 값입니다.
 *
 * @return [Key] 객체를 반환합니다.
 */
fun dynamoDbKeyOf(partitionKey: AttributeValue, sortValue: AttributeValue? = null): Key =
    Key {
        partitionValue(partitionKey)
        sortValue(sortValue)
    }

/**
 * DynamoDB의 [Key]를 생성합니다.
 *
 * ```
 * val key = dynamoDbKeyOf("Hello, World!", 42)
 * ```
 *
 * @param partitionValue 파티션 키에 해당하는 값입니다. (내부적으로 [AttributeValue]로 변환됩니다.)
 * @param sortValue 정렬 키의 값입니다.
 *
 * @return [Key] 객체를 반환합니다.
 */
fun dynamoDbKeyOf(partitionValue: Any, sortValue: Any? = null): Key =
    Key {
        when (partitionValue) {
            is Number    -> partitionValue(partitionValue)
            is ByteArray -> partitionValue(partitionValue.toSdkBytes())
            else         -> partitionValue(partitionValue.toString())
        }
        sortValue?.let {
            when (sortValue) {
                is Number    -> sortValue(sortValue)
                is ByteArray -> sortValue(sortValue.toSdkBytes())
                else         -> sortValue(sortValue.toString())
            }
        }
    }
