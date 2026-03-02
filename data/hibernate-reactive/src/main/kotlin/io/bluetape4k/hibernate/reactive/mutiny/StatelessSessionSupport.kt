package io.bluetape4k.hibernate.reactive.mutiny

import io.smallrye.mutiny.Uni
import jakarta.persistence.EntityGraph
import jakarta.persistence.LockModeType
import org.hibernate.LockMode
import org.hibernate.reactive.common.ResultSetMapping
import org.hibernate.reactive.mutiny.Mutiny

/**
 * 식별자로 엔티티를 StatelessSession에서 조회합니다.
 *
 * ## 동작/계약
 * - `get(T::class.java, id)`를 호출한 `Uni`를 반환합니다.
 * - StatelessSession 특성상 1차 캐시 없이 조회가 수행됩니다.
 * - 미존재 엔티티는 provider 정책에 따라 null 결과가 반환될 수 있습니다.
 *
 * ```kotlin
 * val book = statelessSession.getAs<Book>(bookId).awaitSuspending()
 * // book.id == bookId
 * ```
 */
inline fun <reified T> Mutiny.StatelessSession.getAs(id: java.io.Serializable): Uni<T> =
    get(T::class.java, id)

/**
 * Hibernate 락 모드를 지정해 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - `get(T::class.java, id, lockMode)`를 호출합니다.
 * - 락 획득 실패는 `Uni` 실패로 전파됩니다.
 * - 세션 인스턴스는 재사용되며 새 세션을 생성하지 않습니다.
 *
 * ```kotlin
 * val book = statelessSession.getAs<Book>(bookId, LockMode.NONE).awaitSuspending()
 * // book.id == bookId
 * ```
 */
inline fun <reified T> Mutiny.StatelessSession.getAs(id: java.io.Serializable, lockMode: LockMode): Uni<T> =
    get(T::class.java, id, lockMode)

/**
 * JPA 락 모드를 지정해 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - `get(T::class.java, id, lockModeType)`를 호출합니다.
 * - 락 관련 오류는 Hibernate/JPA 예외로 `Uni` 실패에 반영됩니다.
 * - 조회 이외에 수신 객체 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val book = statelessSession.getAs<Book>(bookId, LockModeType.NONE).awaitSuspending()
 * // book.id == bookId
 * ```
 */
inline fun <reified T> Mutiny.StatelessSession.getAs(id: java.io.Serializable, lockModeType: LockModeType): Uni<T> =
    get(T::class.java, id, lockModeType)

/**
 * EntityGraph를 적용해 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - `get(entityGraph, id)`를 호출해 그래프 기반 fetch plan을 적용합니다.
 * - 그래프 타입/속성 불일치는 provider 예외를 발생시킬 수 있습니다.
 * - stateless 세션 특성은 그대로 유지됩니다.
 *
 * ```kotlin
 * val graph = statelessSession.createEntityGraphAs<Book>().apply { addAttributeNodes(Book_.author) }
 * val book = statelessSession.getAs(graph, bookId).awaitSuspending()
 * // book.author != null
 * ```
 */
inline fun <reified T> Mutiny.StatelessSession.getAs(entityGraph: EntityGraph<T>, id: java.io.Serializable): Uni<T> =
    get(entityGraph, id)

/**
 * 그래프 이름으로 EntityGraph를 조회해 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - [graphName]으로 `getEntityGraphAs<T>`를 조회한 뒤 `get`을 수행합니다.
 * - 그래프 이름이 없으면 provider 예외가 전파됩니다.
 * - 세션 상태는 유지되고 조회 요청만 수행됩니다.
 *
 * ```kotlin
 * val book = statelessSession.getAs<Book>("Book.withAuthor", bookId).awaitSuspending()
 * // book.id == bookId
 * ```
 */
inline fun <reified T> Mutiny.StatelessSession.getAs(graphName: String, id: java.io.Serializable): Uni<T> =
    get(getEntityGraphAs<T>(graphName), id)

/**
 * 결과 타입을 지정해 HQL/JPQL 조회 쿼리를 생성합니다.
 *
 * ## 동작/계약
 * - `createQuery(queryString, R::class.java)`를 호출합니다.
 * - 쿼리 파싱/매핑 오류는 실행 시점에 `Uni` 실패로 전파됩니다.
 * - 새 query 객체를 반환하며 세션 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val books = statelessSession.createQueryAs<Book>("select b from Book b").resultList.awaitSuspending()
 * // books.size == 3
 * ```
 */
