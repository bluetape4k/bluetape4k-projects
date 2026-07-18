package io.bluetape4k.io.serializer

import io.bluetape4k.io.ByteBufferInputStream
import io.bluetape4k.io.ByteBufferOutputStream
import io.bluetape4k.logging.KLogging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputFilter
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

/**
 * 기본 [ObjectInputFilter]. `io.bluetape4k.**`, `java.lang.**`, `java.util.**`,
 * `java.io.*`, `java.math.**`, `java.time.**`, `kotlin.**` 패키지만 허용하고
 * 그 외 모든 클래스의 역직렬화를 차단합니다 (JEP 290 참고).
 *
 * 참고: java.base 모듈 패턴은 일부 JVM 구성에서 제대로 동작하지 않으므로
 * 명시적 패키지 패턴을 사용합니다.
 *
 * 추가 클래스가 필요한 경우 [JdkBinarySerializer] 생성 시 별도 필터를 지정하세요.
 */
val JDK_DEFAULT_OBJECT_INPUT_FILTER: ObjectInputFilter = ObjectInputFilter.Config.createFilter(
    "io.bluetape4k.**;java.lang.*;java.lang.**;java.util.*;java.util.**;" +
        "java.io.*;java.math.*;java.math.**;java.time.*;java.time.**;" +
        "java.net.*;java.sql.*;kotlin.*;kotlin.**;!*"
)

/**
 * JDK의 [ObjectOutputStream], [ObjectInputStream] 를 이용한 Binary 직렬화/역직렬화를 수행하는 [BinarySerializer]
 *
 * > **보안 경고**: JDK 직렬화는 신뢰할 수 없는 데이터 소스에서 사용 시 역직렬화 공격(RCE) 취약점이 발생할 수 있습니다.
 * > 신뢰된 환경에서만 사용하거나, [objectInputFilter]를 설정해 허용할 클래스를 제한하세요. (JEP 290 참고)
 * > 기본값으로 [JDK_DEFAULT_OBJECT_INPUT_FILTER]가 적용됩니다.
 *
 * ```kotlin
 * // 기본 필터 적용 (권장) — JDK_DEFAULT_OBJECT_INPUT_FILTER 사용
 * val serializer = JdkBinarySerializer()
 * val bytes = serializer.serialize("Hello, World!")
 * val text = serializer.deserialize<String>(bytes)  // text="Hello, World!"
 *
 * // 추가 패키지 허용: JEP 290 필터 패턴 지정
 * val customFilter = ObjectInputFilter.Config.createFilter("com.example.**;io.bluetape4k.**;kotlin.**;!*")
 * val safeSerializer = JdkBinarySerializer(objectInputFilter = customFilter)
 * ```
 *
 * [serializeTo] and [deserializeFrom] use fixed ByteBuffer-backed streams. The configured
 * [objectInputFilter] is applied to both ByteArray and ByteBuffer deserialization paths.
 * The [issue #1039 evidence](https://github.com/bluetape4k/bluetape4k-projects/blob/develop/docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md)
 * accepted lower allocation for output; the input comparison was inconclusive.
 *
 * @param bufferSize 버퍼 크기 (기본값: [DEFAULT_BUFFER_SIZE])
 * @param objectInputFilter 역직렬화 시 적용할 [ObjectInputFilter]. 기본값: [JDK_DEFAULT_OBJECT_INPUT_FILTER]
 */
class JdkBinarySerializer(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val objectInputFilter: ObjectInputFilter? = JDK_DEFAULT_OBJECT_INPUT_FILTER,
): AbstractBinarySerializer() {

    companion object: KLogging()

    init {
        require(bufferSize > 0) { "bufferSize must be greater than 0." }
    }

    /**
     * [ObjectOutputStream]을 사용하여 [graph]를 직렬화하고 [ByteArray]로 반환합니다.
     *
     * ```kotlin
     * val serializer = JdkBinarySerializer()
     * val bytes = serializer.serialize("Hello, World!")
     * // bytes 는 JDK ObjectOutputStream 포맷의 바이트 배열
     * ```
     *
     * @param graph 직렬화할 객체 (non-null)
     * @return 직렬화된 [ByteArray]
     */
    override fun doSerialize(graph: Any): ByteArray {
        val output = ByteArrayOutputStream(bufferSize)
        ObjectOutputStream(output).use { oos ->
            oos.writeObject(graph)
        }
        return output.toByteArray()
    }

    override fun serializeTo(graph: Any?, target: ByteBuffer): Int {
        if (target.isReadOnly) throw ReadOnlyBufferException()
        val source = graph ?: return 0
        val start = target.position()
        val view = target.duplicate()

        return try {
            ByteBufferOutputStream.fixed(view).use { output ->
                ObjectOutputStream(output).use { oos ->
                    oos.writeObject(source)
                }
            }
            val written = view.position() - start
            target.position(start + written)
            written
        } catch (failure: Throwable) {
            target.position(start)
            throwBufferSerializationFailure(source, failure)
        }
    }

    /**
     * [ObjectInputStream]을 사용하여 [bytes]를 역직렬화하고 객체를 반환합니다.
     * [objectInputFilter]가 설정된 경우 역직렬화 전에 필터를 적용합니다.
     *
     * ```kotlin
     * val filter = ObjectInputFilter.Config.createFilter("java.*;kotlin.*;!*")
     * val serializer = JdkBinarySerializer(objectInputFilter = filter)
     * val bytes = serializer.serialize("Hello, World!")
     * val text = serializer.deserialize<String>(bytes)  // text="Hello, World!"
     * ```
     *
     * @param T 역직렬화할 객체 수형
     * @param bytes 역직렬화할 [ByteArray]
     * @return 역직렬화한 객체, 또는 null
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        return ByteArrayInputStream(bytes).use { bis ->
            ObjectInputStream(bis).apply {
                val filter = this@JdkBinarySerializer.objectInputFilter
                    ?: ObjectInputFilter.Config.getSerialFilter()
                filter?.let { setObjectInputFilter(it) }
            }.use { ois ->
                ois.readObject() as? T
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> deserializeFrom(source: ByteBuffer): T? {
        val sourceSize = source.remaining()
        if (sourceSize == 0) return null

        return try {
            ByteBufferInputStream(source.duplicate()).use { input ->
                ObjectInputStream(input).apply {
                    val filter = this@JdkBinarySerializer.objectInputFilter
                        ?: ObjectInputFilter.Config.getSerialFilter()
                    filter?.let { setObjectInputFilter(it) }
                }.use { ois ->
                    ois.readObject() as? T
                }
            }
        } catch (failure: Throwable) {
            throwBufferDeserializationFailure(sourceSize, failure)
        }
    }
}
