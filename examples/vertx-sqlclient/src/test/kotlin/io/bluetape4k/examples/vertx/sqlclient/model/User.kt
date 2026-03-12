package io.bluetape4k.examples.vertx.sqlclient.model

import java.io.Serializable

data class User(
    val id: Long,
    val firstName: String,
    val lastName: String,
): Serializable
