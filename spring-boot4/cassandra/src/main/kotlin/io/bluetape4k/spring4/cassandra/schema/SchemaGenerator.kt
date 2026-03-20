package io.bluetape4k.spring4.cassandra.schema

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.spring4.cassandra.schema.SchemaGenerator.createTableAndTypes
import io.bluetape4k.spring4.cassandra.schema.SchemaGenerator.potentiallyCreateTableFor
import io.bluetape4k.spring4.cassandra.schema.SchemaGenerator.potentiallyCreateUdtFor
import io.bluetape4k.spring4.cassandra.schema.SchemaGenerator.truncate
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.core.convert.SchemaFactory
import org.springframework.data.cassandra.core.cql.SessionCallback
import org.springframework.data.cassandra.core.cql.generator.CreateTableCqlGenerator
import org.springframework.data.cassandra.core.cql.generator.CreateUserTypeCqlGenerator
import org.springframework.data.cassandra.core.mapping.CassandraPersistentEntity
import org.springframework.data.cassandra.core.mapping.CassandraPersistentProperty
import org.springframework.data.cassandra.core.mapping.EmbeddedEntityOperations
import kotlin.reflect.KClass

/**
 * 매핑 메타데이터를 기준으로 Cassandra UDT/테이블 생성과 truncate를 수행하는 유틸리티입니다.
 *
 * ## 동작/계약
 * - [createTableAndTypes]는 대상 엔티티의 UDT를 먼저 생성한 뒤 테이블 생성을 시도합니다.
 * - 테이블/UDT가 이미 존재하면 생성 CQL을 실행하지 않습니다.
 * - [truncate]는 대상 테이블이 실제로 존재할 때만 `TRUNCATE`를 실행합니다.
 *
 * ```kotlin
 * SchemaGenerator.truncate<AllPossibleTypes>(operations)
 * SchemaGenerator.createTableAndTypes<AllPossibleTypes>(operations)
 * // result == "table ready"
 * ```
 */
object SchemaGenerator: KLoggingChannel() {
    /**
     * 제네릭 엔티티 타입의 UDT와 테이블 생성을 수행합니다.
     *
     * ## 동작/계약
     * - `T::class`를 사용해 [createTableAndTypes] 오버로드에 위임합니다.
     *
     * ```kotlin
     * SchemaGenerator.createTableAndTypes<AllPossibleTypes>(operations)
     * // result == "schema checked"
     * ```
     */
    inline fun <reified T: Any> createTableAndTypes(operations: CassandraOperations) {
        createTableAndTypes(operations, T::class)
    }

    /**
     * 지정한 엔티티 클래스의 UDT와 테이블 생성을 수행합니다.
     *
     * ## 동작/계약
     * - 매핑 컨텍스트에서 엔티티 메타데이터를 조회하고 [SchemaFactory]를 생성합니다.
     * - UDT 생성([potentiallyCreateUdtFor]) 이후 테이블 생성([potentiallyCreateTableFor]) 순서로 실행합니다.
     * - 엔티티 메타데이터가 없으면 `getRequiredPersistentEntity` 예외가 발생합니다.
     *
     * ```kotlin
     * SchemaGenerator.createTableAndTypes(operations, AllPossibleTypes::class)
     * // result == "schema checked"
     * ```
     */
    fun <T: Any> createTableAndTypes(
        operations: CassandraOperations,
        entityKClass: KClass<T>,
    ) {
        val persistentEntity = operations.converter.mappingContext.getRequiredPersistentEntity(entityKClass.java)
        val schemaFactory = SchemaFactory(operations.converter)

        potentiallyCreateUdtFor(operations, persistentEntity, schemaFactory)
        potentiallyCreateTableFor(operations, persistentEntity, schemaFactory)
    }

    /**
     * 제네릭 엔티티 타입의 테이블 생성을 필요할 때만 수행합니다.
     *
     * ## 동작/계약
     * - `T::class`를 사용해 [potentiallyCreateTableFor] 오버로드에 위임합니다.
     *
     * ```kotlin
     * SchemaGenerator.potentiallyCreateTableFor<AllPossibleTypes>(operations)
     * // result == "table checked"
     * ```
     */
    inline fun <reified T: Any> potentiallyCreateTableFor(operations: CassandraOperations) {
        potentiallyCreateTableFor(operations, T::class)
    }

