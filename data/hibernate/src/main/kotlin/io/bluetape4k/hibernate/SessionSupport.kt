package io.bluetape4k.hibernate

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requirePositiveNumber
import org.hibernate.Session
import org.hibernate.query.Query
import org.slf4j.Logger
import java.io.Serializable
import kotlin.reflect.KClass

private val log: Logger by lazy { KotlinLogging.logger { } }

/**
 * [batchSize]를 설정하고 [block]을 실행합니다.
 *
 * ```
 * // 100개씩 엔티티 저장
 * session.withBatchSize(100) {
 *    // 엔티티 저장
 *    entities.forEach { entity ->
 *          session.save(entity)
 *    }
 * }
 *
 * @param batchSize Batch size
 * @param block 실행할 코드 블럭
 */
fun <T> Session.withBatchSize(batchSize: Int, block: Session.() -> T): T {
    batchSize.requirePositiveNumber("batchSize")

    val prevBatchSize = runCatching { this.jdbcBatchSize }.getOrNull() ?: 0

    return try {
        log.debug { "Batch size[$batchSize]를 적용하여 작업을 수행합니다 ..." }
        this.jdbcBatchSize = batchSize
        block(this)
    } finally {
        runCatching { this.jdbcBatchSize = prevBatchSize }
    }
}

/**
 * id에 해당하는 엔티티를 조회합니다. 없으면 null을 반환합니다.
 */
inline fun <reified T: Any> Session.findAs(id: Serializable): T? = find(T::class.java, id)

/**
 * id에 해당하는 엔티티 참조(proxy)를 조회합니다.
 */
inline fun <reified T: Any> Session.getReferenceAs(id: Serializable): T = getReference(T::class.java, id)

/**
 * HQL/JPQL 문자열과 결과 수형을 받아 [Query]를 생성합니다.
 */
fun <T: Any> Session.createQueryAs(queryString: String, resultClass: KClass<T>): Query<T> =
    createQuery(queryString, resultClass.java)

/**
 * HQL/JPQL 문자열과 reified 타입으로 [Query]를 생성합니다.
 */
inline fun <reified T> Session.createQueryAs(queryString: String): Query<T> =
    createQuery(queryString, T::class.java)

/**
 * Native SQL 문자열과 결과 수형을 받아 [Query]를 생성합니다.
 */
fun <T: Any> Session.createNativeQueryAs(queryString: String, resultClass: KClass<T>): Query<T> =
    createNativeQuery(queryString, resultClass.java)

/**
 * Native SQL 문자열과 reified 타입으로 [Query]를 생성합니다.
 */
inline fun <reified T> Session.createNativeQueryAs(queryString: String): Query<T> =
    createNativeQuery(queryString, T::class.java)
