package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.plus
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import org.redisson.api.map.MapWriterAsync
import java.util.concurrent.CompletionStage

/**
 * Redisson의 Write-through [MapWriterAsync] 를 Exposed와 코루틴을 사용하여 구현한 최상위 클래스입니다.
 *
 * ## 동작/계약
 * - [write]는 `suspendedTransactionAsync`로 [writeToDb]를 실행해 캐시 변경을 DB에 비동기 반영합니다.
 * - [delete]는 `suspendedTransactionAsync`로 [deleteFromDb]를 실행해 캐시 삭제를 DB에 비동기 반영합니다.
 * - 각 메서드는 [CompletionStage]<Void>를 반환하므로 Redisson의 MapWriterAsync 계약을 준수합니다.
 * - DB 오류는 로깅 후 예외를 그대로 전파합니다.
 *
 * ```kotlin
 * val writer = SuspendedEntityMapWriter<Long, UserRecord>(
 *     writeToDb = { map -> repo.saveAll(map.values) },
 *     deleteFromDb = { ids -> repo.deleteAllByIds(ids) },
 * )
 * ```
 *
 * @param writeToDb DB에 데이터를 쓰는 suspend 함수입니다.
 * @param deleteFromDb DB에서 데이터를 삭제하는 suspend 함수입니다.
 * @param scope DB 쓰기 작업에 사용할 [CoroutineScope]. 기본값은 `Dispatchers.IO` 기반 스코프입니다.
 */
@Suppress("DEPRECATION")
open class SuspendedEntityMapWriter<ID : Comparable<ID>, E : Any>(
    private val writeToDb: suspend (map: Map<ID, E>) -> Unit,
    private val deleteFromDb: suspend (keys: Collection<ID>) -> Unit,
    private val scope: CoroutineScope = defaultMapWriterCoroutineScope,
) : MapWriterAsync<ID, E> {
    companion object : KLoggingChannel() {
        protected val defaultMapWriterCoroutineScope = CoroutineScope(Dispatchers.IO) + CoroutineName("DB-Writer")
    }

    /**
     * 캐시 변경 사항을 코루틴 트랜잭션으로 DB에 비동기 반영합니다.
     *
     * @param map 캐시에 쓰여진 ID → 엔티티 맵 전체
     * @return DB 반영 완료를 알리는 [CompletionStage]
     */
    override fun write(map: Map<ID, E>): CompletionStage<Void> =
        scope
            .async {
                suspendedTransactionAsync(context = scope.coroutineContext) {
                    try {
                        writeToDb(map)
                    } catch (e: Throwable) {
                        log.error(e) { "DB에 Write 중 오류 발생" }
                        throw e
                    }
                }.await()
                null
            }.asCompletableFuture()

    /**
     * 캐시에서 제거된 키 목록을 코루틴 트랜잭션으로 DB에 비동기 반영합니다.
     *
     * @param ids 캐시에서 제거된 ID 컬렉션
     * @return DB 반영 완료를 알리는 [CompletionStage]
     */
    override fun delete(ids: Collection<ID>): CompletionStage<Void> =
        scope
            .async {
                suspendedTransactionAsync(context = scope.coroutineContext) {
                    try {
                        log.debug { "캐시 변경 사항을 DB에 반영합니다... ids=$ids" }
                        deleteFromDb(ids)
                    } catch (e: Throwable) {
                        log.error(e) { "DB에서 삭제 중 오류 발생" }
                        throw e
                    }
                }.await()
                null
            }.asCompletableFuture()
}
