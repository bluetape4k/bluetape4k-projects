package io.bluetape4k.examples.cassandra.reactive.people

import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.core.mapping.Table

@Table("coroutine_persons")
data class Person(
    @field:PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 2)
    val firstname: String? = null,

    @field:PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    val lastname: String? = null,

    val age: Int = 0,
)
