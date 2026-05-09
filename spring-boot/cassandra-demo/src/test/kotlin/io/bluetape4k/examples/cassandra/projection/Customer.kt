package io.bluetape4k.examples.cassandra.projection

import org.springframework.data.annotation.Id
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

@Table
data class Customer(
    @field:Id val id: String = "",
    var firstname: String = "",
    var lastname: String = "",
): Serializable
