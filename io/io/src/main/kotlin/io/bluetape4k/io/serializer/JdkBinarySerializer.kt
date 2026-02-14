package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import okio.Buffer
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * JDK의 [ObjectOutputStream], [ObjectInputStream] 를 이용한 Binary 직렬화/역직렬화를 수행하는 [BinarySerializer]
 *
 * ```
 * val serializer = JdkBinarySerializer()
 * val bytes = serializer.serialize("Hello, World!")
 * val text = serializer.deserialize<String>(bytes)  // text="Hello, World!"
 * ```
 */
class JdkBinarySerializer(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
): AbstractBinarySerializer() {

    companion object: KLogging()

    init {
        require(bufferSize > 0) { "bufferSize must be greater than 0." }
    }

    /**
     * I/O 직렬화에서 `doSerialize` 함수를 제공합니다.
     */
    override fun doSerialize(graph: Any): ByteArray {
        val output = Buffer()
        BufferedOutputStream(output.outputStream(), bufferSize).use { bos ->
            ObjectOutputStream(bos).use { oos ->
                oos.writeObject(graph)
                oos.flush()
            }
        }
        return output.readByteArray()
    }

    /**
     * I/O 직렬화에서 `doDeserialize` 함수를 제공합니다.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        return ByteArrayInputStream(bytes).use { bis ->
            BufferedInputStream(bis, bufferSize).use { buffered ->
                ObjectInputStream(buffered).use { ois ->
                    ois.readObject() as? T
                }
            }
        }
    }
}
