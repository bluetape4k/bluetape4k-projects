package io.bluetape4k.hibernate.reactive.stage

import jakarta.persistence.EntityGraph
import org.hibernate.LockMode
import org.hibernate.reactive.common.ResultSetMapping
import org.hibernate.reactive.stage.Stage
import java.util.concurrent.CompletionStage

/**
 * 엔티티 타입 [T]와 식별자 id로 엔티티를 조회합니다.
 */
inline fun <reified T> Stage.StatelessSession.getAs(id: java.io.Serializable): CompletionStage<T> =
    get(T::class.java, id)

/**
 * 엔티티 타입 [T]와 식별자 id로 엔티티를 lock mode와 함께 조회합니다.
 */
inline fun <reified T> Stage.StatelessSession.getAs(id: java.io.Serializable, lockMode: LockMode): CompletionStage<T> =
    get(T::class.java, id, lockMode)

/**
 * 결과 타입 [R]의 HQL/JPQL 조회 쿼리를 생성합니다.
 */
inline fun <reified R> Stage.StatelessSession.createQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createQuery(queryString, R::class.java)

/**
 * 결과 타입 [R]의 selection query를 생성합니다.
 */
inline fun <reified R> Stage.StatelessSession.createSelectionQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createSelectionQuery(queryString, R::class.java)

/**
 * 결과 타입 [R]의 named query를 생성합니다.
 */
inline fun <reified R> Stage.StatelessSession.createNamedQueryAs(queryName: String): Stage.SelectionQuery<R> =
    createNamedQuery(queryName, R::class.java)

/**
 * 결과 타입 [R]의 native query를 생성합니다.
 */
inline fun <reified R> Stage.StatelessSession.createNativeQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java)

/**
 * 매핑 이름으로 결과 타입 [T]의 ResultSetMapping을 조회합니다.
 */
inline fun <reified T> Stage.StatelessSession.getResultSetMappingAs(mappingName: String): ResultSetMapping<T> =
    getResultSetMapping(T::class.java, mappingName)

/**
 * 그래프 이름으로 엔티티 타입 [T]의 EntityGraph를 조회합니다.
 */
inline fun <reified T> Stage.StatelessSession.getEntityGraphAs(graphName: String): EntityGraph<T> =
    getEntityGraph(T::class.java, graphName)

/**
 * 엔티티 타입 [T]의 EntityGraph를 새로 생성합니다.
 */
inline fun <reified T> Stage.StatelessSession.createEntityGraphAs(): EntityGraph<T> =
    createEntityGraph(T::class.java)

/**
 * 그래프 이름으로 엔티티 타입 [T]의 EntityGraph를 생성합니다.
 */
inline fun <reified T> Stage.StatelessSession.createEntityGraphAs(graphName: String): EntityGraph<T> =
    createEntityGraph(T::class.java, graphName)
