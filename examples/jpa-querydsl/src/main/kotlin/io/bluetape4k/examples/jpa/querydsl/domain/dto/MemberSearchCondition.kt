package io.bluetape4k.examples.jpa.querydsl.domain.dto

import java.io.Serializable

data class MemberSearchCondition(
    val memberName: String? = null,
    val teamName: String? = null,
    val ageGoe: Int? = null,
    val ageLoe: Int? = null,
): Serializable
