package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import org.apache.fory.Fory
import org.apache.fory.ThreadSafeFory
import org.apache.fory.config.CompatibleMode
import org.apache.fory.config.Language

/**
 * [Fory](https://fory.apache.org/) 를 이용한 Binary 직렬화/역직렬화를 수행하는 [BinarySerializer]
 *
 * ```
 * val serializer = ForyBinarySerializer()
 * val bytes = serializer.serialize("Hello, World!")
 * val text = serializer.deserialize<String>(bytes)  // text="Hello, World!"
 * ```
 */
class ForyBinarySerializer(
    private val fory: ThreadSafeFory = DefaultFory,
): AbstractBinarySerializer() {

    companion object: KLogging() {
        @JvmStatic
        private val DefaultFory: ThreadSafeFory by lazy {
            Fory.builder()
                .withLanguage(Language.JAVA)
                .withAsyncCompilation(true)
                .withCompatibleMode(CompatibleMode.COMPATIBLE)
                .withRefTracking(true)
                .requireClassRegistration(false)
                .buildThreadSafeForyPool(4, 4 * Runtime.getRuntime().availableProcessors())
        }
    }

    override fun doSerialize(graph: Any): ByteArray {
        // log.trace { "serialize by fory. graph=$graph" }
        return fory.serialize(graph)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        // log.trace { "deserialize by fory. bytes.size=${bytes.size}" }
        return fory.deserialize(bytes) as? T
    }
}
