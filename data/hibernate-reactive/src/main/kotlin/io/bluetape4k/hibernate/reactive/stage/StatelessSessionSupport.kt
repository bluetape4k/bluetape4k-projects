package io.bluetape4k.hibernate.reactive.stage

import jakarta.persistence.EntityGraph
import org.hibernate.LockMode
import org.hibernate.reactive.common.ResultSetMapping
import org.hibernate.reactive.stage.Stage
import java.util.concurrent.CompletionStage

/**
 * 식별자로 엔티티를 Stage StatelessSession에서 조회합니다.
 *
 * ## 동작/계약
 * - `get(T::class.java, id)` 호출 결과 `CompletionStage`를 반환합니다.
 * - StatelessSession 특성상 1차 캐시는 사용하지 않습니다.
 * - 엔티티 미존재 시 provider 정책에 따라 null 결과가 포함될 수 있습니다.
 *
 * ```kotlin
 * val book = statelessSession.getAs<Book>(bookId).await()
 * // book.id == bookId
 * ```
 */
inline fun <reified T> Stage.StatelessSession.getAs(id: java.io.Serializable): CompletionStage<T> =
    get(T::class.java, id)

/**
 * Hibernate 락 모드를 지정해 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - `get(T::class.java, id, lockMode)`를 호출합니다.
 * - 락 충돌/타임아웃은 stage 실패 예외로 전파됩니다.
 * - 세션 상태는 유지되고 새 세션을 만들지 않습니다.
 *
 * ```kotlin
 * val book = statelessSession.getAs<Book>(bookId, LockMode.NONE).await()
 * // book.id == bookId
 * ```
 */
inline fun <reified T> Stage.StatelessSession.getAs(id: java.io.Serializable, lockMode: LockMode): CompletionStage<T> =
    get(T::class.java, id, lockMode)

/**
 * 결과 타입을 지정해 HQL/JPQL 조회 쿼리를 생성합니다.
 *
 * ## 동작/계약
 * - `createQuery(queryString, R::class.java)`를 호출합니다.
 * - 쿼리 문법/매핑 오류는 실행 시점에 stage 실패로 전파됩니다.
 * - 쿼리 객체를 새로 생성해 반환합니다.
 *
 * ```kotlin
 * val books = statelessSession.createQueryAs<Book>("select b from Book b").resultList.await()
 * // books.size == 3
 * ```
 */
inline fun <reified R> Stage.StatelessSession.createQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createQuery(queryString, R::class.java)

/**
 * 결과 타입을 지정해 selection query를 생성합니다.
 *
 * ## 동작/계약
 * - `createSelectionQuery(queryString, R::class.java)`를 호출합니다.
 * - 반환 query는 파라미터 바인딩/페이징 설정 후 재사용 가능합니다.
 * - 유효하지 않은 쿼리는 실행 시 예외를 발생시킵니다.
 *
 * ```kotlin
 * val count = statelessSession.createSelectionQueryAs<Long>("select count(a) from Author a")
 *   .singleResult.await().toLong()
 * // count == 2L
 * ```
 */
inline fun <reified R> Stage.StatelessSession.createSelectionQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createSelectionQuery(queryString, R::class.java)

/**
 * 결과 타입을 지정해 named query를 생성합니다.
 *
 * ## 동작/계약
 * - `createNamedQuery(queryName, R::class.java)`를 호출합니다.
 * - 존재하지 않는 [queryName]은 provider 예외를 발생시킵니다.
 * - query 객체를 새로 반환합니다.
 *
 * ```kotlin
 * val books = statelessSession.createNamedQueryAs<Book>("Book.findAll").resultList.await()
 * // books.isNotEmpty() == true
 * ```
 */
inline fun <reified R> Stage.StatelessSession.createNamedQueryAs(queryName: String): Stage.SelectionQuery<R> =
    createNamedQuery(queryName, R::class.java)

/**
 * 결과 타입을 지정해 native query를 생성합니다.
 *
 * ## 동작/계약
 * - `createNativeQuery(queryString, R::class.java)`를 호출합니다.
 * - [R] 매핑이 SQL 결과와 다르면 실행 시점 예외가 발생할 수 있습니다.
 * - 세션은 변경하지 않고 query 객체만 생성합니다.
 *
 * ```kotlin
 * val count = statelessSession.createNativeQueryAs<Long>("select count(*) from authors")
 *   .singleResult.await().toLong()
 * // count >= 0L
 * ```
 */
inline fun <reified R> Stage.StatelessSession.createNativeQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java)

/**
 * 이름으로 ResultSet 매핑을 조회합니다.
 *
 * ## 동작/계약
 * - `getResultSetMapping(T::class.java, mappingName)`를 호출합니다.
 * - 미등록 [mappingName]은 provider 예외를 발생시킵니다.
 * - 매핑 메타데이터를 반환하며 세션 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val mapping = statelessSession.getResultSetMappingAs<Book>("BookMapping")
 * // mapping != null
 * ```
 */
inline fun <reified T> Stage.StatelessSession.getResultSetMappingAs(mappingName: String): ResultSetMapping<T> =
    getResultSetMapping(T::class.java, mappingName)

/**
 * 이름으로 EntityGraph를 조회합니다.
 *
 * ## 동작/계약
 * - `getEntityGraph(T::class.java, graphName)`를 호출합니다.
 * - 그래프가 등록되지 않았으면 provider 예외가 발생합니다.
 * - 세션 상태를 바꾸지 않고 그래프 메타데이터를 반환합니다.
 *
 * ```kotlin
 * val graph = statelessSession.getEntityGraphAs<Book>("Book.withAuthor")
 * // graph.attributeNodes.isNotEmpty() == true
 * ```
 */
inline fun <reified T> Stage.StatelessSession.getEntityGraphAs(graphName: String): EntityGraph<T> =
    getEntityGraph(T::class.java, graphName)

/**
 * 타입 기반 빈 EntityGraph를 생성합니다.
 *
 * ## 동작/계약
 * - `createEntityGraph(T::class.java)`를 호출해 새 그래프를 반환합니다.
 * - 반환 그래프는 호출자가 attribute node를 추가해 사용합니다.
 * - 세션 자체 상태는 변경되지 않습니다.
 *
 * ```kotlin
 * val graph = statelessSession.createEntityGraphAs<Book>()
 * graph.addAttributeNodes(Book_.author)
 * // graph.attributeNodes.size == 1
 * ```
 */
inline fun <reified T> Stage.StatelessSession.createEntityGraphAs(): EntityGraph<T> =
    createEntityGraph(T::class.java)

/**
 * 이름을 지정해 EntityGraph를 생성합니다.
 *
 * ## 동작/계약
 * - `createEntityGraph(T::class.java, graphName)`를 호출합니다.
 * - 이름 충돌/등록 규칙은 provider 구현을 따릅니다.
 * - 새 그래프 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val graph = statelessSession.createEntityGraphAs<Book>("Book.withAuthor")
 * graph.addAttributeNodes(Book_.author)
 * // graph.name == "Book.withAuthor"
 * ```
 */
inline fun <reified T> Stage.StatelessSession.createEntityGraphAs(graphName: String): EntityGraph<T> =
    createEntityGraph(T::class.java, graphName)
