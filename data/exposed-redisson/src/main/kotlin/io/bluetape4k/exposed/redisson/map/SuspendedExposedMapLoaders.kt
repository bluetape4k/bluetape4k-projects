package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.redisson.api.AsyncIterator
import org.redisson.api.map.MapLoaderAsync
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

open class SuspendedExposedMapLoader<ID: Any, E: Any>(
    private val loadByIdFromDB: suspend (ID) -> E?,
    private val loadAllIdsFromDB: suspend (channel: Channel<ID>) -> Unit,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
): MapLoaderAsync<ID, E> {

    companion object: KLogging()

    override fun load(key: ID): CompletionStage<E?> = scope.async {
        log.debug { "Load from DB... key=$key" }
        newSuspendedTransaction(scope.coroutineContext) {
            loadByIdFromDB(key)
                .apply {
                    log.debug { "Loaded from DB... key=$key, E=$this" }
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
            log.debug { "Load all keys from DB..." }
            try {
                newSuspendedTransaction(scope.coroutineContext) {
                    loadAllIdsFromDB(channel)
                }
            } catch (e: Throwable) {
                log.error(e) { "Error loading all keys" }
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
 * @param table Entity<ID> 를 위한 [IdTable] 입니다.
 * @param toEntity ResultRow 를 E 타입으로 변환하는 함수입니다.
 */
open class SuspendedExposedEntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val table: IdTable<ID>,
    private val toEntity: ResultRow.() -> E,
): SuspendedExposedMapLoader<ID, E>(
    loadByIdFromDB = { id: ID ->
        table.selectAll()
            .where { table.id eq id }
            .singleOrNull()
            ?.toEntity()

    },
    loadAllIdsFromDB = { channel ->
        table.selectAll()
            .forEach {
                channel.send(it[table.id].value)
            }
    },
    scope = scope,
) {
    companion object: KLogging()
}
