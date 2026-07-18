package io.bluetape4k.io.serializer

import io.bluetape4k.io.BufferFailurePolicy
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import java.util.concurrent.CancellationException

internal inline fun serializeTo(
    target: ByteBuffer,
    produce: () -> ByteArray,
): Int = serializeNullableTo(target, produce)

internal inline fun serializeNullableTo(
    target: ByteBuffer,
    produce: () -> ByteArray?,
): Int {
    if (target.isReadOnly) throw ReadOnlyBufferException()
    val start = target.position()
    return try {
        val bytes = produce()
        if (bytes == null) {
            0
        } else {
            if (bytes.size > target.remaining()) throw BufferOverflowException()
            target.put(bytes)
            bytes.size
        }
    } catch (failure: Throwable) {
        target.position(start)
        throw failure
    }
}

internal fun copyRemaining(source: ByteBuffer): ByteArray =
    ByteArray(source.remaining()).also { source.duplicate().get(it) }

internal fun throwBufferSerializationFailure(
    graph: Any,
    failure: Throwable,
): Nothing = BufferSerializationFailureSupport.throwSerialization(graph, failure)

internal fun throwBufferDeserializationFailure(
    sourceSize: Int,
    failure: Throwable,
): Nothing = BufferSerializationFailureSupport.throwDeserialization(sourceSize, failure)

internal inline fun <R, T> useWithCleanup(
    resource: R,
    cleanup: (R) -> Unit,
    block: (R) -> T,
): T {
    var operationFailure: Throwable? = null
    try {
        return block(resource)
    } catch (failure: Throwable) {
        operationFailure = failure
        throw failure
    } finally {
        var cleanupFailure: Throwable? = null
        try {
            cleanup(resource)
        } catch (failure: Throwable) {
            cleanupFailure = failure
        }
        if (cleanupFailure != null) {
            throw checkNotNull(BufferFailurePolicy.classify(operationFailure, cleanupFailure))
        }
    }
}

private object BufferSerializationFailureSupport: KLogging() {

    fun throwSerialization(
        graph: Any,
        failure: Throwable,
    ): Nothing {
        val classified = BufferFailurePolicy.classify(failure, null) ?: failure
        if (classified is Error || classified is CancellationException || classified is BufferOverflowException) {
            throw classified
        }

        val graphType = graph.javaClass.name
        log.error(classified) { "Fail to serialize to ByteBuffer. graphType=$graphType" }
        throw BinarySerializationException("Fail to serialize. graphType=$graphType", classified)
    }

    fun throwDeserialization(
        sourceSize: Int,
        failure: Throwable,
    ): Nothing {
        val classified = BufferFailurePolicy.classify(failure, null) ?: failure
        if (classified is Error || classified is CancellationException) throw classified

        log.error(classified) { "Fail to deserialize from ByteBuffer." }
        throw BinarySerializationException("Fail to deserialize. bytesSize=$sourceSize", classified)
    }
}
