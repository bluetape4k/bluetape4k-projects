package io.bluetape4k.spring.r2dbc.coroutines

import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.isEqual

/**
 * id 기준으로 단건을 조회합니다.
 *
 * @param id 조회할 id
 * @param idName id 컬럼명
 * @return 조회된 엔티티
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.findOneByIdSuspending(
    id: Any,
    idName: String = "id",
): T {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return selectOneSuspending(query)
}

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
 * id 기준으로 단건을 조회하고 없으면 null을 반환합니다.
 *
 * @param id 조회할 id
 * @param idName id 컬럼명
 * @return 조회된 엔티티 또는 null
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.findOneByIdOrNullSuspending(
    id: Any,
    idName: String = "id",
): T? {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return selectOneOrNullSuspending(query)
}

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
 * id 기준으로 첫 번째 엔티티를 조회합니다.
 *
 * @param id 조회할 id
 * @param idName id 컬럼명
 * @return 조회된 엔티티
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.findFirstByIdSuspending(
    id: Any,
    idName: String = "id",
): T {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return selectFirstSuspending(query)
}

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
 * id 기준으로 첫 번째 엔티티를 조회하고 없으면 null을 반환합니다.
 *
 * @param id 조회할 id
 * @param idName id 컬럼명
 * @return 조회된 엔티티 또는 null
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.findFirstByIdOrNullSuspending(
    id: Any,
    idName: String = "id",
): T? {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return selectFirstOrNullSuspending(query)
}

@Deprecated(
    message = "findFirstByIdOrNullSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("findFirstByIdOrNullSuspending(id, idName)"),
)
suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendFindFirstByIdOrNull(
    id: Any,
    idName: String = "id",
): T? =
    findFirstByIdOrNullSuspending(id, idName)
