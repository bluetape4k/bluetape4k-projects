package io.bluetape4k.exposed.r2dbc.redisson.map

import kotlinx.coroutines.future.await
import org.redisson.api.AsyncIterator

suspend fun <T> AsyncIterator<T>.toList(destination: MutableList<T> = mutableListOf()): List<T> {
    while (hasNext().await()) {
        destination.add(next().await())
    }
    return destination
}
