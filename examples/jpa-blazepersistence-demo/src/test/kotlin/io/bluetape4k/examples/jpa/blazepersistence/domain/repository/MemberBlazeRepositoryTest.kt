package io.bluetape4k.examples.jpa.blazepersistence.domain.repository

import com.blazebit.persistence.view.EntityViewManager
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.examples.jpa.blazepersistence.domain.AbstractDomainTest
import io.bluetape4k.examples.jpa.blazepersistence.domain.dto.MemberSearchCondition
import io.bluetape4k.examples.jpa.blazepersistence.domain.view.MemberSummaryView
import io.bluetape4k.examples.jpa.blazepersistence.domain.view.MemberTeamView
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MemberBlazeRepositoryTest(
    @param:Autowired private val memberRepository: MemberBlazeRepository,
    @param:Autowired private val entityViewManager: EntityViewManager,
): AbstractDomainTest() {

    @Test
    fun `context loading`() {
        memberRepository.shouldNotBeNull()
        entityViewManager.shouldNotBeNull()
    }

    @Test
    fun `entity views are registered`() {
        entityViewManager.metamodel.managedView(MemberSummaryView::class.java).shouldNotBeNull()
        entityViewManager.metamodel.managedView(MemberTeamView::class.java).shouldNotBeNull()
    }

    @Test
    fun `find views by dynamic condition`() {
        val condition = MemberSearchCondition(teamName = "teamA", ageGoe = 10, ageLoe = 30)

        val members = memberRepository.findViews(condition)

        members shouldHaveSize 11
        members.first().name shouldBeEqualTo "member-10"
        members.first().teamName shouldBeEqualTo "teamA"
        members.last().name shouldBeEqualTo "member-30"
    }

    @Test
    fun `find page returns paged list metadata`() {
        val condition = MemberSearchCondition(teamName = "teamA", ageGoe = 10, ageLoe = 30)

        val page = memberRepository.findPage(condition, firstResult = 0, maxResults = 5)

        page.content shouldHaveSize 5
        page.totalSize shouldBeEqualTo 11L
        page.totalPages shouldBeEqualTo 3
        page.firstResult shouldBeEqualTo 0
        page.maxResults shouldBeEqualTo 5
        page.content.first().name shouldBeEqualTo "member-10"
        page.content.last().name shouldBeEqualTo "member-18"
        page.keysetPage.shouldNotBeNull()
    }

    @Test
    fun `find next page with keyset pagination`() {
        val condition = MemberSearchCondition(ageGoe = 10, ageLoe = 30)
        val firstPage = memberRepository.findPage(condition, firstResult = 0, maxResults = 10)
        val keysetPage = firstPage.keysetPage.shouldNotBeNull()

        val secondPage = memberRepository.findNextPage(
            condition = condition,
            keysetPage = keysetPage,
            firstResult = 10,
            maxResults = 10,
        )

        secondPage.content shouldHaveSize 10
        secondPage.totalSize shouldBeEqualTo 21L
        secondPage.content.first().name shouldBeEqualTo "member-20"
        secondPage.content.last().name shouldBeEqualTo "member-29"
    }

    @Test
    fun `find page returns empty result`() {
        val condition = MemberSearchCondition(memberName = "missing")

        val page = memberRepository.findPage(condition, firstResult = 0, maxResults = 5)

        page.content shouldHaveSize 0
        page.totalSize shouldBeEqualTo 0L
        page.totalPages shouldBeEqualTo 0
        page.keysetPage shouldBeEqualTo null
    }
}
