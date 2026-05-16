package io.bluetape4k.examples.jpa.blazepersistence.services

import io.bluetape4k.examples.jpa.blazepersistence.domain.model.Member
import io.bluetape4k.examples.jpa.blazepersistence.domain.model.Team
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Test fixture initializer for member/team examples.
 */
@Component
class InitMemberService {

    companion object: KLogging()

    @PersistenceContext
    private lateinit var em: EntityManager

    @Transactional
    fun init() {
        log.debug { "Add sample team and member entities ..." }

        val teamA = Team("teamA")
        val teamB = Team("teamB")
        em.persist(teamA)
        em.persist(teamB)
        em.flush()

        repeat(100) {
            val selectedTeam = if (it % 2 == 0) teamA else teamB
            val member = Member("member-$it", it, selectedTeam)
            em.persist(member)
        }

        em.flush()
    }
}
