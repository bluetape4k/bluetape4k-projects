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
 * 지정한 식별자로 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - `find(T::class.java, id)`를 호출한 `Uni`를 반환합니다.
 * - DB 조회는 구독 시점에 수행되며 수신 세션 상태는 직접 변경하지 않습니다.
 * - 엔티티 미존재 시 Hibernate Reactive 정책에 따라 `null` 결과를 포함한 `Uni`가 반환될 수 있습니다.
 *
 * ```kotlin
 * val book = session.findAs<Book>(bookId).awaitSuspending()
 * // book.id == bookId
 * ```
 */
inline fun <reified T> Mutiny.Session.findAs(id: java.io.Serializable): Uni<T> =
    find(T::class.java, id)

/**
 * 지정한 식별자와 Hibernate 락 모드로 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - `find(T::class.java, id, lockMode)`를 호출합니다.
 * - 락 획득 실패/타임아웃 예외는 `Uni` 실패로 전파됩니다.
 * - 세션 객체 자체를 새로 만들지 않고 기존 세션에서 조회를 수행합니다.
 *
 * ```kotlin
 * val book = session.findAs<Book>(bookId, LockMode.NONE).awaitSuspending()
 * // book.id == bookId
 * ```
 */
inline fun <reified T> Mutiny.Session.findAs(id: java.io.Serializable, lockMode: LockMode): Uni<T> =
    find(T::class.java, id, lockMode)

/**
 * 지정한 식별자와 JPA 락 모드로 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - `find(T::class.java, id, lockModeType)`를 호출합니다.
 * - 락 관련 오류는 Hibernate/JPA 예외로 `Uni` 실패에 반영됩니다.
 * - 수신 세션은 재사용되며 추가 세션을 할당하지 않습니다.
 *
 * ```kotlin
 * val book = session.findAs<Book>(bookId, LockModeType.NONE).awaitSuspending()
 * // book.id == bookId
 * ```
 */
inline fun <reified T> Mutiny.Session.findAs(id: java.io.Serializable, lockModeType: LockModeType): Uni<T> =
    find(T::class.java, id, lockModeType)

/**
 * 여러 식별자로 엔티티 목록을 조회합니다.
 *
 * ## 동작/계약
 * - 가변 인자 [ids]를 그대로 전달해 `find(T::class.java, *ids)`를 호출합니다.
 * - 결과 리스트 순서는 Hibernate 구현과 DB 반환 순서를 따릅니다.
 * - 빈 인자를 전달하면 빈 리스트를 포함한 `Uni`가 반환될 수 있습니다.
 *
 * ```kotlin
 * val authors = session.findAs<Author>(author1Id, author2Id).awaitSuspending()
 * // authors.size == 2
 * ```
 */
inline fun <reified T> Mutiny.Session.findAs(vararg ids: java.io.Serializable): Uni<List<T>> =
    find(T::class.java, *ids)

/**
 * natural id로 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - `find(T::class.java, naturalId)`를 그대로 호출합니다.
 * - natural id가 매핑되지 않았거나 불일치하면 provider 예외가 전파될 수 있습니다.
 * - 세션을 변경하지 않고 조회 결과만 비동기로 반환합니다.
 *
 * ```kotlin
 * val author = session.findAs(identifier).awaitSuspending()
 * // author != null
 * ```
 */
inline fun <reified T> Mutiny.Session.findAs(naturalId: Identifier<T>): Uni<T> =
    find(T::class.java, naturalId)

/**
 * EntityGraph를 fetch plan으로 적용해 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - 전달한 [entityGraph]를 사용해 `find(entityGraph, id)`를 호출합니다.
 * - 그래프에 포함된 연관은 즉시 fetch 되며, 미포함 연관은 기본 fetch 전략을 따릅니다.
 * - 잘못된 그래프 타입/속성은 provider 예외로 전파됩니다.
 *
 * ```kotlin
 * val graph = session.createEntityGraphAs<Book>().apply { addAttributeNodes(Book_.author) }
 * val book = session.findAs(graph, bookId).awaitSuspending()
 * // book.author != null
 * ```
 */
