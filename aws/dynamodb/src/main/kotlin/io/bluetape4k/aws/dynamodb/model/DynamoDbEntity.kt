package io.bluetape4k.aws.dynamodb.model

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.aws.dynamodb.model.DynamoDbEntity.Companion.ENTITY_ID_DELIMITER
import io.bluetape4k.aws.dynamodb.model.DynamoDbEntity.Companion.ENTITY_NAME_DELIMITER
import io.bluetape4k.idgenerators.snowflake.GlobalSnowflake
import io.bluetape4k.idgenerators.uuid.TimebasedUuidGenerator
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.io.Serializable

/**
 * DynamoDB의 Entity를 표현하는 인터페이스입니다.
 */
interface DynamoDbEntity: Serializable {

    companion object {
        const val ENTITY_ID_DELIMITER = "#"
        const val ENTITY_NAME_DELIMITER = ":"

        val uuidGenerator by lazy { TimebasedUuidGenerator() }
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
    fun getUniqueUuidString(): String = uuidGenerator.nextBase62String()
}

/**
 * DynamoDB의 Entity를 표현하는 추상 클래스입니다.
 */
abstract class AbstractDynamoDbEntity: AbstractValueObject(), DynamoDbEntity {

    override val key: Key by lazy {
        Key.builder()
            .partitionValue(partitionKey)
            .sortValue(sortKey)
            .build()
    }

    override fun equalProperties(other: Any): Boolean {
        return other is DynamoDbEntity &&
                partitionKey == other.partitionKey &&
                sortKey == other.sortKey
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("partitionKey", partitionKey)
            .add("sortKey", sortKey)
    }
}

/**
 * DynamoDB의 Entity 에 키의 정보를 문자열로 표현합니다.
 */
inline fun <reified T: DynamoDbEntity> T.makeKeyString(
    partitionKey: Any? = null,
    sortKey: Any? = null,
): String = buildString {
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
