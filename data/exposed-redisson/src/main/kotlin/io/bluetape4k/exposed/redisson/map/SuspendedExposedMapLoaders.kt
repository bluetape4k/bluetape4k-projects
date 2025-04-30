package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.exposed.sql.fetchBatchedResultFlow
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.redisson.api.AsyncIterator
import org.redisson.api.map.MapLoaderAsync
import java.sql.SQLException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

private val defaultMapLoaderCoroutineScope = CoroutineScope(Dispatchers.IO) + CoroutineName("DB-Loader")

open class SuspendedExposedMapLoader<ID: Any, E: Any>(
    private val loadByIdFromDB: suspend (ID) -> E?,
    private val loadAllIdsFromDB: suspend (channel: Channel<ID>) -> Unit,
    private val scope: CoroutineScope = defaultMapLoaderCoroutineScope,
): MapLoaderAsync<ID, E> {

    companion object: KLogging() {
        private const val DEFAULT_QUERY_TIMEOUT = 30_000  // 30 seconds
        private const val DEFAULT_LOAD_ALL_IDS_TIMEOUT = 60_000L  // 60 seconds
    }

    override fun load(id: ID): CompletionStage<E?> = scope.async {
        log.debug { "DB에서 엔티티를 로딩... id=$id" }

        newSuspendedTransaction(scope.coroutineContext) {
            try {
                loadByIdFromDB(id).apply {
                    log.debug { "DB로부터 엔티티를 로딩했습니다. entity=$this" }
                }
            } catch (e: Throwable) {
                throw SQLException("엔티티 로딩 실패. id=$id", e)
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

/**
 * HasIdentifier<ID> 를 위한 [SuspendedExposedMapLoader] 기본 구현체입니다.
 *
 * @param entityTable Entity<ID> 를 위한 [IdTable] 입니다.
 * @param toEntity ResultRow 를 E 타입으로 변환하는 함수입니다.
 */
open class SuspendedExposedEntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
    private val entityTable: IdTable<ID>,
    scope: CoroutineScope = defaultMapLoaderCoroutineScope,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val toEntity: ResultRow.() -> E,
): SuspendedExposedMapLoader<ID, E>(
    loadByIdFromDB = { id: ID ->
        entityTable.selectAll()
            .where { entityTable.id eq id }
            .singleOrNull()
            ?.toEntity()

    },
    loadAllIdsFromDB = { channel ->
        var rowCount = 0

        entityTable.select(entityTable.id)
            .fetchBatchedResultFlow()
            .buffer(3)
            .flowOn(scope.coroutineContext)
            .cancellable()
            .catch { cause ->
                log.error(cause) { "DB에서 모든 ID 로딩 중 오류 발생" }
                throw cause
            }
            .onEach { rows ->
                rowCount += rows.size
                log.trace { "DB에서 모든 ID 로딩 중... 로딩된 id 수=$rowCount" }
            }
            .onCompletion {
                log.debug { "DB에서 모든 ID 로딩 완료. 로딩된 id 수=$rowCount" }
            }
            .collect { rows ->
                rows.forEach { row ->
                    val id = row[entityTable.id].value
                    channel.trySend(id)
                        .onFailure { cause ->
                            throw IllegalStateException("채널 전송 실패. id=$id", cause)
                        }
                }
            }
    },
    scope = scope,
) {
    companion object: KLogging() {
        private const val DEFAULT_BATCH_SIZE = 1000
    }
}
