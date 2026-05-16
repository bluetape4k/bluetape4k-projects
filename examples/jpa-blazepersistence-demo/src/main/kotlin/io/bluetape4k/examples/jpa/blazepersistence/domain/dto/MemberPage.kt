package io.bluetape4k.examples.jpa.blazepersistence.domain.dto

import com.blazebit.persistence.KeysetPage
import java.io.Serializable

/**
 * Stable page result used by the Blaze Persistence keyset examples.
 */
data class MemberPage<T>(
    val content: List<T>,
    val totalSize: Long,
    val totalPages: Int,
    val firstResult: Int,
    val maxResults: Int,
    val keysetPage: KeysetPage?,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
