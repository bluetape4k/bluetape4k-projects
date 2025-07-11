package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.core.HasIdentifier
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
 * Redisson의 Write-through [MapWriterAsync] 를 Exposed와 코루틴를 사용하여 구현한 최상위 클래스입니다.
 *
 * @param writeToDb DB에 데이터를 쓰는 함수입니다.
 * @param deleteFromDb DB에서 데이터를 삭제하는 함수입니다.
 */
@Suppress("DEPRECATION")
open class SuspendedEntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
    private val writeToDb: suspend (map: Map<ID, E>) -> Unit,
    private val deleteFromDb: suspend (keys: Collection<ID>) -> Unit,
    private val scope: CoroutineScope = defaultMapWriterCoroutineScope,
): MapWriterAsync<ID, E> {

    companion object: KLoggingChannel() {
        private const val DEFAULT_QUERY_TIMEOUT = 30_000  // 30 seconds

        protected val defaultMapWriterCoroutineScope = CoroutineScope(Dispatchers.IO) + CoroutineName("DB-Writer")
    }

    override fun write(map: Map<ID, E>): CompletionStage<Void> = scope.async {
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

    override fun delete(ids: Collection<ID>): CompletionStage<Void> = scope.async {
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