inline fun <reified R> Mutiny.StatelessSession.createQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createQuery(queryString, R::class.java)

/**
 * 결과 타입을 지정해 selection query를 생성합니다.
 *
 * ## 동작/계약
 * - `createSelectionQuery(queryString, R::class.java)`를 호출합니다.
 * - 생성된 query는 파라미터/페이징 설정 후 재사용 가능합니다.
 * - 문법 또는 매핑 오류는 실행 시 예외로 전파됩니다.
 *
 * ```kotlin
 * val count = statelessSession.createSelectionQueryAs<Long>("select count(a) from Author a")
 *   .singleResult.awaitSuspending().toLong()
 * // count == 2L
 * ```
 */
inline fun <reified R> Mutiny.StatelessSession.createSelectionQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createSelectionQuery(queryString, R::class.java)

/**
 * 결과 타입을 지정해 named query를 생성합니다.
 *
 * ## 동작/계약
 * - `createNamedQuery(queryName, R::class.java)`를 호출합니다.
 * - 미등록 [queryName]은 provider 예외를 발생시킵니다.
 * - query 객체를 새로 생성해 반환합니다.
 *
 * ```kotlin
 * val books = statelessSession.createNamedQueryAs<Book>("Book.findAll").resultList.awaitSuspending()
 * // books.isNotEmpty() == true
 * ```
 */
inline fun <reified R> Mutiny.StatelessSession.createNamedQueryAs(queryName: String): Mutiny.SelectionQuery<R> =
    createNamedQuery(queryName, R::class.java)

/**
 * 결과 타입을 지정해 native query를 생성합니다.
 *
 * ## 동작/계약
 * - `createNativeQuery(queryString, R::class.java)`를 호출합니다.
 * - 결과 타입 매핑 불일치 시 실행 시점 예외가 발생할 수 있습니다.
 * - 세션 상태는 변경하지 않고 query 객체를 반환합니다.
 *
 * ```kotlin
 * val count = statelessSession.createNativeQueryAs<Long>("select count(*) from authors")
 *   .singleResult.awaitSuspending().toLong()
 * // count >= 0L
 * ```
 */
inline fun <reified R> Mutiny.StatelessSession.createNativeQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java)

/**
 * 이름으로 ResultSet 매핑을 조회합니다.
 *
 * ## 동작/계약
 * - `getResultSetMapping(T::class.java, mappingName)`를 호출합니다.
 * - 미등록 [mappingName]은 provider 예외를 발생시킵니다.
 * - 조회된 매핑 메타데이터를 그대로 반환합니다.
 *
 * ```kotlin
 * val mapping = statelessSession.getResultSetMappingAs<Book>("BookMapping")
 * // mapping != null
 * ```
 */
inline fun <reified T> Mutiny.StatelessSession.getResultSetMappingAs(mappingName: String): ResultSetMapping<T> =
    getResultSetMapping(T::class.java, mappingName)

/**
 * 이름으로 EntityGraph를 조회합니다.
 *
 * ## 동작/계약
 * - `getEntityGraph(T::class.java, graphName)`를 호출합니다.
 * - 그래프 이름이 없으면 provider 예외가 발생합니다.
 * - 세션 상태는 변경하지 않고 그래프 메타데이터를 반환합니다.
 *
 * ```kotlin
 * val graph = statelessSession.getEntityGraphAs<Book>("Book.withAuthor")
 * // graph.attributeNodes.isNotEmpty() == true
 * ```
 */
inline fun <reified T> Mutiny.StatelessSession.getEntityGraphAs(graphName: String): EntityGraph<T> =
    getEntityGraph(T::class.java, graphName)

/**
 * 타입 기반 빈 EntityGraph를 생성합니다.
 *
 * ## 동작/계약
 * - `createEntityGraph(T::class.java)`를 호출해 새 그래프를 반환합니다.
 * - 반환 그래프는 호출자가 attribute node를 추가해 사용합니다.
 * - 세션 상태는 유지됩니다.
 *
 * ```kotlin
 * val graph = statelessSession.createEntityGraphAs<Book>()
 * graph.addAttributeNodes(Book_.author)
 * // graph.attributeNodes.size == 1
 * ```
 */
inline fun <reified T> Mutiny.StatelessSession.createEntityGraphAs(): EntityGraph<T> =
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
inline fun <reified T> Mutiny.StatelessSession.createEntityGraphAs(graphName: String): EntityGraph<T> =
    createEntityGraph(T::class.java, graphName)
