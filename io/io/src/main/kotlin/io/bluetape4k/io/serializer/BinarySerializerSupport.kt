package io.bluetape4k.io.serializer

import io.bluetape4k.io.getBytes
import java.nio.ByteBuffer

/**
 * 객체를 Binary 방식으로 직렬화를 하여 [ByteBuffer]로 반환합니다.
 *
 * ```
 * val serializer = JdkBinarySerializer()
 * val buffer = serializer.serializeAsByteBuffer("Hello, World!")
 * ```
 *
 * @param graph 직렬화할 객체
 * @return 직렬화된 정보를 담은 [ByteBuffer] 인스턴스
 */
fun BinarySerializer.serializeAsByteBuffer(graph: Any?): ByteBuffer =
    ByteBuffer.wrap(serialize(graph))

/**
 * 직렬화된 [buffer]를 읽어 대상 객체로 역직렬화합니다.
 *
 * ```
 * val serializer = JdkBinarySerializer()
 * val buffer = serializer.serializeAsByteBuffer("Hello, World!")
 * val text = serializer.deserialize<String>(buffer)  // text="Hello, World!"
 * ```
 *
 * @param T     역직렬화할 객체 수형
 * @param buffer 직렬화된 데이터
 * @return 역직렬화한 객체
 */
fun <T: Any> BinarySerializer.deserialize(buffer: ByteBuffer): T? =
    deserialize(buffer.getBytes())


/**
 * 객체를 Binary 방식으로 직렬화를 하여 [okio.Buffer]로 반환합니다.
 *
 * ```
 * val serializer = JdkBinarySerializer()
 * val buffer = serializer.serializeAsOkioBuffer("Hello, World!")
 * ```
 *
 * @param graph 직렬화할 객체
 * @return 직렬화된 정보를 담은 [okio.Buffer] 인스턴스
 */
fun BinarySerializer.serializeAsOkioBuffer(graph: Any?): okio.Buffer =
    okio.Buffer().write(serialize(graph))

/**
 * 직렬화된 [buffer]를 읽어 대상 객체로 역직렬화합니다.
 *
 * ```
 * val serializer = JdkBinarySerializer()
 * val buffer = serializer.serializeAsOkioBuffer("Hello, World!")
 * val text = serializer.deserialize<String>(buffer)  // text="Hello, World!"
 * ```
 *
 * @param T     역직렬화할 객체 수형
 * @param buffer 직렬화된 데이터 [okio.Buffer]
 * @return 역직렬화한 객체
 */
fun <T: Any> BinarySerializer.deserialize(buffer: okio.Buffer): T? =
    deserialize(buffer.readByteArray())
