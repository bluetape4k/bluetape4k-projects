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
 * R2DBC DELETE 연산을 시작합니다.
 *
 * @return [DeleteTableSpec] 인스턴스
 */
fun R2dbcClient.delete(): DeleteTableSpec = DeleteTableSpecImpl(this)

/**
 * DELETE 연산의 대상 테이블을 지정하는 인터페이스
 */
interface DeleteTableSpec {
    /**
     * 삭제할 테이블 이름을 지정합니다.
     *
     * @param table 테이블 이름
     * @return [DeleteValueSpec] 인스턴스
     */
    fun from(table: String): DeleteValueSpec

    /**
     * 엔티티 타입으로 삭제할 테이블을 지정합니다.
     *
     * @param T 엔티티 타입
     * @param type 엔티티 클래스
     * @return [ReactiveDeleteOperation.ReactiveDelete] 인스턴스
     */
    fun <T> from(type: Class<T>): ReactiveDeleteOperation.ReactiveDelete
}

/**
 * 제네릭 타입으로 DELETE 대상을 지정합니다.
 */
inline fun <reified T: Any> DeleteTableSpec.from(): ReactiveDeleteOperation.ReactiveDelete = from(T::class.java)

private class DeleteTableSpecImpl(
    private val client: io.bluetape4k.r2dbc.R2dbcClient,
): DeleteTableSpec {
    override fun from(table: String): DeleteValueSpec = DeleteValueSpecImpl(client, table)

    override fun <T> from(type: Class<T>): ReactiveDeleteOperation.ReactiveDelete = client.entityTemplate.delete(type)
}

/**
 * DELETE 조건 설정 및 실행 인터페이스
 */
interface DeleteValueSpec {
    /**
     * WHERE 절과 파라미터로 삭제 조건을 지정합니다.
     *
     * ```
     * val client: R2dbcClient = ...
     *
     * client.delete()
     *      .from("Posts")
     *      .matching("id = :id", mapOf("id", 1L))
     *      .fetch()
     *      .awaitSingleOrNull()
     * ```
     *
     * @param where WHERE 절 (파라미터 바인딩 포함)
     * @param whereParameters 바인딩할 파라미터 맵
     * @return [DatabaseClient.GenericExecuteSpec] 인스턴스
     */
    fun matching(
        where: String? = null,
        whereParameters: Map<String, Any?>? = null,
    ): DatabaseClient.GenericExecuteSpec

    /**
     * [Query] 객체로 삭제 조건을 지정합니다.
     *
     * @param query 삭제 조건에 해당하는 [Query]
     * @return [DatabaseClient.GenericExecuteSpec] 인스턴스
     */
    fun matching(query: Query): DatabaseClient.GenericExecuteSpec = matching(query.sql, query.parameters)

    /**
     * DELETE 결과를 조회합니다.
     */
    fun fetch(): FetchSpec<MutableMap<String, Any>> = matching().fetch()

    /**
     * DELETE 실행 후 완료 신호를 반환합니다.
     */
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
        val sqlToDelete =
            when (where) {
                null -> sql
                else -> "$sql WHERE $where"
            }
        log.debug { "Delete SQL=$sqlToDelete" }

        return client.databaseClient
            .sql(sqlToDelete)
            .bindMap(whereParameters ?: emptyMap())
    }
}
