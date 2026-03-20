package io.bluetape4k.spring.cassandra.domain.model

import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

@Table
data class TypeWithKeyClass(
    @field:PrimaryKey
    val key: CompositeKey? = null,
): Serializable
