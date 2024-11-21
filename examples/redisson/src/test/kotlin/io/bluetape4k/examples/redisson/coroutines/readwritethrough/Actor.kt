package io.bluetape4k.examples.redisson.coroutines.readwritethrough

import java.io.Serializable

data class Actor(
    val id: Int,
    val firstname: String,
    val lastname: String,
): Serializable
