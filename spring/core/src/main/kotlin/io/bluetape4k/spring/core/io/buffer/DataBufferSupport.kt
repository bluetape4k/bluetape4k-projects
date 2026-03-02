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

/**
 * [InputStream]을 [DataBuffer] 스트림으로 읽습니다.
 *
 * ## 동작/계약
 * - [DataBufferUtils.readInputStream]을 사용해 입력 스트림을 읽습니다.
 * - 반환 [Flow]를 수집할 때 실제 읽기가 수행됩니다.
 *
 * ```kotlin
 * val flow = inputStream.readAsDataBuffers(bufferFactory)
 * // flow.toList().isNotEmpty() == true
 * ```
 */
fun InputStream.readAsDataBuffers(
    bufferFactory: DataBufferFactory,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): Flow<DataBuffer> {
    return DataBufferUtils.readInputStream({ this }, bufferFactory, bufferSize).asFlow()
}

/**
 * [ReadableByteChannel]을 [DataBuffer] 스트림으로 읽습니다.
 *
 * ## 동작/계약
 * - [DataBufferUtils.readByteChannel] 호출을 [Flow]로 변환해 반환합니다.
 * - [bufferSize] 크기 단위로 채널을 읽습니다.
 *
 * ```kotlin
 * val flow = channel.readAsDataBuffer(bufferFactory)
 * // flow.toList().isNotEmpty() == true
 * ```
 */
fun ReadableByteChannel.readAsDataBuffer(
    bufferFactory: DataBufferFactory,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): Flow<DataBuffer> {
    return DataBufferUtils.readByteChannel({ this }, bufferFactory, bufferSize).asFlow()
}

/**
 * [AsynchronousFileChannel]을 [DataBuffer] 스트림으로 읽습니다.
 *
 * ## 동작/계약
 * - [position]부터 비동기 파일 채널을 읽습니다.
 * - [DataBufferUtils.readAsynchronousFileChannel] 결과를 [Flow]로 변환합니다.
 *
 * ```kotlin
 * val flow = asyncChannel.readAsDataBuffer(bufferFactory, position = 0)
 * // flow.toList().isNotEmpty() == true
 * ```
 */
fun AsynchronousFileChannel.readAsDataBuffer(
    bufferFactory: DataBufferFactory,
    position: Long = 0L,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): Flow<DataBuffer> {
    return DataBufferUtils.readAsynchronousFileChannel({ this }, position, bufferFactory, bufferSize).asFlow()
}

/**
 * [Path] 파일을 [DataBuffer] 스트림으로 읽습니다.
 *
 * ## 동작/계약
 * - 경로의 파일 내용을 [bufferSize] 단위 [DataBuffer]로 읽습니다.
 * - 반환 [Flow]는 수집 시 읽기 작업을 시작합니다.
 *
 * ```kotlin
 * val flow = path.readAsDataBuffer(bufferFactory)
 * // flow.toList().isNotEmpty() == true
 * ```
 */
fun Path.readAsDataBuffer(
    bufferFactory: DataBufferFactory,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): Flow<DataBuffer> {
    return DataBufferUtils.read(this, bufferFactory, bufferSize).asFlow()
}

/**
 * [Resource]를 [DataBuffer] 스트림으로 읽습니다.
 *
 * ## 동작/계약
 * - [position]부터 리소스 내용을 읽습니다.
 * - [DataBufferUtils.read] 결과를 [Flow]로 변환해 반환합니다.
 *
 * ```kotlin
 * val flow = resource.readAsDataBuffer(bufferFactory)
 * // flow.toList().isNotEmpty() == true
 * ```
 */
fun Resource.readAsDataBuffer(
    bufferFactory: DataBufferFactory,
    position: Long = 0L,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): Flow<DataBuffer> {
    return DataBufferUtils.read(this, position, bufferFactory, bufferSize).asFlow()
}

/**
 * [DataBuffer] 퍼블리셔를 [OutputStream]으로 씁니다.
 *
 * ## 동작/계약
 * - 반환 [Flow]를 수집할 때 쓰기 작업이 시작됩니다.
 * - 대상 [OutputStream]을 자동으로 닫지 않습니다.
 *
 * ```kotlin
 * publisher.write(outputStream).collect()
 * // outputStream.toByteArray() == content
 * ```
 */
@Suppress("UNCHECKED_CAST")
@JvmName("writeDataBufferToOutputStream")
fun Publisher<out DataBuffer>.write(outputStream: OutputStream): Flow<DataBuffer> {
    return DataBufferUtils.write(this as Publisher<DataBuffer>, outputStream).asFlow()
}

/**
 * [DataBuffer] 퍼블리셔를 [WritableByteChannel]로 씁니다.
 *
 * ## 동작/계약
 * - 반환 [Flow]를 수집할 때 쓰기 작업이 시작됩니다.
 * - 대상 채널을 자동으로 닫지 않습니다.
 *
 * ```kotlin
 * val flow = publisher.write(channel)
 * // flow.collect { }
 * ```
 */
@Suppress("UNCHECKED_CAST")
@JvmName("writeDataBufferToWritableByteChannel")
fun Publisher<out DataBuffer>.write(channel: WritableByteChannel): Flow<DataBuffer> {
    return DataBufferUtils.write(this as Publisher<DataBuffer>, channel).asFlow()
}

