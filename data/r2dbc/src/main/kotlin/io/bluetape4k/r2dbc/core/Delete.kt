package io.bluetape4k.r2dbc.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.r2dbc.R2dbcClient
import io.bluetape4k.r2dbc.query.Query
import io.bluetape4k.r2dbc.support.bindMap
import org.springframework.data.r2dbc.core.ReactiveDeleteOperation
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.FetchSpec
import reactor.core.publisher.Mono

/**
 * 삭제를 수행하는 [DeleteTableSpec] 을 생성합니다.
 *
 * @return [DeleteTableSpec] 인스턴스
 */
fun R2dbcClient.delete(): DeleteTableSpec = DeleteTableSpecImpl(this)

/**
 * 데이트를 삭제하기 위한 SQL 구문을 생성합니다
 */
interface DeleteTableSpec {
    fun from(table: String): DeleteValueSpec
    fun <T> from(type: Class<T>): ReactiveDeleteOperation.ReactiveDelete
}

inline fun <reified T: Any> DeleteTableSpec.from(): ReactiveDeleteOperation.ReactiveDelete =
    from(T::class.java)

private class DeleteTableSpecImpl(private val client: io.bluetape4k.r2dbc.R2dbcClient): DeleteTableSpec {

    override fun from(table: String): DeleteValueSpec =
        DeleteValueSpecImpl(client, table)

    override fun <T> from(type: Class<T>): ReactiveDeleteOperation.ReactiveDelete =
        client.entityTemplate.delete(type)
}

/**
 * 삭제를 위한 SQL 구문을 생성합니다.
 */
interface DeleteValueSpec {
    /**
     * 삭제를 위한 SQL 구문을 생성합니다.
     *
     * ```
     * val client: R2dbcClient = ...
     *
     * client.delete()
     *      .from("Posts")
     *      .matching("id", mapOf("id", 1L))
     *      .fetch()
     *      .awaitSingleOrNull()
     * ```
     * @param where
     * @param whereParameters
     * @return
     */
    fun matching(
        where: String? = null,
        whereParameters: Map<String, Any?>? = null,
    ): DatabaseClient.GenericExecuteSpec

    /**
     * 삭제를 위한 SQL 구문을 생성합니다.
     *
     * @param query 삭제 조건에 해당하는 [Query]
     * @return [DatabaseClient.GenericExecuteSpec] 인스턴스
     */
    fun matching(query: Query): DatabaseClient.GenericExecuteSpec =
        matching(query.sql, query.parameters)

    fun fetch(): FetchSpec<MutableMap<String, Any>> = matching().fetch()
    fun then(): Mono<Void> = matching().then()
}

private class DeleteValueSpecImpl(
    private val client: io.bluetape4k.r2dbc.R2dbcClient,
    private val table: String,
): DeleteValueSpec {

    companion object: KLogging()

    override fun matching(
        where: String?,
        whereParameters: Map<String, Any?>?,
    ): DatabaseClient.GenericExecuteSpec {
        val sql = "DELETE FROM $table"
        val sqlToDelete = when (where) {
            null -> sql
            else -> "$sql WHERE $where"
        }
        log.debug { "Delete SQL=$sqlToDelete" }

        return client.databaseClient
            .sql(sqlToDelete)
            .bindMap(whereParameters ?: emptyMap())
    }
}
