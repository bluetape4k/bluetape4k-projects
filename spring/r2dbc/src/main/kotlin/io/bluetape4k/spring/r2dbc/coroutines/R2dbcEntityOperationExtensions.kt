package io.bluetape4k.spring.r2dbc.coroutines

import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.isEqual

/**
 * 지정한 id 컬럼 조건으로 단건을 조회합니다.
 *
 * ## 동작/계약
 * - `Query.where(idName).isEqual(id)`를 생성한 뒤 `selectOneSuspending`으로 위임합니다.
 * - 결과가 2건 이상이면 하위 API에서 `IncorrectResultSizeDataAccessException`이 발생할 수 있습니다.
 * - 수신 객체 상태를 변경하지 않고 조회 쿼리 객체만 새로 생성합니다.
 *
 * ```kotlin
 * val post = operations.findOneByIdSuspending<Post>(1L)
 * // post.id == 1L
 * ```
 *
 * @param id 조회할 식별자 값
 * @param idName 식별자 컬럼명
 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException 조회 결과가 단건이 아닐 때 발생할 수 있습니다.
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.findOneByIdSuspending(
    id: Any,
    idName: String = "id",
): T {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return selectOneSuspending(query)
}

/**
 * [findOneByIdSuspending]의 이전 이름을 제공합니다.
 *
 * ## 동작/계약
 * - 구현은 [findOneByIdSuspending]에 그대로 위임됩니다.
 * - 동작/예외 계약은 대상 함수와 동일합니다.
 *
 * ```kotlin
 * val post = operations.suspendFindOneById<Post>(1L)
 * // post.id == 1L
 * ```
 */
@Deprecated(
    message = "findOneByIdSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("findOneByIdSuspending(id, idName)"),
)
suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendFindOneById(
    id: Any,
    idName: String = "id",
): T =
    findOneByIdSuspending(id, idName)

/**
 * 지정한 id 컬럼 조건으로 단건 조회를 수행하고 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `selectOneOrNullSuspending`에 위임합니다.
 * - 결과가 없으면 `null`을 반환하고, 복수 건이면 하위 API 예외가 전파될 수 있습니다.
 * - 조회용 [Query]를 새로 생성하며 수신 객체는 변경하지 않습니다.
 *
 * ```kotlin
 * val missing = operations.findOneByIdOrNullSuspending<Post>(-1L)
 * // missing == null
 * ```
 *
 * @param id 조회할 식별자 값
 * @param idName 식별자 컬럼명
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.findOneByIdOrNullSuspending(
    id: Any,
    idName: String = "id",
): T? {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return selectOneOrNullSuspending(query)
}

/**
 * [findOneByIdOrNullSuspending]의 이전 이름을 제공합니다.
 *
 * ## 동작/계약
 * - 구현은 [findOneByIdOrNullSuspending]에 위임됩니다.
 * - 결과가 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val post = operations.suspendFindOneByIdOrNull<Post>(-1L)
 * // post == null
 * ```
 */
@Deprecated(
    message = "findOneByIdOrNullSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("findOneByIdOrNullSuspending(id, idName)"),
)
suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendFindOneByIdOrNull(
    id: Any,
    idName: String = "id",
): T? =
    findOneByIdOrNullSuspending(id, idName)

/**
 * 지정한 id 컬럼 조건으로 첫 번째 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - `selectFirstSuspending`으로 위임하므로 첫 요소만 반환합니다.
 * - 결과가 없으면 하위 API에서 `NoSuchElementException` 계열 예외가 전파될 수 있습니다.
 * - 조회 조건 [Query]를 새로 할당합니다.
 *
 * ```kotlin
 * val first = operations.findFirstByIdSuspending<Post>(1L)
 * // first.id == 1L
 * ```
 *
 * @param id 조회할 식별자 값
 * @param idName 식별자 컬럼명
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.findFirstByIdSuspending(
    id: Any,
    idName: String = "id",
): T {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return selectFirstSuspending(query)
}

/**
 * [findFirstByIdSuspending]의 이전 이름을 제공합니다.
 *
 * ## 동작/계약
 * - 내부 구현은 [findFirstByIdSuspending]과 동일합니다.
 * - 첫 번째 결과만 반환합니다.
 *
 * ```kotlin
 * val first = operations.suspendFindFirstById<Post>(1L)
 * // first.id == 1L
 * ```
 */
@Deprecated(
    message = "findFirstByIdSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("findFirstByIdSuspending(id, idName)"),
)
suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendFindFirstById(
    id: Any,
    idName: String = "id",
): T =
    findFirstByIdSuspending(id, idName)

/**
 * 지정한 id 컬럼 조건으로 첫 번째 엔티티를 조회하고 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - `selectFirstOrNullSuspending`에 위임하여 첫 요소만 소비합니다.
 * - 결과가 없으면 `null`을 반환합니다.
 * - 조회 조건 객체를 새로 만들며 수신 객체는 변경하지 않습니다.
 *
 * ```kotlin
 * val first = operations.findFirstByIdOrNullSuspending<Post>(-1L)
 * // first == null
 * ```
 *
 * @param id 조회할 식별자 값
 * @param idName 식별자 컬럼명
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.findFirstByIdOrNullSuspending(
    id: Any,
    idName: String = "id",
): T? {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return selectFirstOrNullSuspending(query)
}

/**
 * [findFirstByIdOrNullSuspending]의 이전 이름을 제공합니다.
 *
 * ## 동작/계약
 * - 구현은 [findFirstByIdOrNullSuspending]에 위임됩니다.
 * - 결과가 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val first = operations.suspendFindFirstByIdOrNull<Post>(-1L)
 * // first == null
 * ```
 */
@Deprecated(
    message = "findFirstByIdOrNullSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("findFirstByIdOrNullSuspending(id, idName)"),
)
suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendFindFirstByIdOrNull(
    id: Any,
    idName: String = "id",
): T? =
    findFirstByIdOrNullSuspending(id, idName)
