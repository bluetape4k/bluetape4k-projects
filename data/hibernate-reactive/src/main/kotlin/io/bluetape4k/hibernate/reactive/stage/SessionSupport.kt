package io.bluetape4k.hibernate.reactive.stage

import jakarta.persistence.EntityGraph
import org.hibernate.LockMode
import org.hibernate.reactive.common.AffectedEntities
import org.hibernate.reactive.common.Identifier
import org.hibernate.reactive.common.ResultSetMapping
import org.hibernate.reactive.stage.Stage
import java.util.concurrent.CompletionStage

/**
 * 엔티티 타입 [T]와 식별자 id로 엔티티를 조회합니다.
 */
inline fun <reified T> Stage.Session.findAs(id: java.io.Serializable): CompletionStage<T> =
    find(T::class.java, id)

/**
 * 엔티티 타입 [T]와 식별자 id로 엔티티를 lock mode와 함께 조회합니다.
 */
inline fun <reified T> Stage.Session.findAs(id: java.io.Serializable, lockMode: LockMode): CompletionStage<T> =
    find(T::class.java, id, lockMode)

/**
 * 엔티티 타입 [T]와 여러 id로 엔티티 목록을 조회합니다.
 */
inline fun <reified T> Stage.Session.findAs(vararg ids: java.io.Serializable): CompletionStage<List<T>> =
    find(T::class.java, *ids)

/**
 * 엔티티 타입 [T]와 natural id로 엔티티를 조회합니다.
 */
inline fun <reified T> Stage.Session.findAs(naturalId: Identifier<T>): CompletionStage<T> =
    find(T::class.java, naturalId)

/**
 * 엔티티 타입 [T]의 참조(프록시)를 id로 조회합니다.
 */
inline fun <reified T> Stage.Session.getReferenceAs(id: java.io.Serializable): T =
    getReference(T::class.java, id)

/**
 * 결과 타입 [R]의 HQL/JPQL 조회 쿼리를 생성합니다.
 */
inline fun <reified R> Stage.Session.createQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createQuery(queryString, R::class.java)

/**
 * 결과 타입 [R]의 selection query를 생성합니다.
 */
inline fun <reified R> Stage.Session.createSelectionQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createSelectionQuery(queryString, R::class.java)

/**
 * 결과 타입 [R]의 named query를 생성합니다.
 */
inline fun <reified R> Stage.Session.createNamedQueryAs(queryName: String): Stage.SelectionQuery<R> =
    createNamedQuery(queryName, R::class.java)

/**
 * 결과 타입 [R]의 native query를 생성합니다.
 */
inline fun <reified R> Stage.Session.createNativeQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java)

/**
 * 영향 엔티티 정보를 포함한 결과 타입 [R]의 native query를 생성합니다.
 */
inline fun <reified R> Stage.Session.createNativeQueryAs(
    queryString: String,
    affectedEntities: AffectedEntities,
): Stage.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java, affectedEntities)

/**
 * 매핑 이름으로 결과 타입 [T]의 ResultSetMapping을 조회합니다.
 */
inline fun <reified T> Stage.Session.getResultSetMappingAs(mappingName: String): ResultSetMapping<T> =
    getResultSetMapping(T::class.java, mappingName)

/**
 * 그래프 이름으로 엔티티 타입 [T]의 EntityGraph를 조회합니다.
 */
inline fun <reified T> Stage.Session.getEntityGraphAs(graphName: String): EntityGraph<T> =
    getEntityGraph(T::class.java, graphName)

/**
 * 엔티티 타입 [T]의 EntityGraph를 새로 생성합니다.
 */
inline fun <reified T> Stage.Session.createEntityGraphAs(): EntityGraph<T> =
    createEntityGraph(T::class.java)

/**
 * 그래프 이름으로 엔티티 타입 [T]의 EntityGraph를 생성합니다.
 */
inline fun <reified T> Stage.Session.createEntityGraphAs(graphName: String): EntityGraph<T> =
    createEntityGraph(T::class.java, graphName)
