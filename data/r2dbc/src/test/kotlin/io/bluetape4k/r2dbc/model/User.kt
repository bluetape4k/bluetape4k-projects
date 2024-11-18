package io.bluetape4k.r2dbc.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.io.Serializable
import java.time.OffsetDateTime

@Table("users")
data class User(
    val username: String,
    val password: String,
    val name: String,
    val description: String? = null,
    val createdAt: OffsetDateTime? = null,
    val active: Boolean? = null,

    @Id
    val userId: Long? = null,
): Serializable
