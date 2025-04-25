package io.bluetape4k.io.serializer

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

    override fun doSerialize(graph: Any): ByteArray {
        val buffer = Buffer()

        withKryoOutput { output ->
            output.outputStream = buffer.outputStream()
            withKryo {
                writeClassAndObject(output, graph)
            }
            output.flush()
            output.buffer
        }
        return buffer.readByteArray().apply { buffer.close() }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        ByteArrayInputStream(bytes).use { bis ->
            withKryoInput { input ->
                input.inputStream = bis
                withKryo {
                    return readClassAndObject(input) as? T
                }
            }
        }
    }
}
