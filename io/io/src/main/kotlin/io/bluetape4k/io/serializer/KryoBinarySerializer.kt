package io.bluetape4k.io.serializer

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import io.bluetape4k.logging.KLogging
import okio.Buffer
import java.io.ByteArrayInputStream

/**
 *  [Kryo](https://github.com/EsotericSoftware/kryo) 라이브러리를 이용하는 [BinarySerializer]
 *
 *  ```
 *  val serializer = KryoBinarySerializer()
 *  val bytes = serializer.serialize("Hello, World!")
 *  val text = serializer.deserialize<String>(bytes)  // text="Hello, World!"
 *  ```
 */
class KryoBinarySerializer(
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
        val buffer = Buffer()

        Output(bufferSize, -1).use { output ->
            output.outputStream = buffer.outputStream()
            withKryo {
                writeClassAndObject(output, graph)
            }
            output.flush()
        }
        return buffer.readByteArray()
    }

    /**
     * I/O 직렬화에서 `doDeserialize` 함수를 제공합니다.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        return ByteArrayInputStream(bytes).use { bis ->
            Input(bufferSize).use { input ->
                input.inputStream = bis
                withKryo {
                    readClassAndObject(input) as? T
                }
            }
        }
    }
}
