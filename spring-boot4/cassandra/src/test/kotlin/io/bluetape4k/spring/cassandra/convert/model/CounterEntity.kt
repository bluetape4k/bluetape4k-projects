package io.bluetape4k.spring.cassandra.convert.model

import org.springframework.data.cassandra.core.mapping.CassandraType
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

@Table
data class CounterEntity(
    @field:PrimaryKey
    val id: String = "",

    @field:CassandraType(type = CassandraType.Name.COUNTER)
    var coount: Long = 0L,
): Serializable
