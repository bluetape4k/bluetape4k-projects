package io.bluetape4k.spring.cassandra

import com.datastax.oss.driver.api.core.cql.Statement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.cassandra.ReactiveResultSet
import org.springframework.data.cassandra.core.DeleteOptions
import org.springframework.data.cassandra.core.EntityWriteResult
import org.springframework.data.cassandra.core.InsertOptions
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.data.cassandra.core.UpdateOptions
import org.springframework.data.cassandra.core.WriteResult
import org.springframework.data.cassandra.core.count
import org.springframework.data.cassandra.core.cql.QueryOptions
import org.springframework.data.cassandra.core.delete
import org.springframework.data.cassandra.core.deleteById
import org.springframework.data.cassandra.core.exists
import org.springframework.data.cassandra.core.query
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.Update
import org.springframework.data.cassandra.core.select
import org.springframework.data.cassandra.core.selectOne
import org.springframework.data.cassandra.core.selectOneById
import org.springframework.data.cassandra.core.truncate
import org.springframework.data.cassandra.core.update
import org.springframework.data.domain.Slice

inline fun <reified T: Any> ReactiveCassandraOperations.selectAsFlow(statement: Statement<*>): Flow<T> =
    select<T>(statement).asFlow()

inline fun <reified T: Any> ReactiveCassandraOperations.selectAsFlow(cql: String): Flow<T> =
    select<T>(cql).asFlow()

inline fun <reified T: Any> ReactiveCassandraOperations.selectAsFlow(query: Query): Flow<T> =
    select<T>(query).asFlow()

/**
 * [Statement]로 단건을 조회합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneSuspending(statement: Statement<*>): T =
    selectOne<T>(statement).awaitSingle()

@Deprecated(
    message = "selectOneSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectOneSuspending(statement)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOne(statement: Statement<*>): T =
    selectOneSuspending(statement)

/**
 * [Statement]로 단건을 조회하고 없으면 null을 반환합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneOrNullSuspending(statement: Statement<*>): T? =
    selectOne<T>(statement).awaitSingleOrNull()

@Deprecated(
    message = "selectOneOrNullSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectOneOrNullSuspending(statement)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOneOrNull(statement: Statement<*>): T? =
    selectOneOrNullSuspending(statement)

/**
 * CQL 문자열로 단건을 조회합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneSuspending(cql: String): T =
    selectOne<T>(cql).awaitSingle()

@Deprecated(
    message = "selectOneSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectOneSuspending(cql)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOne(cql: String): T =
    selectOneSuspending(cql)

/**
 * CQL 문자열로 단건을 조회하고 없으면 null을 반환합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneOrNullSuspending(cql: String): T? =
    selectOne<T>(cql).awaitSingleOrNull()

@Deprecated(
    message = "selectOneOrNullSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectOneOrNullSuspending(cql)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOneOrNull(cql: String): T? =
    selectOneOrNullSuspending(cql)

/**
 * [Query]로 단건을 조회합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneSuspending(query: Query): T =
    selectOne<T>(query).awaitSingle()

@Deprecated(
    message = "selectOneSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectOneSuspending(query)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOne(query: Query): T =
    selectOneSuspending(query)

/**
 * [Query]로 단건을 조회하고 없으면 null을 반환합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneOrNullSuspending(query: Query): T? =
    selectOne<T>(query).awaitSingleOrNull()

@Deprecated(
    message = "selectOneOrNullSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectOneOrNullSuspending(query)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOneOrNull(query: Query): T? =
    selectOneOrNullSuspending(query)

/**
 * [Statement]로 [Slice]를 조회합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.sliceSuspending(statement: Statement<*>): Slice<T> =
    slice(statement, T::class.java).awaitSingle()

@Deprecated(
    message = "sliceSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("sliceSuspending(statement)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSlice(statement: Statement<*>): Slice<T> =
    sliceSuspending(statement)

/**
 * [Query]로 [Slice]를 조회합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.sliceSuspending(query: Query): Slice<T> =
    slice(query, T::class.java).awaitSingle()

@Deprecated(
    message = "sliceSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("sliceSuspending(query)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSlice(query: Query): Slice<T> =
    sliceSuspending(query)

/**
 * [Statement]를 실행하고 [ReactiveResultSet]을 반환합니다.
 */
suspend fun ReactiveCassandraOperations.executeSuspending(statement: Statement<*>): ReactiveResultSet =
    execute(statement).awaitSingle()

@Deprecated(
    message = "executeSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("executeSuspending(statement)")
)
suspend fun ReactiveCassandraOperations.suspendExecute(statement: Statement<*>): ReactiveResultSet =
    executeSuspending(statement)

