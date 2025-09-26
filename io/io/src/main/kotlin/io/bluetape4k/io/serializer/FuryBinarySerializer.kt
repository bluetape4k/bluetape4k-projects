package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
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
@Deprecated(
    "Apache Fury 프로젝트가 중단되어 유지보수가 되지 않음에 따라 사용을 권장하지 않습니다. 대신 ForyBinarySerializer 사용을 권장합니다.",
    ReplaceWith("ForyBinarySerializer")
)
class FuryBinarySerializer(
    private val fury: ThreadSafeFury = DefaultFury,
): AbstractBinarySerializer() {

    companion object: KLogging() {
        @JvmStatic
        private val DefaultFury: ThreadSafeFury by lazy {
            Fury.builder()
                .withLanguage(Language.JAVA)
                .withAsyncCompilation(true)
                .withCompatibleMode(CompatibleMode.COMPATIBLE)
                .withRefTracking(true)
                .requireClassRegistration(false)
                .buildThreadSafeFuryPool(4, 4 * Runtime.getRuntime().availableProcessors())
        }
    }

    override fun doSerialize(graph: Any): ByteArray {
        // log.trace { "serialize by fury. graph=$graph" }
        return fury.serialize(graph)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        // log.trace { "deserialize by fury. bytes.size=${bytes.size}" }
        return fury.deserialize(bytes) as? T
    }
}
