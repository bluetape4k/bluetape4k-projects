package io.bluetape4k.examples.cassandra.multitenancy.keyspace

import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import io.bluetape4k.cassandra.querybuilder.eq
import io.bluetape4k.cassandra.querybuilder.literal
import io.bluetape4k.support.toOptional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.repository.query.CassandraEntityInformation
import org.springframework.data.cassandra.repository.support.SimpleCassandraRepository
import java.io.Serializable
import java.util.*

class KeyspaceAwareCassandraRepository<T: Any, ID: Serializable>(
    private val metadata: CassandraEntityInformation<T, ID>,
    private val operations: CassandraOperations,
): SimpleCassandraRepository<T, ID>(metadata, operations) {

    @Autowired
    private lateinit var tenantId: TenantId

    override fun findById(id: ID): Optional<T> {
        val primaryKey = operations.converter
            .mappingContext
            .getPersistentEntity(metadata.javaClass)
            ?.idProperty
            ?.columnName!!

        val select = QueryBuilder
            .selectFrom(tenantId.get(), metadata.tableName.asCql(true))
            .all()
            .whereColumn(primaryKey.asCql(true)).eq(id.literal())
            .build()

        return operations.selectOne(select, metadata.javaType).toOptional()
    }
}
