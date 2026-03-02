package io.bluetape4k.exposed.r2dbc.redisson.map

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.redisson.api.map.MapWriterAsync
import java.util.concurrent.CompletionStage


/**
 * R2DBC 트랜잭션 안에서 DB 쓰기/삭제 함수를 실행하는 Redisson 비동기 [MapWriterAsync] 구현입니다.
 *
 * ## 동작/계약
 * - [write]는 전달된 map을 하나의 `suspendTransaction`에서 [writeToDb]에 위임합니다.
 * - [delete]는 전달된 키 컬렉션을 하나의 `suspendTransaction`에서 [deleteFromDb]에 위임합니다.
 * - 예외는 로깅 후 그대로 전파됩니다.
 *
 * ```kotlin
 * val writer = R2dbcEntityMapWriter<Long, UserRecord>(
 *     writeToDb = { batch -> repo.saveAll(batch.values) },
 *     deleteFromDb = { ids -> repo.deleteAllByIds(ids) }
 * )
 * // writer.write(mapOf(1L to entity))
 * ```
 *
 * @param writeToDb DB에 데이터를 쓰는 함수입니다.
 * @param deleteFromDb DB에서 데이터를 삭제하는 함수입니다.
 */
open class R2dbcEntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
    private val writeToDb: suspend (map: Map<ID, E>) -> Unit,
    private val deleteFromDb: suspend (keys: Collection<ID>) -> Unit,
    private val scope: CoroutineScope = defaultMapWriterCoroutineScope,
): MapWriterAsync<ID, E> {

    companion object: KLoggingChannel() {
        protected val defaultMapWriterCoroutineScope =
            CoroutineScope(Dispatchers.IO + CoroutineName("R2dbc-Writer"))
    }

    override fun write(map: Map<ID, E>): CompletionStage<Void> = scope.async {
        suspendTransaction {
            try {
                writeToDb(map)
            } catch (e: Throwable) {
                log.error(e) { "R2dbc로 DB에 엔티티 Write 중 오류 발생" }
                throw e
            }
        }
        null
    }.asCompletableFuture()

    override fun delete(ids: Collection<ID>): CompletionStage<Void> = scope.async {
        suspendTransaction {
            try {
                log.debug { "캐시 변경 사항을 DB에 반영합니다... ids=$ids" }
                deleteFromDb(ids)
            } catch (e: Throwable) {
                log.error(e) { "R2dbc로 엔티티 삭제 중 오류 발생" }
                throw e
            }
        }
        null
    }.asCompletableFuture()
}