inline fun <reified T> Mutiny.Session.findAs(entityGraph: EntityGraph<T>, id: java.io.Serializable): Uni<T> =
    find(entityGraph, id)

/**
 * 등록된 그래프 이름으로 fetch plan을 조회해 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - [graphName]으로 `getEntityGraphAs<T>`를 조회한 뒤 `find`를 수행합니다.
 * - 그래프 이름이 없으면 Hibernate 예외가 발생합니다.
 * - 세션 상태는 유지되며 조회 요청만 추가됩니다.
 *
 * ```kotlin
 * val book = session.findAs<Book>("Book.withAuthor", bookId).awaitSuspending()
 * // book.id == bookId
 * ```
 */
inline fun <reified T> Mutiny.Session.findAs(graphName: String, id: java.io.Serializable): Uni<T> =
    find(getEntityGraphAs<T>(graphName), id)

/**
 * 식별자로 엔티티 레퍼런스(프록시 가능)를 가져옵니다.
 *
 * ## 동작/계약
 * - `getReference(T::class.java, id)`를 그대로 호출합니다.
 * - 실제 DB 접근은 프록시 초기화 시점으로 지연될 수 있습니다.
 * - 식별자가 잘못되면 초기화 시점에 `EntityNotFoundException` 계열 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = session.getReferenceAs<Book>(bookId)
 * // ref.id == bookId
 * ```
 */
inline fun <reified T> Mutiny.Session.getReferenceAs(id: java.io.Serializable): T =
    getReference(T::class.java, id)

/**
 * 결과 타입을 지정해 HQL/JPQL 조회 쿼리를 생성합니다.
 *
 * ## 동작/계약
 * - `createQuery(queryString, R::class.java)`를 호출해 typed query를 반환합니다.
 * - 쿼리 문자열 파싱/매핑 오류는 실행 또는 준비 단계에서 provider 예외로 전파됩니다.
 * - 쿼리 객체를 새로 할당하며 세션 자체를 mutate 하지 않습니다.
 *
 * ```kotlin
 * val query = session.createQueryAs<Book>("select b from Book b")
 * val books = query.resultList.awaitSuspending()
 * // books.size == 3
 * ```
 */
inline fun <reified R> Mutiny.Session.createQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createQuery(queryString, R::class.java)

/**
 * 결과 타입을 지정해 selection query를 생성합니다.
 *
 * ## 동작/계약
 * - `createSelectionQuery(queryString, R::class.java)`를 호출합니다.
 * - 반환된 query 인스턴스는 호출자 쪽에서 파라미터/페이징을 설정해 재사용할 수 있습니다.
 * - 유효하지 않은 쿼리는 Hibernate 예외로 전파됩니다.
 *
 * ```kotlin
 * val query = session.createSelectionQueryAs<Long>("select count(a) from Author a")
 * val count = query.singleResult.awaitSuspending().toLong()
 * // count == 2L
 * ```
 */
inline fun <reified R> Mutiny.Session.createSelectionQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createSelectionQuery(queryString, R::class.java)

/**
 * 결과 타입을 지정해 named query를 생성합니다.
 *
 * ## 동작/계약
 * - `createNamedQuery(queryName, R::class.java)`를 호출합니다.
 * - 등록되지 않은 [queryName]은 provider 예외를 발생시킵니다.
 * - 쿼리 객체를 새로 생성해 반환합니다.
 *
 * ```kotlin
 * val query = session.createNamedQueryAs<Book>("Book.findAll")
 * val books = query.resultList.awaitSuspending()
 * // books.isNotEmpty() == true
 * ```
 */
inline fun <reified R> Mutiny.Session.createNamedQueryAs(queryName: String): Mutiny.SelectionQuery<R> =
    createNamedQuery(queryName, R::class.java)

