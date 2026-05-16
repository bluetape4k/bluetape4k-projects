package io.bluetape4k.examples.jpa.blazepersistence.domain.dto

import java.io.Serializable

/**
 * Search condition for the Blaze Persistence member examples.
 */
data class MemberSearchCondition(
    val memberName: String? = null,
    val teamName: String? = null,
    val ageGoe: Int? = null,
    val ageLoe: Int? = null,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
