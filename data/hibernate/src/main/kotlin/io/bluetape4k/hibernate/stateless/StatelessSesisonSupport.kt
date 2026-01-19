package io.bluetape4k.hibernate.stateless

import io.bluetape4k.hibernate.sessionFactory
import jakarta.persistence.EntityManager
import org.hibernate.SessionFactory
import org.hibernate.StatelessSession

/**
 * [block]을 [StatelessSession] 환경하에서 작업을 수행합니다.
 *
 * 참고: [Hibernate’s StatelessSession – What it is and how to use it](https://thorben-janssen.com/hibernates-statelesssession/)
 *
 * ```
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
inline fun <T: Any> SessionFactory.withStatelss(block: (StatelessSession) -> T?): T? {
    return this.openStatelessSession().use { stateless ->
        val tx = stateless.beginTransaction()

        try {
            val result = block(stateless)
            tx.commit()
            result
        } catch (e: Exception) {
            try {
                tx.rollback()
            } catch (re: Throwable) {
                re.printStackTrace()
            }
            null
        }
    }
}

/**
 * [block]을 [StatelessSession] 환경하에서 작업을 수행합니다.
 *
 * 참고: [Hibernate’s StatelessSession – What it is and how to use it](https://thorben-janssen.com/hibernates-statelesssession/)
 *
 * ```
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
    this.sessionFactory().withStatelss(block)
