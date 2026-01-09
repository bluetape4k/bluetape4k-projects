package io.bluetape4k.io.benchmark


import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.junit5.faker.Fakers
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.NANOSECONDS)
class BinarySerializerBenchmark {

    private val jdk = BinarySerializers.Jdk
    private val kryo = BinarySerializers.Kryo
    private val jsonMapper = jacksonObjectMapper().findAndRegisterModules()

    data class SimpleData(
        val id: Long,
        val name: String,
        val age: Int,
        val birth: LocalDate,
        val biography: String,
        val zip: String,
        val address: String,
        val price: BigDecimal? = null,
        val uuid: UUID? = null,
        val bytes: ByteArray? = null,
    ): Serializable

    private val faker = Fakers.faker

    private lateinit var targets: List<SimpleData>

    private fun createSimpleData(): SimpleData {
        return SimpleData(
            id = faker.number().randomNumber(),
            name = faker.name().fullName(),
            age = faker.number().numberBetween(1, 100),
            birth = faker.timeAndDate().birthday(),
            biography = faker.lorem().paragraph(),
            zip = faker.address().zipCode(),
            address = faker.address().fullAddress(),
            price = BigDecimal(faker.number().randomDouble(2, 0, 1000)),
            // uuid = UUID.randomUUID(),
            bytes = faker.random().nextRandomBytes(4096)
        )
    }

    @Setup
    fun setup() {
        targets = List(20) { createSimpleData() }
    }

    @Benchmark
    fun jdk() {
        with(jdk) {
            val bytes = serialize(targets)
            val results = deserialize<List<SimpleData>>(bytes)!!
            results.shouldNotBeEmpty() shouldHaveSize targets.size
        }
    }

    /**
     * Java 버전에 따라 Kryo가 UUID를 제대로 처리하지 못하는 이슈가 있음
     *
     * [Serializing UUID not working with java 17 (but same works on java 11)](https://github.com/EsotericSoftware/kryo/issues/859)
     * 를 참고해서 JvmArgs 에 추가해도 Java 21 에서는 안됨
     */
    @Benchmark
    fun kryo() {
        with(kryo) {
            val bytes = serialize(targets)
            val results = deserialize<List<SimpleData>>(bytes)!!
            results.shouldNotBeEmpty() shouldHaveSize targets.size
        }
    }

    @Benchmark
    fun jsonMapper() {
        with(jsonMapper) {
            val bytes = writeValueAsBytes(targets)
            val results = readValue<List<SimpleData>>(bytes)
            results.shouldNotBeEmpty() shouldHaveSize targets.size
        }
    }
}
