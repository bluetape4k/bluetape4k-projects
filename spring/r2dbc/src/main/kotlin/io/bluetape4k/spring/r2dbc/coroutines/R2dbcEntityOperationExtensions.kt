package io.bluetape4k.spring.r2dbc.coroutines

import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.isEqual

suspend inline fun <reified T: Any> R2dbcEntityOperations.awaitFindOneById(id: Any, idName: String = "id"): T {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return awaitSelectOne(query)
}

@Deprecated("Use awaitFindOneById instead", ReplaceWith("awaitFindOneById(id, idName)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coFindOneById(id: Any, idName: String = "id"): T {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return coSelectOne(query)
}

suspend inline fun <reified T: Any> R2dbcEntityOperations.awaitFindOneByIdOrNull(id: Any, idName: String = "id"): T? {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return awaitSelectOneOrNull(query)
}

@Deprecated("Use awaitFindOneByIdOrNull instead", ReplaceWith("awaitFindOneByIdOrNull(id, idName)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coFindOneByIdOrNull(id: Any, idName: String = "id"): T? {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return coSelectOneOrNull(query)
}

suspend inline fun <reified T: Any> R2dbcEntityOperations.awaitFindFirstById(id: Any, idName: String = "id"): T {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return awaitSelectFirst(query)
}

@Deprecated("Use awaitFindFirstById instead", ReplaceWith("awaitFindFirstById(id, idName)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coFindFirstById(id: Any, idName: String = "id"): T {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return coSelectFirst(query)
}

suspend inline fun <reified T: Any> R2dbcEntityOperations.awaitFindFirstByIdOrNull(id: Any, idName: String = "id"): T? {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return awaitSelectFirstOrNull(query)
}

@Deprecated("Use awaitFindFirstByIdOrNull instead", ReplaceWith("awaitFindFirstByIdOrNull(id, idName)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coFindFirstByIdOrNull(id: Any, idName: String = "id"): T? {
    val query = Query.query(Criteria.where(idName).isEqual(id))
    return coSelectFirstOrNull(query)
}
