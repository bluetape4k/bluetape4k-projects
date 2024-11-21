package io.bluetape4k.examples.cassandra.multitenancy.keyspace

import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable
import java.time.Instant

@Table("ks_mt_emp")
data class Employee(
    @field:PrimaryKey
    var name: String = "",
    var hireAt: Instant = Instant.EPOCH,
): Serializable
