package io.bluetape4k.exposed.redisson

import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.redisson.api.map.MapWriterAsync
import java.util.concurrent.CompletionStage

@Deprecated("Use DefaultSuspendedExposedMapWriter instead")
open class ExposedMapWriterAsync<K: Any, V: Any>(
    private val writeToDb: suspend (map: Map<K, V?>) -> Unit,
    private val deleteFromDb: suspend (keys: Collection<K>) -> Unit,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
): MapWriterAsync<K, V> {

    companion object: KLogging()

    override fun write(map: Map<K, V?>): CompletionStage<Void> {
        return scope.async {
            writeToDb(map)
            null
        }.asCompletableFuture()
    }

    override fun delete(keys: Collection<K>): CompletionStage<Void> {
        return scope.async {
            deleteFromDb(keys)
            null
        }.asCompletableFuture()
    }
}
