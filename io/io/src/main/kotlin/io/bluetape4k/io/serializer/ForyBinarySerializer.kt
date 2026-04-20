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
                .buildThreadSafeForyPool(
                    2,
                    2 * Runtime.getRuntime().availableProcessors(),
                    30L,
                    TimeUnit.MINUTES
                )
        }

        // SCHEMA_CONSISTENT: 필드명 대신 위치 기반 ID 직렬화 — COMPATIBLE 대비 페이로드 크기 및 CPU 절감
        // asyncCompilation=false: 동기 JIT 컴파일로 warmup 내 완전 최적화 보장
        // refTracking=false: 순환참조 없는 DTO 그래프에서 레퍼런스 테이블 오버헤드 제거
        @JvmStatic
        private val FastFory: ThreadSafeFory by lazy {
            Fory.builder()
                .withLanguage(Language.JAVA)
                .withCompatibleMode(CompatibleMode.SCHEMA_CONSISTENT)
                .withAsyncCompilation(false)
                .withRefTracking(false)
                .withRefCopy(false)
                .withCodegen(true)
                .withStringCompressed(false)
                .requireClassRegistration(false)
                .buildThreadSafeForyPool(
                    2,
                    2 * Runtime.getRuntime().availableProcessors(),
                    30L,
                    TimeUnit.MINUTES
                )
        }

        /**
         * SCHEMA_CONSISTENT + refTracking 비활성화로 최적화된 [ForyBinarySerializer]를 반환합니다.
         *
         * 스키마 변경이 없는 고정 타입 DTO 직렬화에 적합합니다.
         * COMPATIBLE 모드 대비 필드명 오버헤드와 레퍼런스 추적 비용 모두 제거.
         */
        @JvmStatic
        fun fast(): ForyBinarySerializer = ForyBinarySerializer(FastFory)

        /**
         * 클래스 등록이 강제되는 보안 [ThreadSafeFory]를 생성합니다.
         *
         * ## 동작/계약
         * - `requireClassRegistration(true)` 설정으로 등록된 클래스만 직렬화/역직렬화를 허용합니다.
         * - 미등록 클래스 직렬화 시도 시 즉시 예외가 발생하므로, 의도치 않은 타입 노출을 방지합니다.
         * - [classes]에 포함한 클래스와 그 필드 타입이 모두 등록되어 있어야 합니다.
         *
         * ```kotlin
         * // 보안 Fory 생성 후 ForyBinarySerializer에 주입
         * val secureFory = ForyBinarySerializer.secureFory(
         *     MyData::class.java,
         *     MyOtherData::class.java,
         * )
         * val serializer = ForyBinarySerializer(fory = secureFory)
         *
         * // 등록된 클래스는 직렬화 가능
         * val bytes = serializer.serialize(MyData("hello"))
         *
         * // 미등록 클래스는 직렬화 시 BinarySerializationException 발생
         * serializer.serialize(UnregisteredClass())  // throws!
         * ```
         *
         * @param classes 직렬화를 허용할 사용자 정의 클래스 목록
         */
        @JvmStatic
        fun secureFory(vararg classes: Class<*>): ThreadSafeFory =
            Fory.builder()
                .withLanguage(Language.JAVA)
                .withCompatibleMode(CompatibleMode.COMPATIBLE)
                .withAsyncCompilation(true)
                .withRefTracking(true)
                .withRefCopy(true)
                .withCodegen(true)
                .withStringCompressed(true)
                .requireClassRegistration(true)   // 보안: 등록된 클래스만 허용
                .buildThreadSafeForyPool(
                    2,
                    2 * Runtime.getRuntime().availableProcessors(),
                    30L,
                    TimeUnit.MINUTES
                ).also { fory ->
                    classes.forEach { fory.register(it) }
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
