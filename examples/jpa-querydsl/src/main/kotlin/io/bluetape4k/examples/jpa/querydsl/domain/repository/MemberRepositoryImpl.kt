package io.bluetape4k.examples.jpa.querydsl.domain.repository

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import io.bluetape4k.examples.jpa.querydsl.domain.dto.MemberSearchCondition
import io.bluetape4k.examples.jpa.querydsl.domain.dto.MemberTeamDto
import io.bluetape4k.examples.jpa.querydsl.domain.model.Member
import io.bluetape4k.examples.jpa.querydsl.domain.model.QMember
import io.bluetape4k.examples.jpa.querydsl.domain.model.QTeam
import io.bluetape4k.logging.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport

class MemberRepositoryImpl: QuerydslRepositorySupport(Member::class.java), MemberRepositoryCustom {

    companion object: KLogging()

    private val queryFactory get() = JPAQueryFactory(entityManager)

    private val qmember = QMember.member
    private val qteam = QTeam.team

    override fun search(condition: MemberSearchCondition): List<MemberTeamDto> {
        // Projections.constructor 를 이용하여 DTO를 바로 제공한다
        val projection = Projections.constructor(
            MemberTeamDto::class.java,
            qmember.id,
            qmember.name,
            qmember.age,
            qteam.id,
            qteam.name
        )

        val whereClauses = listOfNotNull(
            condition.memberName?.let { qmember.name.eq(it) },
            condition.teamName?.let { qteam.name.eq(it) },
            condition.ageGoe?.let { qmember.age.goe(it) },
            condition.ageLoe?.let { qmember.age.loe(it) },
        )

        return queryFactory
            .select(projection)
            .from(qmember)
            .leftJoin(qmember.team(), qteam)
            .where(*whereClauses.toTypedArray())
            .fetch()
    }

    //    fun List<Tuple>.toMemberTeamDto(): List<MemberTeamDto> {
    //        return map { tuple ->
    //            MemberTeamDto(
    //                member = MemberDto(tuple.get(qmember.id)!!, tuple.get(qmember.name)!!, tuple.get(qmember.age)!!),
    //                team = TeamDto(tuple.get(qteam.id) ?: 0, tuple.get(qteam.name) ?: "")
    //            )
    //        }
    //    }


    override fun searchPageSimple(condition: MemberSearchCondition, pageable: Pageable): Page<MemberTeamDto> {
        TODO("Not yet implemented")
    }

    override fun searchPageComplex(condition: MemberSearchCondition, pageable: Pageable): Page<MemberTeamDto> {
        TODO("Not yet implemented")
    }

    override fun searchPageExtremeCountQuery(
        condition: MemberSearchCondition,
        pageable: Pageable,
    ): Page<MemberTeamDto> {
        TODO("Not yet implemented")
    }
}
