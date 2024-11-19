package io.bluetape4k.csv.model

import java.io.Serializable

data class ProductType(
    val tagFamily: String,
    val representative: String,
    val synonym: String?,
    val tagType: String?,
    val priority: Int? = null,
    val parentRepresentativeValue: String? = null,
    val level: Int = 0,
): Serializable
