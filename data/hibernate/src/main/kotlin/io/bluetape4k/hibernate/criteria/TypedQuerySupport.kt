@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package io.bluetape4k.hibernate.criteria

import jakarta.persistence.NoResultException
import jakarta.persistence.TypedQuery
import java.util.stream.IntStream
import java.util.stream.LongStream

/**
 * [TypedQuery]의 Java Long 타입을 `List<Long>` 타입으로 변환합니다.
 */
fun TypedQuery<java.lang.Long>.longList(): List<Long> = resultList.map { it.toLong() }

/**
 * [TypedQuery]의 Java Long 타입을 [LongArray] 로 변환합니다.
 */
fun TypedQuery<java.lang.Long>.longArray(): LongArray = longList().toLongArray()

/**
 * [TypedQuery]의 Java Long 타입의 [LongStream] 타입으로 변환합니다.
 */
fun TypedQuery<java.lang.Long>.longStream(): LongStream = resultStream.mapToLong { it.toLong() }

/**
 * [TypedQuery]의 Java Long 타입을 Kotlin Long 타입으로 변환합니다.
 */
fun TypedQuery<java.lang.Long>.longResult(): Long? = findOneOrNull()?.toLong()

/**
 * [TypedQuery]의 Java Integer 타입을 `List<Int>` 타입으로 변환합니다.
 */
fun TypedQuery<java.lang.Integer>.intList(): List<Int> = resultList.map { it.toInt() }

/**
 * [TypedQuery]의 Java Integer 타입을 [IntArray] 로 변환합니다.
 */
fun TypedQuery<java.lang.Integer>.intArray(): IntArray = intList().toIntArray()

/**
 * [TypedQuery]의 Java Integer 타입의 [IntStream] 타입으로 변환합니다.
 */
fun TypedQuery<java.lang.Integer>.intStream(): IntStream = resultStream.mapToInt { it.toInt() }

/**
 * [TypedQuery]의 Java Integer 타입을 Kotlin Int 타입으로 변환합니다.
 */
fun TypedQuery<java.lang.Integer>.intResult(): Int? = findOneOrNull()?.toInt()

/**
 * [TypedQuery.getSingleResult]를 실행하여, 값을 가져온다. 값이 없거나 예외가 발생하면 null을 반환한다.
 *
 * ```
 * val query = entityManager.createQuery("SELECT COUNT(*) FROM User", Long::class.java)
 * val count = query.getSingleResultOrNull()
 * ```
 */
fun <T: Any> TypedQuery<T>.findOneOrNull(): T? = try {
    singleResult
} catch (e: NoResultException) {
    null
}
