package io.bluetape4k.spring.cassandra.convert.model

import org.springframework.data.cassandra.core.mapping.CassandraType
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

@Table
data class TimeEntity(
    @field:PrimaryKey
    val id: String = "",

    @field:CassandraType(type = CassandraType.Name.TIME)
    var time: Long,
): Serializable
