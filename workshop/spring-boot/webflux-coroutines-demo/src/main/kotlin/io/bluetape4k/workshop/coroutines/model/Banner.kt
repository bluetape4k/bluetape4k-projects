package io.bluetape4k.workshop.coroutines.model

import java.io.Serializable

data class Banner(
    val title: String,
    val message: String,
): Serializable
