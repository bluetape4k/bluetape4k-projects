package io.bluetape4k.io.serializer

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import io.bluetape4k.logging.KLogging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 *  [Kryo](https://github.com/EsotericSoftware/kryo) 라이브러리를 이용하는 [BinarySerializer]
 *
 *  > **보안 참고**: 기본 설정은 `isRegistrationRequired = false`라 모든 클래스를 직렬화합니다.
 *  > 신뢰할 수 없는 데이터를 역직렬화하는 환경이라면 [KryoBinarySerializer.secure]로 생성하여
 *  > 허용할 클래스를 명시적으로 등록하세요.
 *
 *  ```
 *  // 기본 사용 (클래스 등록 불필요)
 *  val serializer = KryoBinarySerializer()
 *  val bytes = serializer.serialize("Hello, World!")
 *  val text = serializer.deserialize<String>(bytes)  // text="Hello, World!"
 *
 *  // 보안 사용 (등록된 클래스만 허용)
 *  val secureSerializer = KryoBinarySerializer.secure(MyData::class.java)
 *  ```
 *
 * @param bufferSize 내부 버퍼 크기 (기본값: [DEFAULT_BUFFER_SIZE])
 * @param kryoPool 커스텀 Kryo 풀. null이면 기본 [KryoProvider] 풀을 사용합니다.
 */
class KryoBinarySerializer(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val kryoPool: Pool<Kryo>? = null,
): AbstractBinarySerializer() {

    companion object: KLogging() {

        /**
         * 클래스 등록이 강제되는 보안 [KryoBinarySerializer]를 생성합니다.
         *
         * ## 동작/계약
         * - `isRegistrationRequired = true` 설정으로 등록된 클래스만 직렬화/역직렬화를 허용합니다.
         * - 미등록 클래스 직렬화 시도 시 즉시 예외가 발생하므로, 의도치 않은 타입 노출을 방지합니다.
         * - [classes]에 포함한 클래스와 그 필드 타입이 모두 등록되어 있어야 합니다.
         * - 내부적으로 풀 기반(Pool)으로 Kryo 인스턴스를 관리하여 thread-safe하게 동작합니다.
         *
         * ```kotlin
         * // 보안 Kryo 직렬화기 생성
         * val serializer = KryoBinarySerializer.secure(
         *     MyData::class.java,
         *     MyOtherData::class.java,
         * )
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
        fun secure(vararg classes: Class<*>): KryoBinarySerializer {
            val pool = object: Pool<Kryo>(true, false, 1024) {
                override fun create(): Kryo = KryoProvider.createKryo().apply {
                    isRegistrationRequired = true
                    classes.forEach { register(it) }
                }
            }
            return KryoBinarySerializer(kryoPool = pool)
        }
    }

    init {
        require(bufferSize > 0) { "bufferSize must be greater than 0." }
    }

    /**
     * 커스텀 [kryoPool]이 있으면 해당 풀을 사용하고, 없으면 기본 [KryoProvider] 풀을 사용합니다.
     */
    private fun <T> useKryo(block: Kryo.() -> T): T {
        val pool = kryoPool
        return if (pool != null) {
            val kryo = pool.obtain()
            try {
                block(kryo)
            } finally {
                pool.free(kryo)
            }
        } else {
            val kryo = KryoProvider.obtainKryo()
            try {
                block(kryo)
            } finally {
                KryoProvider.releaseKryo(kryo)
            }
        }
    }

    /**
     * I/O 직렬화에서 `doSerialize` 함수를 제공합니다.
     */
    override fun doSerialize(graph: Any): ByteArray {
        val buffer = ByteArrayOutputStream(bufferSize)

        Output(bufferSize, -1).use { output ->
            output.outputStream = buffer
            useKryo {
                writeClassAndObject(output, graph)
            }
            output.flush()
        }
        return buffer.toByteArray()
    }

    /**
     * I/O 직렬화에서 `doDeserialize` 함수를 제공합니다.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        return ByteArrayInputStream(bytes).use { bis ->
            Input(bufferSize).use { input ->
                input.inputStream = bis
                useKryo {
                    readClassAndObject(input) as? T
                }
            }
        }
    }
}
