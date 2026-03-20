package io.bluetape4k.aws.dynamodb.model

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.aws.dynamodb.model.DynamoDbEntity.Companion.ENTITY_ID_DELIMITER
import io.bluetape4k.aws.dynamodb.model.DynamoDbEntity.Companion.ENTITY_NAME_DELIMITER
import io.bluetape4k.idgenerators.snowflake.GlobalSnowflake
import io.bluetape4k.idgenerators.uuid.Uuid
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.io.Serializable

/**
 * DynamoDB의 Entity를 표현하는 인터페이스입니다.
 */
interface DynamoDbEntity : Serializable {
    companion object {
        const val ENTITY_ID_DELIMITER = "#"
        const val ENTITY_NAME_DELIMITER = ":"

        val snowflake by lazy { GlobalSnowflake() }
    }

    /**
     * DynamoDB의 파티션 키입니다.
     */
    @get:DynamoDbPartitionKey
    val partitionKey: String

    /**
     * DynamoDB의 정렬 키입니다.
     */
    @get:DynamoDbSortKey
    val sortKey: String

    /**
     * DynamoDB의 [Key] 객체를 표현합니다.
     */
    val key: Key

    /**
     * Long 수형의 Unique 값을 제공합니다.
     */
    fun getUniqueLong(): Long = snowflake.nextId()

    /**
     * UUID를 Base62로 인코딩한 Unique 값을 제공합니다.
     */
    fun getUniqueUuidString(): String = Uuid.V7.nextBase62()
}

/**
 * DynamoDB의 Entity를 표현하는 추상 클래스입니다.
 */
abstract class AbstractDynamoDbEntity :
    AbstractValueObject(),
    DynamoDbEntity {
    override val key: Key by lazy {
        Key
            .builder()
            .partitionValue(partitionKey)
            .sortValue(sortKey)
            .build()
    }

    override fun equalProperties(other: Any): Boolean =
        other is DynamoDbEntity &&
            partitionKey == other.partitionKey &&
            sortKey == other.sortKey

    override fun buildStringHelper(): ToStringBuilder =
        super
            .buildStringHelper()
            .add("partitionKey", partitionKey)
            .add("sortKey", sortKey)
}

/**
 * Entity 타입명·파티션 키·정렬 키를 조합한 키 문자열을 반환합니다.
 *
 * ## 동작/계약
 * - 항상 `T::class.simpleName`을 접두사로 사용한다.
 * - [partitionKey]가 blank가 아니면 `ENTITY_NAME_DELIMITER(:)` 뒤에 추가한다.
 * - [sortKey]가 blank가 아니면 `ENTITY_ID_DELIMITER(#)` 뒤에 추가한다.
 * - null 또는 blank 값은 해당 구분자 없이 생략된다.
 *
 * ```kotlin
 * val key = order.makeKeyString(partitionKey = "user1", sortKey = "order42")
 * // key == "Order:user1#order42"
 *
 * val keyNoSort = order.makeKeyString(partitionKey = "user1")
 * // keyNoSort == "Order:user1"
 * ```
 *
 * @param partitionKey 파티션 키 값 (null 또는 blank이면 생략)
 * @param sortKey 정렬 키 값 (null 또는 blank이면 생략)
 * @return `ClassName[:partitionKey][#sortKey]` 형태의 문자열
 */
inline fun <reified T : DynamoDbEntity> T.makeKeyString(
    partitionKey: Any? = null,
    sortKey: Any? = null,
): String =
    buildString {
        append(T::class.simpleName)

        partitionKey
            ?.takeIf { it.toString().isNotBlank() }
            ?.let {
                append(ENTITY_NAME_DELIMITER)
                append(it)
            }
        sortKey
            ?.takeIf { it.toString().isNotBlank() }
            ?.let {
                append(ENTITY_ID_DELIMITER)
                append(it)
            }
    }
