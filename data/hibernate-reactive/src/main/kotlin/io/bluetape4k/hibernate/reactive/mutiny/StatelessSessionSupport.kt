package io.bluetape4k.hibernate.reactive.mutiny

import io.smallrye.mutiny.Uni
import jakarta.persistence.EntityGraph
import jakarta.persistence.LockModeType
import org.hibernate.LockMode
import org.hibernate.reactive.common.ResultSetMapping
import org.hibernate.reactive.mutiny.Mutiny

/**
 * 엔티티 타입 [T]와 식별자 id로 엔티티를 조회합니다.
 */
inline fun <reified T> Mutiny.StatelessSession.getAs(id: java.io.Serializable): Uni<T> =
    get(T::class.java, id)

/**
 * 엔티티 타입 [T]와 식별자 id로 엔티티를 lock mode와 함께 조회합니다.
 */
inline fun <reified T> Mutiny.StatelessSession.getAs(id: java.io.Serializable, lockMode: LockMode): Uni<T> =
    get(T::class.java, id, lockMode)

/**
 * 엔티티 타입 [T]와 식별자 id로 엔티티를 JPA lock mode와 함께 조회합니다.
 */
inline fun <reified T> Mutiny.StatelessSession.getAs(id: java.io.Serializable, lockModeType: LockModeType): Uni<T> =
    get(T::class.java, id, lockModeType)

/**
 * [entityGraph]를 fetch plan으로 사용하여 엔티티를 조회합니다.
 */
inline fun <reified T> Mutiny.StatelessSession.getAs(entityGraph: EntityGraph<T>, id: java.io.Serializable): Uni<T> =
    get(entityGraph, id)

/**
 * [graphName]으로 EntityGraph를 가져와 fetch plan으로 사용하여 엔티티를 조회합니다.
 */
inline fun <reified T> Mutiny.StatelessSession.getAs(graphName: String, id: java.io.Serializable): Uni<T> =
    get(getEntityGraphAs<T>(graphName), id)

/**
 * 결과 타입 [R]의 HQL/JPQL 조회 쿼리를 생성합니다.
 */
inline fun <reified R> Mutiny.StatelessSession.createQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createQuery(queryString, R::class.java)

/**
 * 결과 타입 [R]의 selection query를 생성합니다.
 */
inline fun <reified R> Mutiny.StatelessSession.createSelectionQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createSelectionQuery(queryString, R::class.java)

/**
 * 결과 타입 [R]의 named query를 생성합니다.
 */
inline fun <reified R> Mutiny.StatelessSession.createNamedQueryAs(queryName: String): Mutiny.SelectionQuery<R> =
    createNamedQuery(queryName, R::class.java)

/**
 * 결과 타입 [R]의 native query를 생성합니다.
 */
inline fun <reified R> Mutiny.StatelessSession.createNativeQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java)

/**
 * 매핑 이름으로 결과 타입 [T]의 ResultSetMapping을 조회합니다.
 */
inline fun <reified T> Mutiny.StatelessSession.getResultSetMappingAs(mappingName: String): ResultSetMapping<T> =
    getResultSetMapping(T::class.java, mappingName)

/**
 * 그래프 이름으로 엔티티 타입 [T]의 EntityGraph를 조회합니다.
 */
inline fun <reified T> Mutiny.StatelessSession.getEntityGraphAs(graphName: String): EntityGraph<T> =
    getEntityGraph(T::class.java, graphName)

/**
 * 엔티티 타입 [T]의 EntityGraph를 새로 생성합니다.
 */
inline fun <reified T> Mutiny.StatelessSession.createEntityGraphAs(): EntityGraph<T> =
    createEntityGraph(T::class.java)

/**
 * 그래프 이름으로 엔티티 타입 [T]의 EntityGraph를 생성합니다.
 */
inline fun <reified T> Mutiny.StatelessSession.createEntityGraphAs(graphName: String): EntityGraph<T> =
    createEntityGraph(T::class.java, graphName)
