package io.bluetape4k.hibernate.reactive.stage

import jakarta.persistence.EntityGraph
import org.hibernate.LockMode
import org.hibernate.reactive.common.AffectedEntities
import org.hibernate.reactive.common.Identifier
import org.hibernate.reactive.common.ResultSetMapping
import org.hibernate.reactive.stage.Stage
import java.util.concurrent.CompletionStage

/**
 * 지정한 식별자로 엔티티를 비동기 조회합니다.
 *
 * ## 동작/계약
 * - `find(T::class.java, id)` 호출 결과 `CompletionStage`를 반환합니다.
 * - 조회 실행 시점은 stage 구독/await 시점이며 세션 자체는 변경하지 않습니다.
 * - 엔티티 미존재 시 provider 정책에 따라 null 결과가 포함될 수 있습니다.
 *
 * ```kotlin
 * val book = session.findAs<Book>(bookId).await()
 * // book.id == bookId
 * ```
 */
inline fun <reified T> Stage.Session.findAs(id: java.io.Serializable): CompletionStage<T> =
    find(T::class.java, id)

/**
 * Hibernate 락 모드를 지정해 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - `find(T::class.java, id, lockMode)`를 호출합니다.
 * - 락 충돌/타임아웃은 stage 실패로 전파됩니다.
 * - 세션 생명주기 및 상태는 기존 컨텍스트를 그대로 사용합니다.
 *
 * ```kotlin
 * val book = session.findAs<Book>(bookId, LockMode.NONE).await()
 * // book.id == bookId
 * ```
 */
inline fun <reified T> Stage.Session.findAs(id: java.io.Serializable, lockMode: LockMode): CompletionStage<T> =
    find(T::class.java, id, lockMode)

/**
 * 여러 식별자로 엔티티 목록을 조회합니다.
 *
 * ## 동작/계약
 * - `find(T::class.java, *ids)`를 그대로 호출합니다.
 * - 결과 순서는 provider/DB 반환 순서를 따릅니다.
 * - 빈 인자 전달 시 빈 리스트가 반환될 수 있습니다.
 *
 * ```kotlin
 * val authors = session.findAs<Author>(author1Id, author2Id).await()
 * // authors.size == 2
 * ```
 */
inline fun <reified T> Stage.Session.findAs(vararg ids: java.io.Serializable): CompletionStage<List<T>> =
    find(T::class.java, *ids)

/**
 * natural id 기반으로 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - `find(T::class.java, naturalId)`를 호출합니다.
 * - natural id 매핑 오류는 stage 실패 예외로 전파됩니다.
 * - 세션을 변경하지 않고 조회 결과만 반환합니다.
 *
 * ```kotlin
 * val author = session.findAs(identifier).await()
 * // author != null
 * ```
 */
inline fun <reified T> Stage.Session.findAs(naturalId: Identifier<T>): CompletionStage<T> =
    find(T::class.java, naturalId)

/**
 * 식별자로 엔티티 레퍼런스를 가져옵니다.
 *
 * ## 동작/계약
 * - `getReference(T::class.java, id)`를 호출합니다.
 * - 프록시 초기화 시점에 엔티티 미존재 예외가 발생할 수 있습니다.
 * - 즉시 DB 접근을 보장하지 않습니다.
 *
 * ```kotlin
 * val ref = session.getReferenceAs<Book>(bookId)
 * // ref.id == bookId
 * ```
 */
inline fun <reified T> Stage.Session.getReferenceAs(id: java.io.Serializable): T =
    getReference(T::class.java, id)

/**
 * 결과 타입을 지정해 HQL/JPQL 조회 쿼리를 생성합니다.
 *
 * ## 동작/계약
 * - `createQuery(queryString, R::class.java)`를 호출합니다.
 * - 잘못된 쿼리 문자열은 생성/실행 시점에 provider 예외를 발생시킵니다.
 * - query 객체를 새로 할당해 반환합니다.
 *
 * ```kotlin
 * val books = session.createQueryAs<Book>("select b from Book b").resultList.await()
 * // books.size == 3
 * ```
 */
inline fun <reified R> Stage.Session.createQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createQuery(queryString, R::class.java)

/**
 * 결과 타입을 지정해 selection query를 생성합니다.
 *
 * ## 동작/계약
 * - `createSelectionQuery(queryString, R::class.java)`를 호출합니다.
 * - 생성된 query는 파라미터 바인딩/페이징 후 재사용 가능합니다.
 * - 문법/매핑 오류는 실행 시점에 예외로 전파됩니다.
 *
 * ```kotlin
 * val count = session.createSelectionQueryAs<Long>("select count(a) from Author a")
 *   .singleResult.await().toLong()
 * // count == 2L
 * ```
 */
inline fun <reified R> Stage.Session.createSelectionQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createSelectionQuery(queryString, R::class.java)

