package io.bluetape4k.hibernate.reactive.mutiny

import io.smallrye.mutiny.Uni
import jakarta.persistence.EntityGraph
import jakarta.persistence.LockModeType
import org.hibernate.LockMode
import org.hibernate.reactive.common.AffectedEntities
import org.hibernate.reactive.common.Identifier
import org.hibernate.reactive.common.ResultSetMapping
import org.hibernate.reactive.mutiny.Mutiny

/**
 * 엔티티 타입 [T]와 식별자 id로 엔티티를 조회합니다.
 */
inline fun <reified T> Mutiny.Session.findAs(id: java.io.Serializable): Uni<T> =
    find(T::class.java, id)

/**
 * 엔티티 타입 [T]와 식별자 id로 엔티티를 lock mode와 함께 조회합니다.
 */
inline fun <reified T> Mutiny.Session.findAs(id: java.io.Serializable, lockMode: LockMode): Uni<T> =
    find(T::class.java, id, lockMode)

/**
 * 엔티티 타입 [T]와 식별자 id로 엔티티를 JPA lock mode와 함께 조회합니다.
 */
inline fun <reified T> Mutiny.Session.findAs(id: java.io.Serializable, lockModeType: LockModeType): Uni<T> =
    find(T::class.java, id, lockModeType)

/**
 * 엔티티 타입 [T]와 여러 id로 엔티티 목록을 조회합니다.
 */
inline fun <reified T> Mutiny.Session.findAs(vararg ids: java.io.Serializable): Uni<List<T>> =
    find(T::class.java, *ids)

/**
 * 엔티티 타입 [T]와 natural id로 엔티티를 조회합니다.
 */
inline fun <reified T> Mutiny.Session.findAs(naturalId: Identifier<T>): Uni<T> =
    find(T::class.java, naturalId)

/**
 * [entityGraph]를 fetch plan으로 사용하여 엔티티를 조회합니다.
 */
inline fun <reified T> Mutiny.Session.findAs(entityGraph: EntityGraph<T>, id: java.io.Serializable): Uni<T> =
    find(entityGraph, id)

/**
 * [graphName]으로 EntityGraph를 가져와 fetch plan으로 사용하여 엔티티를 조회합니다.
 */
inline fun <reified T> Mutiny.Session.findAs(graphName: String, id: java.io.Serializable): Uni<T> =
    find(getEntityGraphAs<T>(graphName), id)

/**
 * 엔티티 타입 [T]의 참조(프록시)를 id로 조회합니다.
 */
inline fun <reified T> Mutiny.Session.getReferenceAs(id: java.io.Serializable): T =
    getReference(T::class.java, id)

/**
 * 결과 타입 [R]의 HQL/JPQL 조회 쿼리를 생성합니다.
 */
inline fun <reified R> Mutiny.Session.createQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createQuery(queryString, R::class.java)

/**
 * 결과 타입 [R]의 selection query를 생성합니다.
 */
inline fun <reified R> Mutiny.Session.createSelectionQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createSelectionQuery(queryString, R::class.java)

/**
 * 결과 타입 [R]의 named query를 생성합니다.
 */
inline fun <reified R> Mutiny.Session.createNamedQueryAs(queryName: String): Mutiny.SelectionQuery<R> =
    createNamedQuery(queryName, R::class.java)

/**
 * 결과 타입 [R]의 native query를 생성합니다.
 */
inline fun <reified R> Mutiny.Session.createNativeQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java)

/**
 * 영향 엔티티 정보를 포함한 결과 타입 [R]의 native query를 생성합니다.
 */
inline fun <reified R> Mutiny.Session.createNativeQueryAs(
    queryString: String,
    affectedEntities: AffectedEntities,
): Mutiny.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java, affectedEntities)

/**
 * 매핑 이름으로 결과 타입 [T]의 ResultSetMapping을 조회합니다.
 */
inline fun <reified T> Mutiny.Session.getResultSetMappingAs(mappingName: String): ResultSetMapping<T> =
    getResultSetMapping(T::class.java, mappingName)

/**
 * 그래프 이름으로 엔티티 타입 [T]의 EntityGraph를 조회합니다.
 */
inline fun <reified T> Mutiny.Session.getEntityGraphAs(graphName: String): EntityGraph<T> =
    getEntityGraph(T::class.java, graphName)

/**
 * 엔티티 타입 [T]의 EntityGraph를 새로 생성합니다.
 */
inline fun <reified T> Mutiny.Session.createEntityGraphAs(): EntityGraph<T> =
    createEntityGraph(T::class.java)

/**
 * 그래프 이름으로 엔티티 타입 [T]의 EntityGraph를 생성합니다.
 */
inline fun <reified T> Mutiny.Session.createEntityGraphAs(graphName: String): EntityGraph<T> =
    createEntityGraph(T::class.java, graphName)
