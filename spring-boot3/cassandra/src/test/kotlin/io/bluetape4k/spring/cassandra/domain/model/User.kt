package io.bluetape4k.spring.cassandra.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

@Table("users")
data class User(
    @field:Id var
    id: String = "",

    var firstname: String? = null,
    var lastname: String? = null,
): Serializable
