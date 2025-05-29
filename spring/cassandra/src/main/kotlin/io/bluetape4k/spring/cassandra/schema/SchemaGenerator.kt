package io.bluetape4k.spring.cassandra.schema

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
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
 * Spring Data Cassandra 용 Entity 정의를 바탕으로 Schema 를 생성합니다.
 *
 * ```
 * @Autowired
 * private val operations: CassandraOperations = uninitialized()
 *
 * SchemaGenerator.truncate<AllPossibleTypes>(operations)
 * SchemaGenerator.potentiallyCreateTableFor<AllPossibleTypes>(operations)
 * ```
 */
object SchemaGenerator: KLoggingChannel() {

    inline fun <reified T: Any> createTableAndTypes(operations: CassandraOperations) {
        createTableAndTypes(operations, T::class)
    }

    fun <T: Any> createTableAndTypes(operations: CassandraOperations, entityKClass: KClass<T>) {
        val persistentEntity = operations.converter.mappingContext.getRequiredPersistentEntity(entityKClass.java)
        val schemaFactory = SchemaFactory(operations.converter)

        potentiallyCreateUdtFor(operations, persistentEntity, schemaFactory)
        potentiallyCreateTableFor(operations, persistentEntity, schemaFactory)
    }

    inline fun <reified T: Any> potentiallyCreateTableFor(operations: CassandraOperations) {
        potentiallyCreateTableFor(operations, T::class)
    }

    fun <T: Any> potentiallyCreateTableFor(operations: CassandraOperations, entityKClass: KClass<T>) {
        val persistentEntity = operations.converter.mappingContext.getRequiredPersistentEntity(entityKClass.java)
        potentiallyCreateTableFor(operations, persistentEntity, SchemaFactory(operations.converter))
    }

    private fun potentiallyCreateTableFor(
        operations: CassandraOperations,
        persistentEntity: CassandraPersistentEntity<*>,
        schemaFactory: SchemaFactory,
    ) {
        operations.cqlOperations.execute(SessionCallback<Any?> { session ->
            val table = session.keyspace
                .flatMap { session.metadata.getKeyspace(it) }
                .flatMap { it.getTable(persistentEntity.tableName) }

            if (!table.isPresent) {
                val tableSpecification = schemaFactory.getCreateTableSpecificationFor(persistentEntity)
                val createCql = CreateTableCqlGenerator(tableSpecification).toCql()
                log.info { "Create table. cql=\n$createCql" }
                operations.cqlOperations.execute(createCql)
            }
        })
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
                    val propertyEntity = when {
                        property.isEmbedded -> EmbeddedEntityOperations(mappingContext).getEntity(property)
                        else -> mappingContext.getRequiredPersistentEntity(property)
                    }
                    log.debug { "property=$property, propertyEntity=$propertyEntity" }
                    potentiallyCreateUdtFor(operations, propertyEntity, schemaFactory)
                }
        }
    }

    inline fun <reified T: Any> truncate(operations: CassandraOperations) {
        truncate(operations, T::class.java)
    }

    fun <T: Any> truncate(operations: CassandraOperations, entityClass: Class<T>) {
        val persistentEntity = operations.converter.mappingContext.getRequiredPersistentEntity(entityClass)
        operations.cqlOperations.execute(
            SessionCallback<Any?> { session ->
                val table = session.keyspace
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
