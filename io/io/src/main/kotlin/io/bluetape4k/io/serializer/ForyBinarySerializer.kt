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
 * > **보안 경고**: 기본 설정(`requireClassRegistration(false)`)은 등록되지 않은 모든 클래스의 역직렬화를 허용합니다.
 * > 신뢰할 수 없는 데이터 소스에서 역직렬화할 경우 임의 코드 실행(RCE) 취약점이 발생할 수 있습니다.
 * > 보안이 중요한 환경에서는 `requireClassRegistration(true)`로 변경하고 허용할 클래스를 명시적으로 등록하세요.
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
