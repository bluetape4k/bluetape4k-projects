package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import org.apache.fory.Fory
import org.apache.fory.ThreadSafeFory
import org.apache.fory.config.CompatibleMode
import org.apache.fory.config.Language
import java.util.concurrent.TimeUnit

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
                .withCompatibleMode(CompatibleMode.COMPATIBLE)
                .withAsyncCompilation(true)
                .withRefTracking(true)
                .withRefCopy(true)
                .withCodegen(true)
                .withStringCompressed(true)
                .requireClassRegistration(false)
                // .registerGuavaTypes(true)
                .buildThreadSafeForyPool(
                    2,
                    2 * Runtime.getRuntime().availableProcessors(),
                    30L,
                    TimeUnit.MINUTES
                )
        }
    }

    /**
     * I/O 직렬화에서 `doSerialize` 함수를 제공합니다.
     */
    override fun doSerialize(graph: Any): ByteArray {
        return fory.serialize(graph)
    }

    /**
     * I/O 직렬화에서 `doDeserialize` 함수를 제공합니다.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        return fory.deserialize(bytes) as? T
    }
}
