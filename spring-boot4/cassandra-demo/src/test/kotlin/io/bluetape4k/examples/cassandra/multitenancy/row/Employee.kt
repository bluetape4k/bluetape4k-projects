package io.bluetape4k.examples.cassandra.multitenancy.row

import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable
import java.time.Instant

@Table("row_mt_emp")
data class Employee(
    @field:PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED)
    val tenantId: String,
    @field:PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED)
    var name: String,
    var hireAt: Instant = Instant.EPOCH,
): Serializable
