package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputFilter
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * JDK의 [ObjectOutputStream], [ObjectInputStream] 를 이용한 Binary 직렬화/역직렬화를 수행하는 [BinarySerializer]
 *
 * > **보안 경고**: JDK 직렬화는 신뢰할 수 없는 데이터 소스에서 사용 시 역직렬화 공격(RCE) 취약점이 발생할 수 있습니다.
 * > 신뢰된 환경에서만 사용하거나, [objectInputFilter]를 설정해 허용할 클래스를 제한하세요. (JEP 290 참고)
 *
 * ```
 * // 신뢰된 환경에서 사용
 * val serializer = JdkBinarySerializer()
 * val bytes = serializer.serialize("Hello, World!")
 * val text = serializer.deserialize<String>(bytes)  // text="Hello, World!"
 *
 * // 필터 적용 예시
 * val filter = ObjectInputFilter.Config.createFilter("java.*;kotlin.*;!*")
 * val safeSerializer = JdkBinarySerializer(objectInputFilter = filter)
 * ```
 *
 * @param bufferSize 버퍼 크기 (기본값: [DEFAULT_BUFFER_SIZE])
 * @param objectInputFilter 역직렬화 시 적용할 [ObjectInputFilter]. null이면 시스템 전역 필터를 사용합니다.
 */
class JdkBinarySerializer(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val objectInputFilter: ObjectInputFilter? = null,
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
        BufferedOutputStream(output, bufferSize).use { bos ->
            ObjectOutputStream(bos).use { oos ->
                oos.writeObject(graph)
                oos.flush()
            }
        }
        return output.toByteArray()
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
            BufferedInputStream(bis, bufferSize).use { buffered ->
                ObjectInputStream(buffered).apply {
                    val filter = objectInputFilter ?: ObjectInputFilter.Config.getSerialFilter()
                    filter?.let { setObjectInputFilter(it) }
                }.use { ois ->
                    ois.readObject() as? T
                }
            }
        }
    }
}
