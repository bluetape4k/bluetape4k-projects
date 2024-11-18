package io.bluetape4k.spring.core.io.buffer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.reactivestreams.Publisher
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.OpenOption
import java.nio.file.Path

fun InputStream.readAsDataBuffers(
    bufferFactory: DataBufferFactory,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): Flow<DataBuffer> {
    return DataBufferUtils.readInputStream({ this }, bufferFactory, bufferSize).asFlow()
}

fun ReadableByteChannel.readAsDataBuffer(
    bufferFactory: DataBufferFactory,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): Flow<DataBuffer> {
    return DataBufferUtils.readByteChannel({ this }, bufferFactory, bufferSize).asFlow()
}

fun AsynchronousFileChannel.readAsDataBuffer(
    bufferFactory: DataBufferFactory,
    position: Long = 0L,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): Flow<DataBuffer> {
    return DataBufferUtils.readAsynchronousFileChannel({ this }, position, bufferFactory, bufferSize).asFlow()
}

fun Path.readAsDataBuffer(
    bufferFactory: DataBufferFactory,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): Flow<DataBuffer> {
    return DataBufferUtils.read(this, bufferFactory, bufferSize).asFlow()
}

fun Resource.readAsDataBuffer(
    bufferFactory: DataBufferFactory,
    position: Long = 0L,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): Flow<DataBuffer> {
    return DataBufferUtils.read(this, position, bufferFactory, bufferSize).asFlow()
}

/**
 * Write the given stream of [DataBuffer] to the given
 * [OutputStream]. Does **not** close the output stream when the flux is terminated,
 * and does **not** `release(DataBuffer) release` the data buffers in the source.
 *
 * If releasing is required, then subscribe to the returned [Flow] with a `collect`
 *
 * Note that the writing process does not start until the returned [Flow]
 *
 * @receiver  the stream of data buffers to be written
 * @param outputStream the output stream to write to
 * @return a Flux containing the same buffers as in `source`, that
 * starts the writing process when subscribed to, and that publishes any
 * writing errors and the completions signal
 */
fun Publisher<DataBuffer>.write(outputStream: OutputStream): Flow<DataBuffer> {
    return DataBufferUtils.write(this, outputStream).asFlow()
}

fun Publisher<DataBuffer>.write(channel: WritableByteChannel): Flow<DataBuffer> {
    return DataBufferUtils.write(this, channel).asFlow()
}

fun Publisher<DataBuffer>.write(channel: AsynchronousFileChannel, position: Long = 0): Flow<DataBuffer> {
    return DataBufferUtils.write(this, channel, position).asFlow()
}

suspend fun Publisher<DataBuffer>.write(destination: Path, vararg options: OpenOption) {
    DataBufferUtils.write(this, destination, *options).awaitSingle()
}

/**
 * 주어진 [Publisher]의 버퍼를 총합이 `DataBuffer.readableByteCount()` 바이트 수가 주어진 최대 바이트 수에 도달하거나
 * 도달할 때까지 또는 퍼블리셔가 완료될 때까지 버퍼를 릴레이합니다.
 *
 * @receiver the publisher to filters
 * @param maxByteCount the maximum byte count
 * @return a flow whose maximum byte count is [maxByteCount]
 */
fun Publisher<out DataBuffer>.takeUntilByteCount(maxByteCount: Long): Flow<DataBuffer> {
    return DataBufferUtils.takeUntilByteCount(this, maxByteCount).asFlow()
}

/**
 * 주어진 [Publisher]의 버퍼를 총합이 `DataBuffer.readableByteCount()` 바이트 수가 주어진 최대 바이트 수에 도달하거나
 * 도달할 때까지 또는 퍼블리셔가 완료될 때까지 버퍼를 스킵합니다.
 *
 * @receiver the publisher to filters
 * @param maxByteCount the maximum byte count
 * @return a flux with the remaining part of the given publisher
 */
fun Publisher<out DataBuffer>.skipUntilByteCount(maxByteCount: Long): Flow<DataBuffer> {
    return DataBufferUtils.skipUntilByteCount(this, maxByteCount).asFlow()
}

/**
 * 지정된 데이터 버퍼가 [org.springframework.core.io.buffer.PooledDataBuffer] 인 경우 해당 데이터 버퍼를 유지합니다.
 *
 * @param T
 * @return
 */
fun <T: DataBuffer> T.retain(): T {
    return DataBufferUtils.retain(this)
}

/**
 * 풀링된 버퍼이고 유출 추적을 지원하는 경우 주어진 [hint]를 데이터 버퍼와 연결합니다.
 *
 * @param hint 데이터 버퍼와 연결할 힌트
 */
fun <T: DataBuffer> T.touch(hint: Any): T {
    return DataBufferUtils.touch(this, hint)
}

/**
 * 주어진 Data buffer 가 [org.springframework.core.io.buffer.PooledDataBuffer] 이고,
 * [org.springframework.core.io.buffer.PooledDataBuffer.isAllocated] 로 할당이 되었다면, 해제합니다.
 *
 * @receiver the data buffer to release
 * @return `true` if the buffer was released; ``false` otherwise.
 */
fun DataBuffer.release(): Boolean {
    return DataBufferUtils.release(this)
}

suspend fun Publisher<out DataBuffer>.join(maxByteCount: Int = -1): DataBuffer {
    return DataBufferUtils.join(this, maxByteCount).awaitSingle()
}
