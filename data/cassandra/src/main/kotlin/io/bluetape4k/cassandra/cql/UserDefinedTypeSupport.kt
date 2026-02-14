package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.type.UserDefinedType
import com.datastax.oss.driver.internal.core.type.UserDefinedTypeBuilder
import io.bluetape4k.cassandra.toCqlIdentifier

/**
 * [UserDefinedTypeBuilder]를 사용하여, [UserDefinedType] 을 생성합니다.
 *
 * ```
 * val userType = userDefinedTypeOf("keyspace", "type".asCqlIdentifier()) {
 *    withField("field1", DataTypes.TEXT)
 *    withField("field2", DataTypes.INT)
 *    withField("field3", DataTypes.BOOLEAN)
 *    withField("field4", DataTypes.TIMESTAMP)
 *    withField("field5", DataTypes.UUID)
 * }
 * ```
 *
 * @param keyspaceId 키스페이스 식별자
 * @param typeId 타입 식별자
 * @param builder 초기화 블럭
 */
inline fun userDefinedTypeOf(
    keyspaceId: CqlIdentifier,
    typeId: CqlIdentifier,
    @BuilderInference builder: UserDefinedTypeBuilder.() -> Unit,
): UserDefinedType {
    return UserDefinedTypeBuilder(keyspaceId, typeId).apply(builder).build()
}

/**
 * [UserDefinedTypeBuilder]를 사용하여, [UserDefinedType] 을 생성합니다.
 *
 * ```
 * val userType = userDefinedTypeOf("keyspace", "type") {
 *     withField("field1", DataTypes.TEXT)
 *     withField("field2", DataTypes.INT)
 *     withField("field3", DataTypes.BOOLEAN)
 *     withField("field4", DataTypes.TIMESTAMP)
 *     withField("field5", DataTypes.UUID)
 * }
 * ```
 *
 * @param keyspaceName 키스페이스 이름
 * @param typeName 타입 명
 * @param builder 초기화 블럭
 */
inline fun userDefinedTypeOf(
    keyspaceName: String,
    typeName: String,
    @BuilderInference builder: UserDefinedTypeBuilder.() -> Unit,
): UserDefinedType = userDefinedTypeOf(
    keyspaceName.toCqlIdentifier(),
    typeName.toCqlIdentifier(),
    builder
)
