package io.bluetape4k.io.serializer

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer
import com.esotericsoftware.kryo.serializers.EnumNameSerializer
import com.esotericsoftware.kryo.serializers.JavaSerializer
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import com.esotericsoftware.kryo.util.Pool
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.objenesis.strategy.StdInstantiatorStrategy

/**
 * Kryo 인스턴스를 Pool 방식으로 관리하는 싱글톤 유틸리티입니다.
 *
 * Kryo는 Thread-Safe하지 않으므로 직접 공유하면 안 됩니다. `KryoProvider`는 [Pool]을 이용해
 * 스레드마다 Kryo / Input / Output 인스턴스를 안전하게 대여(obtain) · 반납(free)합니다.
 *
 * ```kotlin
 * // Kryo 직렬화
 * val kryo = KryoProvider.obtainKryo()
 * val output = KryoProvider.obtainOutput()
 * try {
 *     kryo.writeClassAndObject(output, myObject)
 *     val bytes = output.toBytes()
 *     println(bytes.size) // 직렬화된 바이트 크기
 * } finally {
 *     KryoProvider.releaseOutput(output)
 *     KryoProvider.releaseKryo(kryo)
 * }
 *
 * // Kryo 역직렬화
 * val kryo2 = KryoProvider.obtainKryo()
 * val input = KryoProvider.obtainInput()
 * try {
 *     input.setBuffer(bytes)
 *     val restored = kryo2.readClassAndObject(input)
 *     println(restored) // 원본 객체
 * } finally {
 *     KryoProvider.releaseInput(input)
 *     KryoProvider.releaseKryo(kryo2)
 * }
 * ```
 */
object KryoProvider: KLogging() {

    private const val MAX_POOL_SIZE = 1024

    private val kryoPool: Pool<Kryo> by lazy {
        /**
         * `object` 싱글톤/유틸리티입니다.
         */
        object: Pool<Kryo>(true, false, MAX_POOL_SIZE) {
            /**
             * I/O 직렬화에서 `create` 함수를 제공합니다.
             */
            override fun create(): Kryo = createKryo()
        }
    }

    private val inputPool by lazy {
        /**
         * `object` 싱글톤/유틸리티입니다.
         */
        object: Pool<Input>(true, false, MAX_POOL_SIZE) {
            /**
             * I/O 직렬화에서 `create` 함수를 제공합니다.
             */
            override fun create(): Input = Input(DEFAULT_BUFFER_SIZE)
        }
    }

    private val outputPool: Pool<Output> by lazy {
        /**
         * `object` 싱글톤/유틸리티입니다.
         */
        object: Pool<Output>(true, false, MAX_POOL_SIZE) {
            /**
             * I/O 직렬화에서 `create` 함수를 제공합니다.
             */
            override fun create(): Output = Output(DEFAULT_BUFFER_SIZE, -1)
        }
    }

    /**
     * Pool에서 [Kryo] 인스턴스를 대여합니다.
     *
     * 사용 후 반드시 [releaseKryo]로 반납해야 합니다.
     *
     * @return Pool에서 대여한 [Kryo] 인스턴스
     */
    fun obtainKryo(): Kryo = kryoPool.obtain()

    /**
     * Pool에서 [Input] 인스턴스를 대여합니다.
     *
     * 사용 후 반드시 [releaseInput]으로 반납해야 합니다.
     *
     * @return Pool에서 대여한 [Input] 인스턴스
     */
    fun obtainInput(): Input = inputPool.obtain()

    /**
     * Pool에서 [Output] 인스턴스를 대여합니다.
     *
     * 사용 후 반드시 [releaseOutput]으로 반납해야 합니다.
     *
     * @return Pool에서 대여한 [Output] 인스턴스
     */
    fun obtainOutput(): Output = outputPool.obtain()

    /**
     * 사용이 끝난 [Kryo] 인스턴스를 Pool에 반납합니다.
     *
     * @param kryo 반납할 [Kryo] 인스턴스
     */
    fun releaseKryo(kryo: Kryo) {
        kryoPool.free(kryo)
    }

    /**
     * 사용이 끝난 [Input] 인스턴스를 Pool에 반납합니다.
     *
     * @param input 반납할 [Input] 인스턴스
     */
    fun releaseInput(input: Input) {
        inputPool.free(input)
    }

    /**
     * 사용이 끝난 [Output] 인스턴스를 Pool에 반납합니다.
     *
     * @param output 반납할 [Output] 인스턴스
     */
    fun releaseOutput(output: Output) {
        outputPool.free(output)
    }

    /**
     * 새로운 [Kryo] 인스턴스를 생성합니다.
     *
     * > **보안 경고**: `isRegistrationRequired = false` 설정은 등록되지 않은 모든 클래스의 역직렬화를 허용합니다.
     * > 신뢰할 수 없는 데이터 소스에서 역직렬화할 경우 임의 코드 실행(RCE) 취약점이 발생할 수 있습니다.
     * > 보안이 중요한 환경에서는 `isRegistrationRequired = true`로 변경하고 허용할 클래스를 명시적으로 등록하세요.
     */
    internal fun createKryo(classLoader: ClassLoader? = null): Kryo {
        log.debug { "Create new Kryo instance ..." }

        return Kryo().apply {
            classLoader?.let { setClassLoader(it) }

            isRegistrationRequired = false
            references = false
            addDefaultSerializer(Throwable::class.java, JavaSerializer())

            // no-arg constructor 가 없더라도 deserialize 가 가능하도록
            // for kryo 5.x
            instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())

            // for kryo 4.x
            // instantiatorStrategy = Kryo.DefaultInstantiatorStrategy(StdInstantiatorStrategy())

            // schema evolution 시 오류를 줄일 수 있다.
            setDefaultSerializer(CompatibleFieldSerializer::class.java)

            // enum ordinal 이 아닌 name 으로 직렬화
            addDefaultSerializer(Enum::class.java, EnumNameSerializer::class.java)

            register(java.util.Optional::class.java)
            register(java.time.Instant::class.java)
            register(java.time.LocalDateTime::class.java)
            register(java.time.LocalDate::class.java)
            register(java.time.LocalTime::class.java)
            register(java.time.OffsetDateTime::class.java)
            register(java.time.OffsetTime::class.java)
            register(java.time.ZonedDateTime::class.java)
        }
    }
}
