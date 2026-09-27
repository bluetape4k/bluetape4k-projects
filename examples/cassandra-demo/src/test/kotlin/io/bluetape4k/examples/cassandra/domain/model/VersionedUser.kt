package io.bluetape4k.examples.cassandra.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

@Table("vusers")
data class VersionedUser(
    @field:Id val id: String = "",
): Serializable {

    constructor(id: String, firstname: String, lastname: String): this(id) {
        this.firstname = firstname
        this.lastname = lastname
    }

    @field:Version
    var version: Long = 0L

    var firstname: String = ""
    var lastname: String = ""
}
