package io.bluetape4k.hibernate

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory

@PublishedApi
internal val emfLog by lazy { KotlinLogging.logger("io.bluetape4k.hibernate.EntityManagerFactorySupport") }

/**
 * 새로운 [EntityManager] 를 생성하여, Transaction 하에서 DB 작업을 수행하고, [EntityManager]는 소멸시킵니다.
 *
 * ```kotlin
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
        } catch (e: Throwable) {
            if (em.transaction.isActive) {
                // WHY: runCatching { rollback() }를 사용하면 롤백 실패가 완전히 무음 처리되어
                //      디버깅 시 원인 파악이 불가능하다. try/catch로 전환하여 warn 로그를 남기되,
                //      롤백 예외를 suppress하고 원본 예외(e)를 그대로 전파한다.
                try {
                    em.transaction.rollback()
                } catch (rollbackEx: Throwable) {
                    emfLog.warn(rollbackEx) { "트랜잭션 롤백 중 예외가 발생했습니다. 원본 예외가 전파됩니다." }
                }
            }
            throw e
        }
    }
}
