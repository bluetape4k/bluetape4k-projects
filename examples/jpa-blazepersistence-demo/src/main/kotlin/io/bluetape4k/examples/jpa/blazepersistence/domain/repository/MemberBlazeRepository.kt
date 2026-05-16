package io.bluetape4k.examples.jpa.blazepersistence.domain.repository

import com.blazebit.persistence.CriteriaBuilder
import com.blazebit.persistence.CriteriaBuilderFactory
import com.blazebit.persistence.KeysetPage
import com.blazebit.persistence.PagedList
import com.blazebit.persistence.view.EntityViewManager
import com.blazebit.persistence.view.EntityViewSetting
import io.bluetape4k.examples.jpa.blazepersistence.domain.dto.MemberPage
import io.bluetape4k.examples.jpa.blazepersistence.domain.dto.MemberSearchCondition
import io.bluetape4k.examples.jpa.blazepersistence.domain.model.Member
import io.bluetape4k.examples.jpa.blazepersistence.domain.view.MemberTeamView
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * Repository examples that use Blaze Persistence directly.
 */
@Repository
@Transactional(readOnly = true)
class MemberBlazeRepository(
    private val criteriaBuilderFactory: CriteriaBuilderFactory,
    private val entityViewManager: EntityViewManager,
) {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    fun findViews(condition: MemberSearchCondition): List<MemberTeamView> {
        return entityViewManager
            .applySetting(EntityViewSetting.create(MemberTeamView::class.java), baseCriteria(condition))
            .resultList
    }

    fun findPage(condition: MemberSearchCondition, firstResult: Int, maxResults: Int): MemberPage<MemberTeamView> {
        val paged = viewPage(condition, firstResult, maxResults)
        return paged.toMemberPage()
    }

    fun findNextPage(
        condition: MemberSearchCondition,
        keysetPage: KeysetPage,
        firstResult: Int,
        maxResults: Int,
    ): MemberPage<MemberTeamView> {
        val setting = EntityViewSetting.create(MemberTeamView::class.java, firstResult, maxResults)
            .withKeysetPage(keysetPage)

        val paged = entityViewManager
            .applySetting(setting, baseCriteria(condition))
            .resultList

        return paged.toMemberPage()
    }

    private fun viewPage(
        condition: MemberSearchCondition,
        firstResult: Int,
        maxResults: Int,
    ): PagedList<MemberTeamView> {
        val setting = EntityViewSetting.create(MemberTeamView::class.java, firstResult, maxResults)
            .withKeysetPage(null)

        return entityViewManager
            .applySetting(setting, baseCriteria(condition))
            .resultList
    }

    private fun baseCriteria(condition: MemberSearchCondition): CriteriaBuilder<Member> {
        val criteria = criteriaBuilderFactory.create(entityManager, Member::class.java, "member")
            .leftJoin("member.team", "team")

        condition.memberName?.let { criteria.where("member.name").eq(it) }
        condition.teamName?.let { criteria.where("team.name").eq(it) }
        condition.ageGoe?.let { criteria.where("member.age").ge(it) }
        condition.ageLoe?.let { criteria.where("member.age").le(it) }

        return criteria
            .orderByAsc("age")
            .orderByAsc("id")
    }

    private fun PagedList<MemberTeamView>.toMemberPage(): MemberPage<MemberTeamView> {
        return MemberPage(
            content = this,
            totalSize = totalSize,
            totalPages = totalPages,
            firstResult = firstResult,
            maxResults = maxResults,
            keysetPage = keysetPage,
        )
    }
}
