package io.bluetape4k.examples.jpa.querydsl

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.Hibernate
import org.springframework.stereotype.Component

/**
 * Spring Boot 4에서 제거된 `TestEntityManager`를 대체하는 shim.
 *
 * `@DataJpaTest` 슬라이스 테스트를 `@SpringBootTest + @Transactional` 기반으로 전환할 때 사용합니다.
 *
 * ```kotlin
 * @Autowired
 * private lateinit var tem: TestEntityManager
 *
 * val saved = tem.persistFlushFind(User(name = "debop"))
 * // saved는 1차 캐시가 비워진 뒤 DB에서 다시 로드된 fresh instance.
 * ```
 */
@Component
class TestEntityManager(@PersistenceContext val entityManager: EntityManager) {

    companion object : KLogging()

    /**
     * 엔티티를 영속성 컨텍스트에 저장하고 반환합니다.
     *
     * ```kotlin
     * val user = tem.persist(User(name = "debop"))
     * ```
     */
    fun <E : Any> persist(entity: E): E {
        entityManager.persist(entity)
        return entity
    }

    /**
     * 엔티티를 저장하고 즉시 flush하여 DB에 반영합니다.
     *
     * ```kotlin
     * val user = tem.persistAndFlush(User(name = "debop"))
     * // flush 이후 DB에서 즉시 조회 가능
     * ```
     */
    fun <E : Any> persistAndFlush(entity: E): E {
        entityManager.persist(entity)
        entityManager.flush()
        return entity
    }

    /**
     * `persist` → `flush` → `detach` → `find` 순으로 실행하여, 영속성 컨텍스트의 1차 캐시를
     * 우회한 "DB에서 막 읽어온" 인스턴스를 반환합니다. JPA 매핑 및 리스너 검증에 유용합니다.
     *
     * [Hibernate.getClass]를 사용하여 프록시/바이트코드 향상 인스턴스에서 실제 엔티티 클래스를 해석합니다.
     *
     * ```kotlin
     * val saved = tem.persistFlushFind(User(name = "debop"))
     * // saved는 1차 캐시 우회 후 DB에서 다시 로드된 fresh instance.
     * ```
     *
     * @throws IllegalArgumentException flush 후에도 ID로 조회되지 않는 경우.
     */
    fun <E : Any> persistFlushFind(entity: E): E {
        entityManager.persist(entity)
        entityManager.flush()
        entityManager.detach(entity)
        val id = entityManager.entityManagerFactory.persistenceUnitUtil.getIdentifier(entity)
        // WHY: Hibernate.getClass() resolves the real entity class from a proxy/bytecode-enhanced instance.
        @Suppress("UNCHECKED_CAST")
        val entityClass = Hibernate.getClass(entity) as Class<E>
        log.debug { "persistFlushFind: entityClass=${entityClass.simpleName}, id=$id" }
        return requireNotNull(entityManager.find(entityClass, id)) {
            "Entity of type ${entityClass.simpleName} with id=$id not found after flush"
        }
    }

    /**
     * 영속성 컨텍스트를 flush하여 변경 사항을 DB에 반영합니다.
     */
    fun flush() = entityManager.flush()

    /**
     * 영속성 컨텍스트의 1차 캐시를 비웁니다.
     */
    fun clear() = entityManager.clear()

    /**
     * 엔티티를 영속성 컨텍스트에서 제거합니다.
     * 분리 상태(detached)인 경우 먼저 merge 후 remove합니다.
     */
    fun remove(entity: Any) {
        val managed = if (entityManager.contains(entity)) entity else entityManager.merge(entity)
        entityManager.remove(managed)
    }

    /**
     * 주어진 ID로 엔티티를 조회합니다. 없으면 `null`을 반환합니다.
     *
     * ```kotlin
     * val user = tem.find(User::class.java, userId)
     * ```
     *
     * @throws IllegalArgumentException [id]가 null인 경우 — flush 전에 ID가 할당됐는지 확인하세요.
     */
    fun <E : Any> find(clazz: Class<E>, id: Any?): E? {
        requireNotNull(id) { "id must not be null when calling find — check that entity was flushed first" }
        return entityManager.find(clazz, id)
    }
}