/**
 * 결과 타입을 지정해 native query를 생성합니다.
 *
 * ## 동작/계약
 * - `createNativeQuery(queryString, R::class.java)`를 호출합니다.
 * - 반환 타입 [R] 매핑이 맞지 않으면 실행 시 매핑 예외가 발생할 수 있습니다.
 * - query 객체를 새로 생성해 반환합니다.
 *
 * ```kotlin
 * val query = session.createNativeQueryAs<Long>("select count(*) from authors")
 * val count = query.singleResult.awaitSuspending().toLong()
 * // count >= 0L
 * ```
 */
inline fun <reified R> Mutiny.Session.createNativeQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java)

/**
 * 영향 엔티티 정보를 포함해 native query를 생성합니다.
 *
 * ## 동작/계약
 * - [affectedEntities]를 함께 전달해 2차 캐시 동기화 힌트를 제공합니다.
 * - native SQL/매핑 오류는 실행 시 provider 예외로 전파됩니다.
 * - 세션을 변경하지 않고 query 객체를 생성해 반환합니다.
 *
 * ```kotlin
 * val query = session.createNativeQueryAs<Long>("select count(*) from authors", affectedEntities)
 * val count = query.singleResult.awaitSuspending().toLong()
 * // count >= 0L
 * ```
 */
inline fun <reified R> Mutiny.Session.createNativeQueryAs(
    queryString: String,
    affectedEntities: AffectedEntities,
): Mutiny.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java, affectedEntities)

/**
 * 이름으로 ResultSet 매핑을 조회합니다.
 *
 * ## 동작/계약
 * - `getResultSetMapping(T::class.java, mappingName)`를 호출합니다.
 * - 등록되지 않은 [mappingName]은 provider 예외를 발생시킵니다.
 * - 조회된 매핑 메타데이터 객체를 반환하며 세션 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val mapping = session.getResultSetMappingAs<Book>("BookMapping")
 * // mapping != null
 * ```
 */
inline fun <reified T> Mutiny.Session.getResultSetMappingAs(mappingName: String): ResultSetMapping<T> =
    getResultSetMapping(T::class.java, mappingName)

/**
 * 이름으로 EntityGraph를 조회합니다.
 *
 * ## 동작/계약
 * - `getEntityGraph(T::class.java, graphName)`를 호출합니다.
 * - 미등록 그래프명은 provider 예외를 발생시킵니다.
 * - 그래프 메타데이터를 반환하며 세션 데이터는 변경하지 않습니다.
 *
 * ```kotlin
 * val graph = session.getEntityGraphAs<Book>("Book.withAuthor")
 * // graph.attributeNodes.isNotEmpty() == true
 * ```
 */
inline fun <reified T> Mutiny.Session.getEntityGraphAs(graphName: String): EntityGraph<T> =
    getEntityGraph(T::class.java, graphName)

/**
 * 타입 기반의 빈 EntityGraph를 생성합니다.
 *
 * ## 동작/계약
 * - `createEntityGraph(T::class.java)`를 호출해 새 그래프를 할당합니다.
 * - 반환 그래프는 호출자에서 attribute node를 추가해 사용합니다.
 * - 세션은 유지되고 그래프 객체만 새로 생성됩니다.
 *
 * ```kotlin
 * val graph = session.createEntityGraphAs<Book>()
 * graph.addAttributeNodes(Book_.author)
 * // graph.attributeNodes.size == 1
 * ```
 */
inline fun <reified T> Mutiny.Session.createEntityGraphAs(): EntityGraph<T> =
    createEntityGraph(T::class.java)

/**
 * 이름을 지정해 EntityGraph를 생성합니다.
 *
 * ## 동작/계약
 * - `createEntityGraph(T::class.java, graphName)`를 호출합니다.
 * - 중복 이름/등록 정책은 JPA provider 구현을 따릅니다.
 * - 새 그래프 인스턴스를 반환하며 세션 자체는 변경하지 않습니다.
 *
 * ```kotlin
 * val graph = session.createEntityGraphAs<Book>("Book.withAuthor")
 * graph.addAttributeNodes(Book_.author)
 * // graph.name == "Book.withAuthor"
 * ```
 */
inline fun <reified T> Mutiny.Session.createEntityGraphAs(graphName: String): EntityGraph<T> =
    createEntityGraph(T::class.java, graphName)