/**
 * [Query]와 [Update]로 갱신하고 성공 여부를 반환합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.updateSuspending(
    query: Query,
    update: Update,
): Boolean =
    update<T>(query, update).awaitSingle()

@Deprecated(
    message = "updateSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("updateSuspending(query, update)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendUpdate(query: Query, update: Update): Boolean =
    updateSuspending<T>(query, update)

/**
 * [Query]로 삭제하고 성공 여부를 반환합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.deleteSuspending(query: Query): Boolean =
    delete<T>(query).awaitSingle()

@Deprecated(
    message = "deleteSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("deleteSuspending(query)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendDelete(query: Query): Boolean =
    deleteSuspending<T>(query)

/**
 * 전체 건수를 반환합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.countSuspending(): Long =
    count<T>().awaitSingle()

@Deprecated(
    message = "countSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("countSuspending()")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendCount(): Long =
    countSuspending<T>()

/**
 * [Query] 조건의 건수를 반환합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.countSuspending(query: Query): Long =
    count<T>(query).awaitSingle()

@Deprecated(
    message = "countSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("countSuspending(query)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendCount(query: Query): Long =
    countSuspending<T>(query)

/**
 * 데이터 존재 여부를 반환합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.existsSuspending(): Boolean =
    query<T>().exists().awaitSingle()

@Deprecated(
    message = "existsSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("existsSuspending()")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendExists(): Boolean =
    existsSuspending<T>()

/**
 * [Query] 조건으로 존재 여부를 반환합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.existsSuspending(query: Query): Boolean =
    exists<T>(query).awaitSingle()

@Deprecated(
    message = "existsSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("existsSuspending(query)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendExists(query: Query): Boolean =
    existsSuspending<T>(query)

/**
 * id 기준으로 존재 여부를 반환합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.existsSuspending(id: Any): Boolean =
    exists<T>(id).awaitSingle()

@Deprecated(
    message = "existsSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("existsSuspending(id)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendExists(id: Any): Boolean =
    existsSuspending<T>(id)

/**
 * id 기준으로 단건을 조회합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneByIdSuspending(id: Any): T =
    selectOneById<T>(id).awaitSingle()

@Deprecated(
    message = "selectOneByIdSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectOneByIdSuspending(id)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOneById(id: Any): T =
    selectOneByIdSuspending(id)

/**
 * id 기준으로 단건을 조회하고 없으면 null을 반환합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneOrNullByIdSuspending(id: Any): T? =
    selectOneById<T>(id).awaitSingleOrNull()

@Deprecated(
    message = "selectOneOrNullByIdSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectOneOrNullByIdSuspending(id)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOneOrNullById(id: Any): T? =
    selectOneOrNullByIdSuspending(id)

/**
 * 엔티티를 저장하고 저장된 엔티티를 반환합니다.
 */
suspend fun <T: Any> ReactiveCassandraOperations.insertSuspending(entity: T): T =
    insert(entity).awaitSingle()

@Deprecated(
    message = "insertSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("insertSuspending(entity)")
)
suspend fun <T: Any> ReactiveCassandraOperations.suspendInsert(entity: T): T =
    insertSuspending(entity)

/**
 * 엔티티를 저장하고 [InsertOptions]를 적용합니다.
 */
suspend fun <T: Any> ReactiveCassandraOperations.insertSuspending(
    entity: T,
    options: InsertOptions,
): EntityWriteResult<T> =
    insert(entity, options).awaitSingle()

@Deprecated(
    message = "insertSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("insertSuspending(entity, options)")
)
suspend fun <T: Any> ReactiveCassandraOperations.suspendInsert(
    entity: T,
    options: InsertOptions,
): EntityWriteResult<T> =
    insertSuspending(entity, options)

/**
 * 엔티티를 갱신하고 갱신된 엔티티를 반환합니다.
 */
suspend fun <T: Any> ReactiveCassandraOperations.updateSuspending(entity: T): T =
    update(entity).awaitSingle()

@Deprecated(
    message = "updateSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("updateSuspending(entity)")
)
suspend fun <T: Any> ReactiveCassandraOperations.suspendUpdate(entity: T): T =
    updateSuspending(entity)

/**
 * 엔티티를 갱신하고 [UpdateOptions]를 적용합니다.
 */
suspend fun <T: Any> ReactiveCassandraOperations.updateSuspending(
    entity: T,
    options: UpdateOptions,
): EntityWriteResult<T> =
    update(entity, options).awaitSingle()

@Deprecated(
    message = "updateSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("updateSuspending(entity, options)")
)
suspend fun <T: Any> ReactiveCassandraOperations.suspendUpdate(
    entity: T,
    options: UpdateOptions,
): EntityWriteResult<T> =
    updateSuspending(entity, options)

/**
 * 엔티티를 삭제하고 삭제된 엔티티를 반환합니다.
 */
suspend fun <T: Any> ReactiveCassandraOperations.deleteSuspending(entity: T): T =
    delete(entity).awaitSingle()

@Deprecated(
    message = "deleteSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("deleteSuspending(entity)")
)
suspend fun <T: Any> ReactiveCassandraOperations.suspendDelete(entity: T): T =
    deleteSuspending(entity)

/**
 * 엔티티를 삭제하고 [QueryOptions]를 적용합니다.
 */
suspend fun <T: Any> ReactiveCassandraOperations.deleteSuspending(
    entity: T,
    options: QueryOptions,
): WriteResult =
    delete(entity, options).awaitSingle()

@Deprecated(
    message = "deleteSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("deleteSuspending(entity, options)")
)
suspend fun <T: Any> ReactiveCassandraOperations.suspendDelete(entity: T, options: QueryOptions): WriteResult =
    deleteSuspending(entity, options)

/**
 * 엔티티를 삭제하고 [DeleteOptions]를 적용합니다.
 */
suspend fun <T: Any> ReactiveCassandraOperations.deleteSuspending(
    entity: T,
    options: DeleteOptions,
): WriteResult =
    delete(entity, options).awaitSingle()

@Deprecated(
    message = "deleteSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("deleteSuspending(entity, options)")
)
suspend fun <T: Any> ReactiveCassandraOperations.suspendDelete(entity: T, options: DeleteOptions): WriteResult =
    deleteSuspending(entity, options)

/**
 * id 기준으로 삭제합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.deleteByIdSuspending(id: Any): Boolean =
    deleteById<T>(id).awaitSingle()

@Deprecated(
    message = "deleteByIdSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("deleteByIdSuspending(id)")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendDeleteById(id: Any): Boolean =
    deleteByIdSuspending<T>(id)

/**
 * 테이블을 truncate 합니다.
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.truncateSuspending() {
    truncate<T>().awaitSingleOrNull()
}

@Deprecated(
    message = "truncateSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("truncateSuspending<T>()")
)
suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendTruncate() {
    truncateSuspending<T>()
}
