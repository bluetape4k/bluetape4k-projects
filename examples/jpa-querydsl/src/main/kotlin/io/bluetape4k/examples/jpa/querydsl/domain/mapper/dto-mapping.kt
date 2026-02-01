package io.bluetape4k.examples.jpa.querydsl.domain.mapper

import io.bluetape4k.examples.jpa.querydsl.domain.dto.MemberDto
import io.bluetape4k.examples.jpa.querydsl.domain.dto.TeamDto
import io.bluetape4k.examples.jpa.querydsl.domain.model.Member
import io.bluetape4k.examples.jpa.querydsl.domain.model.Team

fun Member.toDto(): MemberDto =
    MemberDto(
        id = this.id ?: 0L,
        name = this.name,
        age = this.age ?: 0
    )

fun Team.toDto(): TeamDto =
    TeamDto(
        id = this.id ?: 0L,
        name = this.name
    )
