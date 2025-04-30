package io.bluetape4k.exposed.dao

import java.io.Serializable

interface HasIdentifier<ID>: Serializable {
    val id: ID
}
