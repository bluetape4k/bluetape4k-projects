package io.bluetape4k.hibernate.stateless

import jakarta.persistence.EntityGraph
import org.hibernate.LockMode
import org.hibernate.StatelessSession
import org.hibernate.graph.GraphSemantic
import org.hibernate.query.SelectionQuery

/**
 * 엔티티 타입 [T]와 식별자 id로 엔티티를 조회합니다.
 */
inline fun <reified T> StatelessSession.getAs(id: java.io.Serializable): T =
    get(T::class.java, id)

/**
 * 엔티티 타입 [T]와 식별자 id로 엔티티를 lock mode와 함께 조회합니다.
 */
inline fun <reified T> StatelessSession.getAs(id: java.io.Serializable, lockMode: LockMode): T =
    get(T::class.java, id, lockMode)

/**
 * [entityGraph]와 [graphSemantic]을 fetch plan으로 사용하여 엔티티를 조회합니다.
 */
inline fun <reified T> StatelessSession.getAs(
    entityGraph: EntityGraph<T>,
    graphSemantic: GraphSemantic,
    id: java.io.Serializable,
): T = get(entityGraph, graphSemantic, id)

/**
 * [entityGraph]와 [graphSemantic], [lockMode]를 함께 사용하여 엔티티를 조회합니다.
 */
inline fun <reified T> StatelessSession.getAs(
    entityGraph: EntityGraph<T>,
    graphSemantic: GraphSemantic,
    id: java.io.Serializable,
    lockMode: LockMode,
): T = get(entityGraph, graphSemantic, id, lockMode)

/**
 * [graphName]으로 EntityGraph를 가져와 [graphSemantic] fetch plan으로 엔티티를 조회합니다.
 */
inline fun <reified T> StatelessSession.getAs(
    graphName: String,
    graphSemantic: GraphSemantic,
    id: java.io.Serializable,
): T = getAs(getEntityGraphAs<T>(graphName), graphSemantic, id)

/**
 * [graphName]으로 EntityGraph를 가져와 [graphSemantic], [lockMode]를 함께 사용해 엔티티를 조회합니다.
 */
inline fun <reified T> StatelessSession.getAs(
    graphName: String,
    graphSemantic: GraphSemantic,
    id: java.io.Serializable,
    lockMode: LockMode,
): T = getAs(getEntityGraphAs<T>(graphName), graphSemantic, id, lockMode)

/**
 * 결과 타입 [R]의 HQL/JPQL 조회 쿼리를 생성합니다.
 */
inline fun <reified R> StatelessSession.createQueryAs(queryString: String): SelectionQuery<R> =
    createQuery(queryString, R::class.java)

/**
 * 결과 타입 [R]의 selection query를 생성합니다.
 */
inline fun <reified R> StatelessSession.createSelectionQueryAs(queryString: String): SelectionQuery<R> =
    createSelectionQuery(queryString, R::class.java)

/**
 * 결과 타입 [R]의 named query를 생성합니다.
 */
inline fun <reified R> StatelessSession.createNamedQueryAs(queryName: String): SelectionQuery<R> =
    createNamedQuery(queryName, R::class.java)

/**
 * 결과 타입 [R]의 native query를 생성합니다.
 */
inline fun <reified R> StatelessSession.createNativeQueryAs(queryString: String): SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java)

/**
 * 엔티티 타입 [T]의 EntityGraph를 새로 생성합니다.
 */
inline fun <reified T> StatelessSession.createEntityGraphAs(): EntityGraph<T> =
    createEntityGraph(T::class.java)

/**
 * 그래프 이름으로 엔티티 타입 [T]의 EntityGraph를 생성합니다.
 */
inline fun <reified T> StatelessSession.createEntityGraphAs(graphName: String): EntityGraph<T> =
    createEntityGraph(T::class.java, graphName)

/**
 * 그래프 이름으로 엔티티 타입 [T]의 EntityGraph를 조회합니다.
 */
inline fun <reified T> StatelessSession.getEntityGraphAs(graphName: String): EntityGraph<T> =
    @Suppress("UNCHECKED_CAST")
    (getEntityGraph(graphName) as EntityGraph<T>)