/**
 * 결과 타입을 지정해 named query를 생성합니다.
 *
 * ## 동작/계약
 * - `createNamedQuery(queryName, R::class.java)`를 호출합니다.
 * - 존재하지 않는 [queryName]은 provider 예외를 발생시킵니다.
 * - query 객체를 새로 생성해 반환합니다.
 *
 * ```kotlin
 * val books = session.createNamedQueryAs<Book>("Book.findAll").resultList.await()
 * // books.isNotEmpty() == true
 * ```
 */
inline fun <reified R> Stage.Session.createNamedQueryAs(queryName: String): Stage.SelectionQuery<R> =
    createNamedQuery(queryName, R::class.java)

/**
 * 결과 타입을 지정해 native query를 생성합니다.
 *
 * ## 동작/계약
 * - `createNativeQuery(queryString, R::class.java)`를 호출합니다.
 * - [R] 매핑 불일치 시 실행 시점에 매핑 예외가 발생할 수 있습니다.
 * - 세션은 변경하지 않고 query 객체만 생성합니다.
 *
 * ```kotlin
 * val count = session.createNativeQueryAs<Long>("select count(*) from authors")
 *   .singleResult.await().toLong()
 * // count >= 0L
 * ```
 */
inline fun <reified R> Stage.Session.createNativeQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java)

/**
 * 영향 엔티티 힌트를 포함해 native query를 생성합니다.
 *
 * ## 동작/계약
 * - [affectedEntities]를 전달해 캐시 동기화 힌트를 제공합니다.
 * - SQL/매핑 오류는 stage 실패 예외로 전파됩니다.
 * - query 객체를 새로 생성해 반환합니다.
 *
 * ```kotlin
 * val count = session.createNativeQueryAs<Long>("select count(*) from authors", affectedEntities)
 *   .singleResult.await().toLong()
 * // count >= 0L
 * ```
 */
inline fun <reified R> Stage.Session.createNativeQueryAs(
    queryString: String,
    affectedEntities: AffectedEntities,
): Stage.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java, affectedEntities)

/**
 * 이름으로 ResultSet 매핑을 조회합니다.
 *
 * ## 동작/계약
 * - `getResultSetMapping(T::class.java, mappingName)`를 호출합니다.
 * - 미등록 [mappingName]은 provider 예외를 발생시킵니다.
 * - 매핑 메타데이터를 반환하며 세션 상태를 바꾸지 않습니다.
 *
 * ```kotlin
 * val mapping = session.getResultSetMappingAs<Book>("BookMapping")
 * // mapping != null
 * ```
 */
inline fun <reified T> Stage.Session.getResultSetMappingAs(mappingName: String): ResultSetMapping<T> =
    getResultSetMapping(T::class.java, mappingName)

/**
 * 이름으로 EntityGraph를 조회합니다.
 *
 * ## 동작/계약
 * - `getEntityGraph(T::class.java, graphName)`를 호출합니다.
 * - 미등록 그래프명은 provider 예외를 발생시킵니다.
 * - 조회된 그래프 메타데이터를 그대로 반환합니다.
 *
 * ```kotlin
 * val graph = session.getEntityGraphAs<Book>("Book.withAuthor")
 * // graph.attributeNodes.isNotEmpty() == true
 * ```
 */
inline fun <reified T> Stage.Session.getEntityGraphAs(graphName: String): EntityGraph<T> =
    getEntityGraph(T::class.java, graphName)

/**
 * 타입 기반 빈 EntityGraph를 생성합니다.
 *
 * ## 동작/계약
 * - `createEntityGraph(T::class.java)`를 호출해 새 그래프를 반환합니다.
 * - 호출자는 반환된 그래프에 attribute node를 추가해 사용합니다.
 * - 세션 상태는 변경하지 않습니다.
 *
 * ```kotlin
 * val graph = session.createEntityGraphAs<Book>()
 * graph.addAttributeNodes(Book_.author)
 * // graph.attributeNodes.size == 1
 * ```
 */
inline fun <reified T> Stage.Session.createEntityGraphAs(): EntityGraph<T> =
    createEntityGraph(T::class.java)

/**
 * 이름을 지정해 EntityGraph를 생성합니다.
 *
 * ## 동작/계약
 * - `createEntityGraph(T::class.java, graphName)`를 호출합니다.
 * - 그래프 이름 충돌 처리 규칙은 provider 구현을 따릅니다.
 * - 새 그래프 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val graph = session.createEntityGraphAs<Book>("Book.withAuthor")
 * graph.addAttributeNodes(Book_.author)
 * // graph.name == "Book.withAuthor"
 * ```
 */
inline fun <reified T> Stage.Session.createEntityGraphAs(graphName: String): EntityGraph<T> =
    createEntityGraph(T::class.java, graphName)