    /**
     * 지정한 엔티티 클래스의 테이블 생성을 필요할 때만 수행합니다.
     *
     * ## 동작/계약
     * - 매핑 컨텍스트에서 엔티티를 조회한 뒤 private 오버로드로 위임합니다.
     * - 엔티티 메타데이터가 없으면 `getRequiredPersistentEntity` 예외가 발생합니다.
     *
     * ```kotlin
     * SchemaGenerator.potentiallyCreateTableFor(operations, AllPossibleTypes::class)
     * // result == "table checked"
     * ```
     */
    fun <T: Any> potentiallyCreateTableFor(
        operations: CassandraOperations,
        entityKClass: KClass<T>,
    ) {
        val persistentEntity = operations.converter.mappingContext.getRequiredPersistentEntity(entityKClass.java)
        potentiallyCreateTableFor(operations, persistentEntity, SchemaFactory(operations.converter))
    }

    private fun potentiallyCreateTableFor(
        operations: CassandraOperations,
        persistentEntity: CassandraPersistentEntity<*>,
        schemaFactory: SchemaFactory,
    ) {
        operations.cqlOperations.execute(
            SessionCallback<Any?> { session ->
                val table =
                    session.keyspace
                        .flatMap { session.metadata.getKeyspace(it) }
                        .flatMap { it.getTable(persistentEntity.tableName) }

                if (!table.isPresent) {
                    val tableSpecification = schemaFactory.getCreateTableSpecificationFor(persistentEntity)
                    val createCql = CreateTableCqlGenerator(tableSpecification).toCql()
                    log.info { "Create table. cql=\n$createCql" }
                    operations.cqlOperations.execute(createCql)
                }
            }
        )
    }

    private fun potentiallyCreateUdtFor(
        operations: CassandraOperations,
        persistentEntity: CassandraPersistentEntity<*>,
        schemaFactory: SchemaFactory,
    ) {
        if (persistentEntity.isUserDefinedType) {
            val udtSpec = schemaFactory.getCreateUserTypeSpecificationFor(persistentEntity).ifNotExists()
            operations.cqlOperations.execute(CreateUserTypeCqlGenerator.toCql(udtSpec))
        } else {
            val mappingContext = operations.converter.mappingContext
            persistentEntity
                .filterNot { it.isEntity }
                .forEach { property: CassandraPersistentProperty ->
                    val propertyEntity =
                        when {
                            property.isEmbedded -> EmbeddedEntityOperations(mappingContext).getEntity(property)
                            else -> mappingContext.getRequiredPersistentEntity(property)
                        }
                    log.debug { "property=$property, propertyEntity=$propertyEntity" }
                    potentiallyCreateUdtFor(operations, propertyEntity, schemaFactory)
                }
        }
    }

    /**
     * 제네릭 엔티티 타입 테이블을 존재할 때만 truncate 합니다.
     *
     * ## 동작/계약
     * - `T::class.java`를 사용해 [truncate] 오버로드에 위임합니다.
     *
     * ```kotlin
     * SchemaGenerator.truncate<AllPossibleTypes>(operations)
     * // result == "table truncated if exists"
     * ```
     */
    inline fun <reified T: Any> truncate(operations: CassandraOperations) {
        truncate(operations, T::class.java)
    }

    /**
     * 지정한 엔티티 클래스의 테이블을 존재할 때만 truncate 합니다.
     *
     * ## 동작/계약
     * - 세션 메타데이터로 테이블 존재 여부를 확인한 뒤 존재할 때만 `operations.truncate(entityClass)`를 실행합니다.
     * - 테이블이 없으면 아무 작업도 수행하지 않습니다.
     *
     * ```kotlin
     * SchemaGenerator.truncate(operations, AllPossibleTypes::class.java)
     * // result == "truncate attempted"
     * ```
     */
    fun <T: Any> truncate(
        operations: CassandraOperations,
        entityClass: Class<T>,
    ) {
        val persistentEntity = operations.converter.mappingContext.getRequiredPersistentEntity(entityClass)
        operations.cqlOperations.execute(
            SessionCallback<Any?> { session ->
                val table =
                    session.keyspace
                        .flatMap { session.metadata.getKeyspace(it) }
                        .flatMap { it.getTable(persistentEntity.tableName) }

                if (table.isPresent) {
                    log.info { "Truncate table for entity[${entityClass.name}]" }
                    operations.truncate(entityClass)
                }
            }
        )
    }
}
