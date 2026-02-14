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
inline fun <T: Any> SessionFactory.withStateless(block: (StatelessSession) -> T?): T? =
    this.openStatelessSession().use { stateless ->
        val tx = stateless.beginTransaction()

        try {
            val result = block(stateless)
            tx.commit()
            result
        } catch (e: Throwable) {
            runCatching {
                if (tx.isActive) {
                    tx.rollback()
                }
            }
            throw e
        }
    }

/**
 * 오타가 포함된 이전 API 이름.
 *
 * 유지보수 호환성을 위해 남겨두며, 새 코드에서는 [withStateless]를 사용하세요.
 */
@Deprecated(
    message = "Use withStateless instead.",
    replaceWith = ReplaceWith("withStateless(block)")
)
inline fun <T: Any> SessionFactory.withStatelss(block: (StatelessSession) -> T?): T? =
    withStateless(block)

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
    this.sessionFactory().withStateless(block)
