package io.bluetape4k.hibernate.stateless

import io.bluetape4k.hibernate.sessionFactory
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import jakarta.persistence.EntityManager
import org.hibernate.SessionFactory
import org.hibernate.StatelessSession

internal object StatelessSessionLogger: KLogging()

/**
 * [block]을 [StatelessSession] 환경하에서 작업을 수행합니다.
 *
 * 참고: [Hibernate’s StatelessSession – What it is and how to use it](https://thorben-janssen.com/hibernates-statelesssession/)
 *
 * ```kotlin
 * sessionFactory.withStateless { stateless ->
 *     repeat(ENTITY_COUNT) {
 *         val master = createMaster("master-$it")
 *         stateless.insert(master)
 *         master.details.forEach { detail ->
 *             stateless.insert(detail)
 *         }
 *     }
 * }
 * ```
 *
 * @param T     결과 수형
 * @param block Stateless Session 하에서 실행할 코드 블럭
 * @return 결과 값
 */
inline fun <T: Any> SessionFactory.withStateless(block: (StatelessSession) -> T?): T? =
    this.openStatelessSession().use { stateless ->
        val tx = stateless.beginTransaction()

        try {
            val result = block(stateless)
            tx.commit()
            result
        } catch (e: Throwable) {
            try {
                if (tx.isActive) {
                    tx.rollback()
                }
            } catch (rollbackEx: Throwable) {
                e.addSuppressed(rollbackEx)
                StatelessSessionLogger.log.error(rollbackEx) { "Rollback failed" }
            }
            throw e
        }
    }

/**
 * [block]을 [StatelessSession] 환경하에서 작업을 수행합니다.
 *
 * 참고: [Hibernate’s StatelessSession – What it is and how to use it](https://thorben-janssen.com/hibernates-statelesssession/)
 *
 * ```kotlin
 * entityManager.withStateless { stateless ->
 *     repeat(ENTITY_COUNT) {
 *         val master = createMaster("master-$it")
 *         stateless.insert(master)
 *         master.details.forEach { detail ->
 *             stateless.insert(detail)
 *         }
 *     }
 * }
 * ```
 *
 * @param T     결과 수형
 * @param block Stateless Session 하에서 실행할 코드 블럭
 * @return 결과 값
 */
inline fun <T: Any> EntityManager.withStateless(block: (StatelessSession) -> T?): T? =
    this.sessionFactory().withStateless(block)
