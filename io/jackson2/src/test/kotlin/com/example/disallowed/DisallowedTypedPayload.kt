package com.example.disallowed

import java.io.Serializable

internal data class DisallowedTypedPayload(
    val value: String,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = -4314960917682563259L
    }
}
