package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.type.UserDefinedType
import com.datastax.oss.driver.internal.core.type.UserDefinedTypeBuilder

/**
 * [UserDefinedTypeBuilder]를 사용하여, [UserDefinedType] 을 생성합니다.
 *
 * ```
 * val userType = userDefinedType("keyspace", "type".asCqlIdentifier()) {
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
 * @param initializer 초기화 블럭
 */
inline fun userDefinedType(
    keyspaceId: CqlIdentifier,
    typeId: CqlIdentifier,
    @BuilderInference initializer: UserDefinedTypeBuilder.() -> Unit,
): UserDefinedType {
    return UserDefinedTypeBuilder(keyspaceId, typeId).apply(initializer).build()
}

/**
 * [UserDefinedTypeBuilder]를 사용하여, [UserDefinedType] 을 생성합니다.
 *
 * ```
 * val userType = userDefinedType("keyspace", "type") {
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
 * @param initializer 초기화 블럭
 */
inline fun userDefinedType(
    keyspaceName: String,
    typeName: String,
    @BuilderInference initializer: UserDefinedTypeBuilder.() -> Unit,
): UserDefinedType {
    return UserDefinedTypeBuilder(keyspaceName, typeName).apply(initializer).build()
}
