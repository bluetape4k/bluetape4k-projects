package io.bluetape4k.examples.jpa.querydsl.domain.dto

import com.querydsl.core.annotations.QueryProjection
import java.io.Serializable

data class TeamDto(
    val id: Long?,
    val name: String?,
): Serializable


data class TeamVo @QueryProjection constructor(
    val id: Long?,
    val name: String?,
): Serializable
