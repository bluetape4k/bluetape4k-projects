package io.bluetape4k.examples.cassandra.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

@Table("versioned_entity_oplock")
data class VersionedEntity(
    @field:Id
    val id: Long,

    @field:Version
    val version: Long = 0,

    var name: String? = null,
): Serializable
