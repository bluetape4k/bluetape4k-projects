package io.bluetape4k.examples.cassandra.event

import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

@Table(value = "event_users")
data class User(
    @field:PrimaryKey var id: Long,
    var firstname: String,
    var lastname: String,
): Serializable
