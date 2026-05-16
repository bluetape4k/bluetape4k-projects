package io.bluetape4k.examples.jpa.querydsl.domain.repository

import io.bluetape4k.examples.jpa.querydsl.domain.AbstractDomainTest
import io.bluetape4k.examples.jpa.querydsl.domain.dto.MemberSearchCondition
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class JpaRepositoryTest(
    @param:Autowired private val memberRepo: MemberRepository,
): AbstractDomainTest() {

    companion object: KLogging()

    @Test
    fun `context loading`() {
        memberRepo.shouldNotBeNull()
    }

    @Test
    fun `find by search condition`() {
        val searchCond = MemberSearchCondition(memberName = "member-5")
        val memberTeamDtos = memberRepo.search(searchCond)
        memberTeamDtos.forEach {
            log.debug { it }
        }
        memberTeamDtos shouldHaveSize 1
    }

    @Test
    fun `search page methods return filtered page`() {
        val searchCond = MemberSearchCondition(teamName = "teamA", ageGoe = 10, ageLoe = 30)
        val pageable = PageRequest.of(0, 5, Sort.by("age").ascending())

        val pages = listOf(
            memberRepo.searchPageSimple(searchCond, pageable),
            memberRepo.searchPageComplex(searchCond, pageable),
            memberRepo.searchPageExtremeCountQuery(searchCond, pageable),
        )

        pages.forEach { page ->
            page.content shouldHaveSize 5
            page.totalElements shouldBeEqualTo 11L
            page.content.first().member.name shouldBeEqualTo "member-10"
            page.content.last().member.name shouldBeEqualTo "member-18"
        }
    }

    @Test
    fun `search extreme count query supports member-only count condition`() {
        val searchCond = MemberSearchCondition(ageGoe = 10, ageLoe = 30)
        val pageable = PageRequest.of(0, 10, Sort.by("age").ascending())

        val page = memberRepo.searchPageExtremeCountQuery(searchCond, pageable)

        page.content shouldHaveSize 10
        page.totalElements shouldBeEqualTo 21L
        page.content.first().member.name shouldBeEqualTo "member-10"
        page.content.last().member.name shouldBeEqualTo "member-19"
    }
}
