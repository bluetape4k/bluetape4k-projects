package io.bluetape4k.hibernate

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory

/**
 * 새로운 [EntityManager] 를 생성하여, Transaction 하에서 DB 작업을 수행하고, [EntityManager]는 소멸시킵니다.
 *
 * ```
 * val newEntity = entityManagerFactory.withNewEntityManager { em ->
 *    val entity = em.find(Entity::class.java, id)
 *    entity.name = "new name"
 *    em.persist(entity)
 * }
 * ```
 *
 * @param block 실행할 코드 블럭
 * @return 실행 결과
 */
inline fun <T> EntityManagerFactory.withNewEntityManager(block: (EntityManager) -> T): T {
    createEntityManager().use { em ->
        em.transaction.begin()
        try {
            val result = block(em)
            em.transaction.commit()
            return result
        } catch (e: Exception) {
            em.transaction.rollback()
            throw RuntimeException(e)
        }
    }
}
