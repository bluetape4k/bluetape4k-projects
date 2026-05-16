package io.bluetape4k.examples.jpa.querydsl.domain.repository

import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import io.bluetape4k.examples.jpa.querydsl.domain.dto.MemberSearchCondition
import io.bluetape4k.examples.jpa.querydsl.domain.dto.MemberTeamDto
import io.bluetape4k.examples.jpa.querydsl.domain.model.Member
import io.bluetape4k.examples.jpa.querydsl.domain.model.QMember
import io.bluetape4k.examples.jpa.querydsl.domain.model.QTeam
import io.bluetape4k.logging.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.data.support.PageableExecutionUtils

class MemberRepositoryImpl: QuerydslRepositorySupport(Member::class.java), MemberRepositoryCustom {

    companion object: KLogging()

    private val queryFactory get() = JPAQueryFactory(entityManager)

    private val qmember = QMember.member
    private val qteam = QTeam.team

    override fun search(condition: MemberSearchCondition): List<MemberTeamDto> {
        return selectMemberTeams(condition).fetch()
    }

    override fun searchPageSimple(condition: MemberSearchCondition, pageable: Pageable): Page<MemberTeamDto> {
        val content = fetchPageContent(condition, pageable)
        return PageImpl(content, pageable, countMembers(condition))
    }

    override fun searchPageComplex(condition: MemberSearchCondition, pageable: Pageable): Page<MemberTeamDto> {
        val content = fetchPageContent(condition, pageable)
        return PageableExecutionUtils.getPage(content, pageable) { countMembers(condition) }
    }

    override fun searchPageExtremeCountQuery(
        condition: MemberSearchCondition,
        pageable: Pageable,
    ): Page<MemberTeamDto> {
        val content = fetchPageContent(condition, pageable)
        return PageableExecutionUtils.getPage(content, pageable) { countMembersExtreme(condition) }
    }

    private fun selectMemberTeams(condition: MemberSearchCondition): JPAQuery<MemberTeamDto> {
        // Projections.constructor 를 이용하여 DTO를 바로 제공한다
        val projection = Projections.constructor(
            MemberTeamDto::class.java,
            qmember.id,
            qmember.name,
            qmember.age,
            qteam.id,
            qteam.name,
        )

        return queryFactory
            .select(projection)
            .from(qmember)
            .leftJoin(qmember.team(), qteam)
            .where(*whereClauses(condition))
    }

    private fun fetchPageContent(condition: MemberSearchCondition, pageable: Pageable): List<MemberTeamDto> {
        return selectMemberTeams(condition)
            .applyPageable(pageable)
            .fetch()
    }

    private fun countMembers(condition: MemberSearchCondition): Long {
        return queryFactory
            .select(qmember.count())
            .from(qmember)
            .leftJoin(qmember.team(), qteam)
            .where(*whereClauses(condition))
            .fetchOne()
            ?: 0L
    }

    private fun countMembersExtreme(condition: MemberSearchCondition): Long {
        if (condition.teamName == null) {
            return queryFactory
                .select(qmember.count())
                .from(qmember)
                .where(*memberWhereClauses(condition))
                .fetchOne()
                ?: 0L
        }

        return countMembers(condition)
    }

    private fun whereClauses(condition: MemberSearchCondition): Array<BooleanExpression> {
        return listOfNotNull(
            *memberWhereClauses(condition),
            condition.teamName?.let { qteam.name.eq(it) },
        ).toTypedArray()
    }

    private fun memberWhereClauses(condition: MemberSearchCondition): Array<BooleanExpression> {
        return listOfNotNull(
            condition.memberName?.let { qmember.name.eq(it) },
            condition.ageGoe?.let { qmember.age.goe(it) },
            condition.ageLoe?.let { qmember.age.loe(it) },
        ).toTypedArray()
    }

    private fun JPAQuery<MemberTeamDto>.applyPageable(pageable: Pageable): JPAQuery<MemberTeamDto> {
        pageable.sort.forEach { order ->
            orderBy(order.toOrderSpecifier())
        }

        return if (pageable.isPaged) {
            offset(pageable.offset).limit(pageable.pageSize.toLong())
        } else {
            this
        }
    }

    private fun Sort.Order.toOrderSpecifier(): OrderSpecifier<*> {
        val direction = if (isAscending) Order.ASC else Order.DESC
        return when (property) {
            "id", "member.id"     -> OrderSpecifier(direction, qmember.id)
            "name", "member.name" -> OrderSpecifier(direction, qmember.name)
            "age", "member.age"   -> OrderSpecifier(direction, qmember.age)
            "team.id"             -> OrderSpecifier(direction, qteam.id)
            "team.name"           -> OrderSpecifier(direction, qteam.name)
            else                  -> throw IllegalArgumentException("Unsupported member search sort property: $property")
        }
    }
}