/**
 * [DataBuffer] 퍼블리셔를 [AsynchronousFileChannel]에 씁니다.
 *
 * ## 동작/계약
 * - [position]부터 파일 채널에 버퍼를 기록합니다.
 * - 반환 [Flow]를 수집할 때 쓰기 작업이 수행됩니다.
 *
 * ```kotlin
 * val flow = publisher.write(asyncChannel, position = 0)
 * // flow.collect { }
 * ```
 */
@JvmName("writeDataBufferToAsynchronousFileChannel")
fun Publisher<out DataBuffer>.write(channel: AsynchronousFileChannel, position: Long = 0): Flow<DataBuffer> {
    return DataBufferUtils.write(this, channel, position).asFlow()
}

/**
 * [DataBuffer] 퍼블리셔를 [Path] 파일에 기록합니다.
 *
 * ## 동작/계약
 * - [DataBufferUtils.write] 완료를 `awaitSingle()`로 대기합니다.
 * - 반환값이 없는 suspend 함수이며 완료 시 파일 쓰기가 끝난 상태입니다.
 *
 * ```kotlin
 * publisher.write(destination)
 * // destination 파일에 데이터가 기록됨
 * ```
 */
@Suppress("UNCHECKED_CAST")
@JvmName("writeDataBufferToPath")
suspend fun Publisher<out DataBuffer>.write(destination: Path, vararg options: OpenOption) {
    DataBufferUtils.write(this as Publisher<DataBuffer>, destination, *options).awaitSingle()
}

/**
 * 총 바이트 수가 [maxByteCount]에 도달할 때까지 버퍼를 전달합니다.
 *
 * ## 동작/계약
 * - 누적 `readableByteCount()`가 [maxByteCount]에 도달하면 이후 데이터 전달을 중단합니다.
 * - 테스트에서 `"abcdefg"` 입력과 `3` 제한으로 `abc`만 반환됩니다.
 *
 * ```kotlin
 * val bytes = publisher.takeUntilByteCount(3)
 * // bytes.toList() == listOf('a', 'b', 'c')
 * ```
 */
fun Publisher<out DataBuffer>.takeUntilByteCount(maxByteCount: Long): Flow<DataBuffer> {
    return DataBufferUtils.takeUntilByteCount(this, maxByteCount).asFlow()
}

/**
 * 총 바이트 수가 [maxByteCount]에 도달할 때까지 버퍼를 건너뜁니다.
 *
 * ## 동작/계약
 * - 누적 `readableByteCount()`가 [maxByteCount]에 도달할 때까지 데이터를 스킵합니다.
 * - 테스트에서 `"abcdefg"` 입력과 `3` 제한으로 `defg`가 남습니다.
 *
 * ```kotlin
 * val result = publisher.skipUntilByteCount(3)
 * // result.toList() 는 "defg" 구간만 포함
 * ```
 */
fun Publisher<out DataBuffer>.skipUntilByteCount(maxByteCount: Long): Flow<DataBuffer> {
    return DataBufferUtils.skipUntilByteCount(this, maxByteCount).asFlow()
}

/**
 * 풀링 버퍼인 경우 참조 카운트를 증가시켜 버퍼를 유지합니다.
 *
 * ## 동작/계약
 * - [DataBufferUtils.retain] 결과를 그대로 반환합니다.
 * - 반환 타입은 수신 객체와 동일한 제네릭 타입 [T]입니다.
 *
 * ```kotlin
 * val retained = dataBuffer.retain()
 * // retained === dataBuffer
 * ```
 */
fun <T: DataBuffer> T.retain(): T {
    return DataBufferUtils.retain(this)
}

/**
 * 풀링 버퍼의 유출 추적용 힌트를 연결합니다.
 *
 * ## 동작/계약
 * - [DataBufferUtils.touch]를 호출해 힌트를 연결합니다.
 * - 반환 타입은 수신 객체와 동일한 제네릭 타입 [T]입니다.
 *
 * ```kotlin
 * val touched = dataBuffer.touch("response-1")
 * // touched === dataBuffer
 * ```
 */
fun <T: DataBuffer> T.touch(hint: Any): T {
    return DataBufferUtils.touch(this, hint)
}

/**
 * 풀링된 [DataBuffer]를 해제합니다.
 *
 * ## 동작/계약
 * - 해제에 성공하면 `true`, 해제할 필요가 없으면 `false`를 반환합니다.
 * - 테스트에서 `DefaultDataBuffer`는 `false`, Netty 풀 버퍼는 `true`를 반환합니다.
 *
 * ```kotlin
 * val released = dataBuffer.release()
 * // released == true || released == false
 * ```
 */
fun DataBuffer.release(): Boolean =
    DataBufferUtils.release(this)

/**
 * [DataBuffer] 퍼블리셔를 하나의 [DataBuffer]로 결합합니다.
 *
 * ## 동작/계약
 * - [maxByteCount]가 `-1`이면 제한 없이 결합합니다.
 * - 테스트에서 `"abc"`와 `"def"` 버퍼를 결합하면 `"abcdef"`가 됩니다.
 *
 * ```kotlin
 * val joined = publisher.join()
 * // joined.readableByteCount() == 6
 * ```
 */
suspend fun Publisher<out DataBuffer>.join(maxByteCount: Int = -1): DataBuffer =
    DataBufferUtils.join(this, maxByteCount).awaitSingle()
