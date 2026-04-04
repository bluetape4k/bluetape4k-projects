package io.bluetape4k.hibernate.criteria

import jakarta.persistence.criteria.AbstractQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * `from(Class<T>)` 메서드를 호출할 때, `T` 를 추론하기 위한 확장 함수입니다.
 *
 * ## 동작/계약
 * - `from(T::class.java)`로 위임합니다.
 */
inline fun <reified T: Any> AbstractQuery<T>.from(): Root<T> = this.from(T::class.java)

/**
 * [attribute] 이름으로 조회하는 [Path] 를 생성합니다.
 *
 * ## 동작/계약
 * - Kotlin property 이름을 그대로 JPA 메타모델 경로 이름으로 사용합니다.
 */
fun <T, V> Root<T>.attribute(attribute: KProperty1<T, V>): Path<V> = this.get(attribute.name)

/**
 * [clazz]를 위한 [CriteriaQuery]를 생성합니다.
 *
 * ## 동작/계약
 * - `createQuery(clazz.java)`로 위임합니다.
 */
fun <T: Any> CriteriaBuilder.createQuery(clazz: KClass<T>): CriteriaQuery<T> = createQuery(clazz.java)

/**
 * [T]를 위한 [CriteriaQuery]를 생성합니다.
 *
 * ## 동작/계약
 * - reified 타입으로 [CriteriaQuery]를 생성하는 편의 함수입니다.
 */
inline fun <reified T: Any> CriteriaBuilder.createQueryAs(): CriteriaQuery<T> = createQuery(T::class.java)

/**
 * `equal` 용 [Predicate]를 생성합니다.
 *
 * ```kotlin
 * val pred = builder.eq(root.get<String>("name"), "Alice")
 * ```
 */
fun CriteriaBuilder.eq(x: Expression<*>, y: Any?): Predicate = this.equal(x, y)

/**
 * `equal` 용 [Predicate]를 생성합니다.
 *
 * ```kotlin
 * val pred = builder.eq(root.get<String>("name"), otherExpr)
 * ```
 */
fun CriteriaBuilder.eq(x: Expression<*>, y: Expression<*>): Predicate = this.equal(x, y)

/**
 * `notEqual` 용 [Predicate]를 생성합니다.
 *
 * ```kotlin
 * val pred = builder.ne(root.get<String>("status"), "DELETED")
 * ```
 */
fun CriteriaBuilder.ne(x: Expression<*>, y: Any?): Predicate = this.notEqual(x, y)

/**
 * `notEqual` 용 [Predicate]를 생성합니다.
 *
 * ```kotlin
 * val pred = builder.ne(root.get<String>("name"), otherExpr)
 * ```
 */
fun CriteriaBuilder.ne(x: Expression<*>, y: Expression<*>): Predicate = this.notEqual(x, y)

/**
 * `in` 용 [CriteriaBuilder.In]를 생성합니다.
 *
 * ```kotlin
 * val inClause = builder.inValues(root.get<String>("status"))
 * inClause.value("ACTIVE").value("PENDING")
 * ```
 */
fun <T> CriteriaBuilder.inValues(expr: Expression<out T>): CriteriaBuilder.In<T> = this.`in`(expr)
