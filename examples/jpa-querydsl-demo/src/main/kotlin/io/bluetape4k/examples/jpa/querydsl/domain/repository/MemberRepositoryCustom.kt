package io.bluetape4k.examples.jpa.querydsl.domain.repository

import io.bluetape4k.examples.jpa.querydsl.domain.dto.MemberSearchCondition
import io.bluetape4k.examples.jpa.querydsl.domain.dto.MemberTeamDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface MemberRepositoryCustom {

    fun search(condition: MemberSearchCondition): List<MemberTeamDto>

    fun searchPageSimple(condition: MemberSearchCondition, pageable: Pageable): Page<MemberTeamDto>

    fun searchPageComplex(condition: MemberSearchCondition, pageable: Pageable): Page<MemberTeamDto>

    fun searchPageExtremeCountQuery(condition: MemberSearchCondition, pageable: Pageable): Page<MemberTeamDto>

}
