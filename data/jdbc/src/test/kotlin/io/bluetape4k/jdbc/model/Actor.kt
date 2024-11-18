package io.bluetape4k.jdbc.model

import java.io.Serializable

data class Actor(
    val id: Int,
    val firstname: String,
    val lastname: String,
): Serializable
