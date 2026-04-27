package io.bluetape4k.hibernate

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.Hibernate
import org.springframework.stereotype.Component

/**
 * Spring Boot 4에서 제거된 TestEntityManager를 대체하는 shim.
 * @DataJpaTest 슬라이스 테스트를 @SpringBootTest + @Transactional 기반으로 전환할 때 사용.
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
        // WHY: Hibernate.getClass() resolves the real entity class from a proxy/bytecode-enhanced instance.
        val entityClass = Hibernate.getClass(entity) as Class<E>
        return requireNotNull(entityManager.find(entityClass, id)) {
            "Entity of type ${entityClass.simpleName} with id=$id not found after flush"
        }
    }

    fun flush() = entityManager.flush()

    fun clear() = entityManager.clear()

    fun remove(entity: Any) {
        val managed = if (entityManager.contains(entity)) entity else entityManager.merge(entity)
        entityManager.remove(managed)
    }

    fun <E: Any> find(clazz: Class<E>, id: Any?): E? {
        requireNotNull(id) { "id must not be null when calling find — check that entity was flushed first" }
        return entityManager.find(clazz, id)
    }
}
