package io.bluetape4k.exposed.r2dbc.redisson.map

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import kotlinx.coroutines.CancellationException
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
open class R2dbcEntityMapWriter<ID: Any, E: Any>(
    private val writeToDb: suspend (map: Map<ID, E>) -> Unit,
    private val deleteFromDb: suspend (keys: Collection<ID>) -> Unit,
    private val scope: CoroutineScope = defaultMapWriterCoroutineScope,
): MapWriterAsync<ID, E> {
    companion object: KLoggingChannel() {
        protected val defaultMapWriterCoroutineScope =
            CoroutineScope(Dispatchers.IO + CoroutineName("R2dbc-Writer"))
    }

    override fun write(map: Map<ID, E>): CompletionStage<Void> =
        scope
            .async {
                // WHY: write-behind 방식에서 배치 단위로 호출되므로, 단일 트랜잭션 안에서
                //      map 전체를 처리해 부분 커밋(데이터 불일치)을 방지한다.
                suspendTransaction {
                    try {
                        writeToDb(map)
                    } catch (e: CancellationException) {
                        // 코루틴 취소는 반드시 재전파해야 한다 — 삼키면 구조적 동시성이 깨진다
                        throw e
                    } catch (e: Throwable) {
                        log.error(e) { "R2dbc로 DB에 엔티티 Write 중 오류 발생" }
                        throw e
                    }
                }
                null
            }.asCompletableFuture()

    override fun delete(ids: Collection<ID>): CompletionStage<Void> =
        scope
            .async {
                // WHY: 여러 키 삭제를 단일 트랜잭션으로 묶어 원자성을 보장한다.
                //      개별 삭제 루프를 쓰면 중간 실패 시 일부만 삭제되는 불일치가 발생한다.
                suspendTransaction {
                    try {
                        log.debug { "캐시 변경 사항을 DB에 반영합니다... ids=$ids" }
                        deleteFromDb(ids)
                    } catch (e: CancellationException) {
                        // 코루틴 취소는 반드시 재전파해야 한다 — 삼키면 구조적 동시성이 깨진다
                        throw e
                    } catch (e: Throwable) {
                        log.error(e) { "R2dbc로 엔티티 삭제 중 오류 발생" }
                        throw e
                    }
                }
                null
            }.asCompletableFuture()
}
