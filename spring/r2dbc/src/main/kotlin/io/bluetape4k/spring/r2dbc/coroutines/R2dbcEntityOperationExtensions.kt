package io.bluetape4k.spring.r2dbc.coroutines

import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.isEqual

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendFindOneById(id: Any, idName: String = "id"): T {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return suspendSelectOne(query)
}

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendFindOneByIdOrNull(id: Any, idName: String = "id"): T? {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return suspendSelectOneOrNull(query)
}

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendFindFirstById(id: Any, idName: String = "id"): T {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return suspendSelectFirst(query)
}

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendFindFirstByIdOrNull(
    id: Any,
    idName: String = "id",
): T? {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return suspendSelectFirstOrNull(query)
}
