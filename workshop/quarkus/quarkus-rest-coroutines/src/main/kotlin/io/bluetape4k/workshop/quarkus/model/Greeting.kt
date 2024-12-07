package io.bluetape4k.workshop.quarkus.model

import java.io.Serializable

data class Greeting(
    val message: String = "Hi",
): Serializable
