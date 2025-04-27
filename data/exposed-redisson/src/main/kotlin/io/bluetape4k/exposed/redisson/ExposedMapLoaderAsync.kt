package io.bluetape4k.exposed.redisson

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
import org.redisson.api.AsyncIterator
import org.redisson.api.map.MapLoaderAsync
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

open class ExposedMapLoaderAsync<K: Any, V: Any>(
    private val loadFromDb: suspend (K) -> V?,
    private val loadAllKeysFromDb: suspend (channel: Channel<K>) -> Collection<K>,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
): MapLoaderAsync<K, V> {

    companion object: KLogging()

    override fun load(key: K): CompletionStage<V?> = coroutineScope.async {
        loadFromDb(key)
    }.asCompletableFuture()

    override fun loadAllKeys(): AsyncIterator<K> {
        val channel = Channel<K>(Channel.RENDEZVOUS).also {
            it.invokeOnClose { cause ->
                log.debug { "channel closed. cause=$cause" }
            }
        }

        val loadingJob = coroutineScope.launch {
            try {
                loadAllKeysFromDb(channel)
            } catch (e: Throwable) {
                log.error(e) { "Error loading all keys" }
            } finally {
                channel.close()
            }
        }

        return object: AsyncIterator<K> {
            private var pendingReceive: CompletableFuture<ChannelResult<K>>? = null
            private fun ensurePending(): CompletableFuture<ChannelResult<K>> {
                return pendingReceive ?: coroutineScope.async {
                    channel.receiveCatching()
                }
                    .asCompletableFuture()
                    .also { pendingReceive = it }
            }

            override fun hasNext(): CompletionStage<Boolean?> = ensurePending().thenApply { result ->
                result.isSuccess
            }

            override fun next(): CompletionStage<K> = ensurePending().thenApply { result ->
                pendingReceive = null
                result.getOrNull() ?: throw NoSuchElementException("No more elements")
            }
        }
    }
}
