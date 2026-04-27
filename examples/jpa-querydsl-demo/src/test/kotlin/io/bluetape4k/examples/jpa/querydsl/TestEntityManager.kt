package io.bluetape4k.examples.jpa.querydsl

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component

/**
 * Spring Boot 4에서 제거된 TestEntityManager를 대체하는 shim.
 */
@Component
class TestEntityManager(@PersistenceContext val entityManager: EntityManager) {

    fun <E: Any> persist(entity: E): E {
        entityManager.persist(entity)
        return entity
    }

    fun <E: Any> persistAndFlush(entity: E): E {
        entityManager.persist(entity)
        entityManager.flush()
        return entity
    }

    @Suppress("UNCHECKED_CAST")
    fun <E: Any> persistFlushFind(entity: E): E {
        entityManager.persist(entity)
        entityManager.flush()
        entityManager.detach(entity)
        val id = entityManager.entityManagerFactory.persistenceUnitUtil.getIdentifier(entity)
        return entityManager.find(entity::class.java, id) as E
    }

    fun flush() = entityManager.flush()

    fun clear() = entityManager.clear()

    fun remove(entity: Any) {
        val managed = if (entityManager.contains(entity)) entity else entityManager.merge(entity)
        entityManager.remove(managed)
    }

    fun <E: Any> find(clazz: Class<E>, id: Any?): E? =
        if (id == null) null else entityManager.find(clazz, id)
}
