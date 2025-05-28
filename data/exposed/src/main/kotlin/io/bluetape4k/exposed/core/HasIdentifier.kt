package io.bluetape4k.exposed.core

interface HasIdentifier<ID>: java.io.Serializable {
    val id: ID
}
