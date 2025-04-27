package io.bluetape4k.exposed.repository

import java.io.Serializable

interface HasIdentifier<ID>: Serializable {
    val id: ID
}
