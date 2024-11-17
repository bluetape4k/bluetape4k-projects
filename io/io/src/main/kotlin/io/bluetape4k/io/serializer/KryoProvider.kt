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
 * Kryo는 Thread Safe 하지 않습니다. 그래서 Kryo 인스턴스를 Pool 에서 관리합니다.
 */
object KryoProvider: KLogging() {

    private const val MAX_POOL_SIZE = 1024

    private val kryoPool: Pool<Kryo> by lazy {
        object: Pool<Kryo>(true, false, MAX_POOL_SIZE) {
            override fun create(): Kryo = createKryo()
        }
    }

    private val inputPool by lazy {
        object: Pool<Input>(true, false, MAX_POOL_SIZE) {
            override fun create(): Input = Input(DEFAULT_BUFFER_SIZE)
        }
    }

    private val outputPool: Pool<Output> by lazy {
        object: Pool<Output>(true, false, MAX_POOL_SIZE) {
            override fun create(): Output = Output(DEFAULT_BUFFER_SIZE, -1)
        }
    }

    fun obtainKryo(): Kryo = kryoPool.obtain()

    fun obtainInput(): Input = inputPool.obtain()

    fun obtainOutput(): Output = outputPool.obtain()


    fun releaseKryo(kryo: Kryo) {
        kryoPool.free(kryo)
    }

    fun releaseInput(input: Input) {
        inputPool.free(input)
    }

    fun releaseOutput(output: Output) {
        outputPool.free(output)
    }

    /**
     * 새로운 [Kryo] 인스턴스를 생성합니다.
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
