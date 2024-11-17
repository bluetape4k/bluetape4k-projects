package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okio.Buffer
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

    override fun doSerialize(graph: Any): ByteArray {
        log.debug { "serialize by jdk. graph=$graph" }

        val output = Buffer()
        ObjectOutputStream(output.outputStream()).use { oos ->
            oos.writeObject(graph)
            oos.flush()
        }
        return output.readByteArray().apply { output.close() }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        log.debug { "deserialize by jdk. bytes.size=${bytes.size}" }

        ByteArrayInputStream(bytes).use { bis ->
            ObjectInputStream(bis).use { ois ->
                return ois.readObject() as? T
            }
        }
    }
}
