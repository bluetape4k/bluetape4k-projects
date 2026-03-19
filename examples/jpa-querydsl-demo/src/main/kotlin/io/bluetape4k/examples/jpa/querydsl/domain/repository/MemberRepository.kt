package io.bluetape4k.examples.jpa.querydsl.domain.repository

import io.bluetape4k.examples.jpa.querydsl.domain.model.Member
import io.bluetape4k.examples.jpa.querydsl.domain.model.Team
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface MemberRepository: JpaRepository<Member, Long>,
                            MemberRepositoryCustom,
                            QuerydslPredicateExecutor<Member> {

    fun findAllByName(name: String): List<Member>

    fun findAllByTeam(team: Team): List<Member>
}
