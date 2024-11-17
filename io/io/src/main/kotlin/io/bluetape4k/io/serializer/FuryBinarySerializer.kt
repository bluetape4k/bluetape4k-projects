package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.fury.Fury
import org.apache.fury.ThreadSafeFury
import org.apache.fury.config.CompatibleMode
import org.apache.fury.config.Language

/**
 * [Fury](https://fury.apache.org/) 를 이용한 Binary 직렬화/역직렬화를 수행하는 [BinarySerializer]
 *
 * ```
 * val serializer = FuryBinarySerializer()
 * val bytes = serializer.serialize("Hello, World!")
 * val text = serializer.deserialize<String>(bytes)  // text="Hello, World!"
 * ```
 */
class FuryBinarySerializer(
    private val fury: ThreadSafeFury = DefaultFury,
): AbstractBinarySerializer() {

    companion object: KLogging() {
        @JvmStatic
        private val DefaultFury: ThreadSafeFury by lazy {
            Fury.builder()
                .requireClassRegistration(false)
                .withLanguage(Language.JAVA)
                .withAsyncCompilation(true)
                .withCompatibleMode(CompatibleMode.COMPATIBLE)
                .withRefTracking(true)
                .requireClassRegistration(false)
                .buildThreadSafeFuryPool(8, 4 * Runtime.getRuntime().availableProcessors())
        }
    }

    override fun doSerialize(graph: Any): ByteArray {
        log.debug { "serialize by fury. graph=$graph" }
        return fury.serialize(graph)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        log.debug { "deserialize by fury. bytes.size=${bytes.size}" }
        return fury.deserialize(bytes) as? T
    }
}
