package io.bluetape4k.examples.jpa.blazepersistence

import io.bluetape4k.logging.KLogging
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component

/**
 * Minimal Spring Boot 4 test entity manager shim.
 */
@Component
class TestEntityManager(@PersistenceContext val entityManager: EntityManager) {

    companion object: KLogging()

    fun <E: Any> persist(entity: E): E {
        entityManager.persist(entity)
        return entity
    }

    fun flush() = entityManager.flush()

    fun clear() = entityManager.clear()
}
