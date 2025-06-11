package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.redisson.api.AsyncIterator
import org.redisson.api.map.MapLoaderAsync
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

/**
 * SuspendedExposedMapLoader는 Exposed를 사용하여 DB에서 데이터를 비동기적으로 로드하는 [MapLoaderAsync]입니다.
 *
 * @param ID ID 타입
 * @param E 엔티티 타입
 * @param loadByIdFromDB ID로 엔티티를 로드하는 함수
 * @param loadAllIdsFromDB 모든 ID를 로드하는 함수
 * @param scope CoroutineScope
 */
@Suppress("DEPRECATION")
open class SuspendedEntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
    private val loadByIdFromDB: suspend (ID) -> E?,
    private val loadAllIdsFromDB: suspend (channel: Channel<ID>) -> Unit,
    private val scope: CoroutineScope = defaultMapLoaderCoroutineScope,
): MapLoaderAsync<ID, E> {

    companion object: KLoggingChannel() {
        private const val DEFAULT_QUERY_TIMEOUT = 30_000  // 30 seconds
        private const val DEFAULT_LOAD_ALL_IDS_TIMEOUT = 60_000L  // 60 seconds

        protected val defaultMapLoaderCoroutineScope = CoroutineScope(Dispatchers.IO) + CoroutineName("DB-Loader")
    }

    override fun load(id: ID): CompletionStage<E?> = scope.async {
        log.debug { "DB에서 엔티티를 로딩... id=$id" }
        newSuspendedTransaction(scope.coroutineContext) {
            try {
                loadByIdFromDB(id)
                    .apply {
                        log.debug { "DB로부터 엔티티를 로딩했습니다. id=$id, entity=$this" }
                    }
            } catch (e: Throwable) {
                log.error(e) { "DB에서 엔티티 로딩 중 오류 발생. id=$id" }
                throw e
            }
        }
    }.asCompletableFuture()

    override fun loadAllKeys(): AsyncIterator<ID> {
        val channel = Channel<ID>(Channel.RENDEZVOUS).also {
            it.invokeOnClose { cause ->
                log.debug { "Channel closed. cause=$cause" }
            }
        }

        scope.launch {
            log.debug { "DB에서 모든 ID를 로딩합니다 ..." }
            try {
                newSuspendedTransaction(scope.coroutineContext) {
                    this.queryTimeout = DEFAULT_QUERY_TIMEOUT  // 30 seconds
                    withTimeoutOrNull(DEFAULT_LOAD_ALL_IDS_TIMEOUT) {
                        loadAllIdsFromDB(channel)
                    } ?: log.warn { "DB에서 모든 ID를 읽는 작업 중 Timeout 이 발생했습니다. timeout=$DEFAULT_LOAD_ALL_IDS_TIMEOUT msec" }
                }
            } catch (e: Throwable) {
                log.error(e) { "DB에서 모든 ID 로딩 중 오류 발생" }
                throw e
            } finally {
                channel.close()
            }
        }

        return object: AsyncIterator<ID> {
            private var pendingReceive: CompletableFuture<ChannelResult<ID>>? = null

            private fun ensurePending(): CompletableFuture<ChannelResult<ID>> {
                return pendingReceive ?: scope.async {
                    channel.receiveCatching()
                }
                    .asCompletableFuture()
                    .also { pendingReceive = it }
            }

            override fun hasNext(): CompletionStage<Boolean?> = ensurePending().thenApply { result ->
                result.isSuccess
            }

            override fun next(): CompletionStage<ID> = ensurePending().thenApply { result ->
                pendingReceive = null
                result.getOrNull() ?: throw NoSuchElementException("No more elements")
            }
        }
    }
}
