package io.bluetape4k.io.compressor

import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

private inline fun writeToCallerBufferState(
    source: ByteBuffer,
    target: ByteBuffer,
    operation: (
        sourcePosition: Int,
        sourceRemaining: Int,
        targetPosition: Int,
        targetRemaining: Int,
    ) -> Int,
): Int {
    if (target.isReadOnly) throw ReadOnlyBufferException()
    rejectDetectableOverlap(source, target)
    if (!source.hasRemaining()) return 0

    val sourcePosition = source.position()
    val sourceLimit = source.limit()
    val targetPosition = target.position()
    val targetLimit = target.limit()
    val sourceOrder = source.order()
    val targetOrder = target.order()

    try {
        val written = operation(
            sourcePosition,
            source.remaining(),
            targetPosition,
            target.remaining(),
        )
        check(written in 0..(targetLimit - targetPosition)) {
            "Compressor buffer operation returned invalid written=$written, " +
                "targetRemaining=${targetLimit - targetPosition}"
        }
        check(
            source.position() == sourcePosition &&
                source.limit() == sourceLimit &&
                source.order() == sourceOrder
        ) {
            "Compressor buffer operation modified caller source state"
        }
        check(target.limit() == targetLimit && target.order() == targetOrder) {
            "Compressor buffer operation modified caller target limit or byte order"
        }
        target.position(targetPosition + written)
        return written
    } catch (failure: Throwable) {
        try {
            target.position(targetPosition)
        } catch (rollbackFailure: Throwable) {
            if (rollbackFailure !== failure) {
                failure.addSuppressed(rollbackFailure)
            }
        }
        throw failure
    }
}

internal inline fun writeToCallerBufferViews(
    source: ByteBuffer,
    target: ByteBuffer,
    operation: (
        sourceView: ByteBuffer,
        targetView: ByteBuffer,
        sourcePosition: Int,
        sourceRemaining: Int,
        targetPosition: Int,
        targetRemaining: Int,
    ) -> Int,
): Int = writeToCallerBufferState(source, target) { sourcePosition, sourceRemaining, targetPosition, targetRemaining ->
    operation(
        source.duplicate().order(source.order()),
        target.duplicate().order(target.order()),
        sourcePosition,
        sourceRemaining,
        targetPosition,
        targetRemaining,
    )
}

internal inline fun writeFallback(
    source: ByteBuffer,
    target: ByteBuffer,
    transform: (ByteArray) -> ByteArray,
): Int = writeToCallerBufferViews(source, target) {
        sourceView,
        targetView,
        sourcePosition,
        sourceRemaining,
        targetPosition,
        targetRemaining,
    ->
    val input = ByteArray(sourceRemaining).also { bytes ->
        sourceView
            .position(sourcePosition)
            .limit(sourcePosition + sourceRemaining)
            .get(bytes)
    }
    val output = transform(input)
    if (output.size > targetRemaining) throw BufferOverflowException()
    targetView
        .position(targetPosition)
        .limit(targetPosition + targetRemaining)
        .put(output)
    output.size
}

private fun rejectDetectableOverlap(source: ByteBuffer, target: ByteBuffer) {
    if (source === target) {
        throw IllegalArgumentException("source and target must not be the same buffer")
    }
    if (!source.hasArray() || !target.hasArray() || source.array() !== target.array()) return

    val sourceStart = source.arrayOffset() + source.position()
    val sourceEnd = source.arrayOffset() + source.limit()
    val targetStart = target.arrayOffset() + target.position()
    val targetEnd = target.arrayOffset() + target.limit()
    if (sourceStart < targetEnd && targetStart < sourceEnd) {
        throw IllegalArgumentException("source and target array ranges must not overlap")
    }
}

internal fun putIntBigEndian(target: ByteBuffer, index: Int, value: Int) {
    target.put(index, (value ushr 24).toByte())
    target.put(index + 1, (value ushr 16).toByte())
    target.put(index + 2, (value ushr 8).toByte())
    target.put(index + 3, value.toByte())
}

internal fun getIntBigEndian(source: ByteBuffer, index: Int): Int =
    ((source.get(index).toInt() and 0xFF) shl 24) or
        ((source.get(index + 1).toInt() and 0xFF) shl 16) or
        ((source.get(index + 2).toInt() and 0xFF) shl 8) or
        (source.get(index + 3).toInt() and 0xFF)
