package io.bluetape4k.examples.jpa.querydsl.domain.dto

import com.querydsl.core.annotations.QueryProjection
import io.bluetape4k.examples.jpa.querydsl.domain.mapper.toDto
import io.bluetape4k.examples.jpa.querydsl.domain.model.Member
import io.bluetape4k.examples.jpa.querydsl.domain.model.Team
import java.io.Serializable

data class MemberTeamDto(
    val member: MemberDto,
    val team: TeamDto,
): Serializable {

    constructor(
        memberId: Long,
        memberName: String,
        memberAge: Int,
        teamId: Long?,
        teamName: String?,
    ): this(
        member = MemberDto(memberId, memberName, memberAge),
        team = TeamDto(teamId, teamName)
    )

    /**
     * 이렇게도 가능하지만, 성능 상 좋은 점이 없다
     * 또한, DTO 모듈에 Entity 관련 기능이 포함되어 버린다
     */
    constructor(member: Member, team: Team): this(member.toDto(), team.toDto())
}

data class MemberTeamVo @QueryProjection constructor(
    val member: MemberVo? = null,
    val team: TeamVo? = null,
)
