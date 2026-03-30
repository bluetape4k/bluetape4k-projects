package io.bluetape4k.hibernate

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty
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
 * ## 동작/계약
 * - [batchSize]는 0보다 커야 하며, 아니면 [IllegalArgumentException]이 발생합니다.
 * - 블록 실행 전 `jdbcBatchSize`를 변경하고, 종료 후 원래 값으로 복원합니다.
 * - 블록에서 예외가 발생해도 `finally`에서 배치 크기 원복을 시도합니다.
 *
 * ```kotlin
 * session.withBatchSize(100) {
 *     entities.forEach { save(it) }
 * }
 * // 실행 후 jdbcBatchSize는 이전 값으로 복원됨
 * ```
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
 *
 * ## 동작/계약
 * - 내부적으로 Hibernate `find`를 호출합니다.
 */
inline fun <reified T: Any> Session.findAs(id: Serializable): T? = find(T::class.java, id)

/**
 * id에 해당하는 엔티티 참조(proxy)를 조회합니다.
 *
 * ## 동작/계약
 * - 내부적으로 Hibernate `getReference`를 호출합니다.
 * - 즉시 DB 조회가 일어나지 않을 수 있으며, 접근 시점에 예외가 발생할 수 있습니다.
 */
inline fun <reified T: Any> Session.getReferenceAs(id: Serializable): T = getReference(T::class.java, id)

/**
 * simple natural id 값으로 엔티티를 조회합니다. 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - Hibernate `bySimpleNaturalId(...).load(...)`에 위임합니다.
 * - `@NaturalId`가 하나인 엔티티에 사용합니다.
 */
inline fun <reified T : Any> Session.findBySimpleNaturalId(naturalId: Any): T? =
    bySimpleNaturalId(T::class.java).load(naturalId)

/**
 * 복합 natural id 속성으로 엔티티를 조회합니다. 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - [naturalIdValues]는 비어 있을 수 없고, 각 속성명은 blank일 수 없습니다.
 * - Hibernate `byNaturalId(...).using(...).load()`에 위임합니다.
 */
inline fun <reified T : Any> Session.findByNaturalId(
    naturalIdValues: Map<String, Any?>,
): T? {
    naturalIdValues.requireNotEmpty("naturalIdValues")

    val access = byNaturalId(T::class.java)
    naturalIdValues.forEach { (attributeName, value) ->
        attributeName.requireNotBlank("naturalIdValues.key")
        access.using(attributeName, value)
    }
    return access.load()
}

/**
 * 복합 natural id 속성으로 엔티티를 조회합니다. 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - [findByNaturalId]의 pair 오버로드입니다.
 */
inline fun <reified T : Any> Session.findByNaturalId(
    vararg naturalIdValues: Pair<String, Any?>,
): T? = findByNaturalId(naturalIdValues.toMap())

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
