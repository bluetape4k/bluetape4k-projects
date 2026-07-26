package io.bluetape4k.benchmark.webframework

import io.bluetape4k.idgenerators.flake.Flake
import io.bluetape4k.idgenerators.ksuid.KsuidGenerator
import io.bluetape4k.idgenerators.snowflake.SnowflakeGenerator
import io.bluetape4k.idgenerators.ulid.UlidGenerator
import io.bluetape4k.idgenerators.uuid.Uuid
import io.bluetape4k.idgenerators.uuid.UuidGenerator
import io.bluetape4k.ktor.core.installBluetape4kKtorCore
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.io.Serializable as JavaSerializable

private const val LOOPBACK = "127.0.0.1"
private const val DEFAULT_BATCH_SIZE = 10
private const val MAX_BATCH_SIZE = 100

/**
 * Throughput benchmark for equivalent Ktor CIO and Spring WebFlux ID endpoints.
 *
 * Each operation is one blocking JDK HTTP client round trip to the embedded server.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
open class WebFrameworkRequestBenchmark: WebFrameworkBenchmarkSupport()

/**
 * Average latency benchmark for equivalent Ktor CIO and Spring WebFlux ID endpoints.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
open class WebFrameworkLatencyBenchmark: WebFrameworkBenchmarkSupport()

/**
 * Startup and resident-memory snapshot benchmark for the same benchmark apps.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
open class WebFrameworkStartupBenchmark {

    @Benchmark
    fun ktorStartup(blackhole: Blackhole) {
        KtorBenchmarkServer().use { server ->
            blackhole.consume(server.baseUrl)
            blackhole.consume(usedMemoryBytes())
        }
    }

    @Benchmark
    fun springWebFluxStartup(blackhole: Blackhole) {
        SpringWebFluxBenchmarkServer().use { server ->
            blackhole.consume(server.baseUrl)
            blackhole.consume(usedMemoryBytes())
        }
    }
}

@State(Scope.Benchmark)
open class WebFrameworkBenchmarkSupport {

    private lateinit var servers: BenchmarkServers
    private lateinit var client: HttpClient

    @Setup(Level.Trial)
    fun setup() {
        servers = BenchmarkServers.start()
        client = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build()
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        servers.close()
    }

    @Benchmark
    fun ktorHealth(blackhole: Blackhole) {
        blackhole.consume(get(servers.ktor.baseUrl, "/health"))
    }

    @Benchmark
    fun springWebFluxHealth(blackhole: Blackhole) {
        blackhole.consume(get(servers.spring.baseUrl, "/health"))
    }

    @Benchmark
    fun ktorSingleId(blackhole: Blackhole) {
        blackhole.consume(get(servers.ktor.baseUrl, "/idgen/uuid-v7"))
    }

    @Benchmark
    fun springWebFluxSingleId(blackhole: Blackhole) {
        blackhole.consume(get(servers.spring.baseUrl, "/idgen/uuid-v7"))
    }

    @Benchmark
    fun ktorBatchIds(blackhole: Blackhole) {
        blackhole.consume(get(servers.ktor.baseUrl, "/idgen/uuid-v7/batch?size=10"))
    }

    @Benchmark
    fun springWebFluxBatchIds(blackhole: Blackhole) {
        blackhole.consume(get(servers.spring.baseUrl, "/idgen/uuid-v7/batch?size=10"))
    }

    @Benchmark
    fun ktorBadRequest(blackhole: Blackhole) {
        blackhole.consume(get(servers.ktor.baseUrl, "/idgen/unknown"))
    }

    @Benchmark
    fun springWebFluxBadRequest(blackhole: Blackhole) {
        blackhole.consume(get(servers.spring.baseUrl, "/idgen/unknown"))
    }

    private fun get(baseUrl: String, path: String): BenchmarkHttpResponse {
        val request = HttpRequest
            .newBuilder(URI.create("$baseUrl$path"))
            .timeout(Duration.ofSeconds(3))
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return BenchmarkHttpResponse(
            statusCode = response.statusCode(),
            bodyLength = response.body().length,
        )
    }
}

private class BenchmarkServers(
    val ktor: KtorBenchmarkServer,
    val spring: SpringWebFluxBenchmarkServer,
): AutoCloseable {

    override fun close() {
        runCatching { ktor.close() }
        runCatching { spring.close() }
    }

    companion object {
        fun start(): BenchmarkServers =
            BenchmarkServers(
                ktor = KtorBenchmarkServer(),
                spring = SpringWebFluxBenchmarkServer(),
            )
    }
}

private class KtorBenchmarkServer: AutoCloseable {

    private val server =
        embeddedServer(CIO, host = LOOPBACK, port = 0) {
            idGeneratorKtorBenchmarkModule()
        }.start(wait = false)
    private val port: Int = runBlocking {
        server.engine.resolvedConnectors().first().port
    }

    val baseUrl: String = "http://$LOOPBACK:$port"

    override fun close() {
        server.stop(gracePeriodMillis = 100, timeoutMillis = 1_000)
    }
}

private class SpringWebFluxBenchmarkServer: AutoCloseable {

    private val context: ConfigurableApplicationContext =
        SpringApplicationBuilder(SpringWebFluxBenchmarkApplication::class.java)
            .web(WebApplicationType.REACTIVE)
            .properties(
                mapOf(
                    "server.address" to LOOPBACK,
                    "server.port" to 0,
                    "spring.main.banner-mode" to "off",
                    "spring.main.lazy-initialization" to "false",
                    "logging.level.root" to "ERROR",
                )
            )
            .run()
    private val port: Int =
        context.environment.getRequiredProperty("local.server.port", Int::class.java)

    val baseUrl: String = "http://$LOOPBACK:$port"

    override fun close() {
        context.close()
    }
}

private fun Application.idGeneratorKtorBenchmarkModule(
    registry: BenchmarkIdRegistry = BenchmarkIdRegistry.default(),
) {
    installBluetape4kKtorCore()
    routing {
        idGeneratorKtorBenchmarkRoutes(registry)
    }
}

private fun Routing.idGeneratorKtorBenchmarkRoutes(registry: BenchmarkIdRegistry) {
    get("/health") {
        call.respond(HealthResponse(status = "UP", supportedTypes = registry.supportedTypes))
    }
    get("/idgen/{type}") {
        call.respond(registry.nextIdResponse(call.parameters["type"].orEmpty()))
    }
    get("/idgen/{type}/batch") {
        val size = call.request.queryParameters["size"]?.toIntOrNull() ?: DEFAULT_BATCH_SIZE
        call.respond(registry.nextIdsResponse(call.parameters["type"].orEmpty(), size))
    }
}

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableConfigurationProperties
private class SpringWebFluxBenchmarkApplication {

    @Bean
    fun benchmarkIdRegistry(): BenchmarkIdRegistry =
        BenchmarkIdRegistry.default()
}

@RestController
private class SpringWebFluxBenchmarkController(
    private val registry: BenchmarkIdRegistry,
) {

    @GetMapping("/health")
    suspend fun health(): HealthResponse =
        HealthResponse(status = "UP", supportedTypes = registry.supportedTypes)

    @GetMapping("/idgen/{type}")
    suspend fun generate(
        @PathVariable type: String,
    ): IdResponse =
        registry.nextIdResponse(type)

    @GetMapping("/idgen/{type}/batch")
    suspend fun generateBatch(
        @PathVariable type: String,
        @RequestParam(required = false) size: Int?,
    ): IdBatchResponse =
        registry.nextIdsResponse(type, size ?: DEFAULT_BATCH_SIZE)
}

@RestControllerAdvice
private class SpringWebFluxBenchmarkExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequest(ex: IllegalArgumentException): ErrorResponse =
        ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "bad_request",
            message = ex.message ?: "Bad request",
            path = "",
        )
}

private class BenchmarkIdRegistry(
    entries: Map<String, () -> String>,
) {
    private val entries: Map<String, () -> String> = entries.toMap()

    val supportedTypes: List<String> = entries.keys.toList()

    fun nextIdResponse(type: String): IdResponse =
        IdResponse(type = type, id = entry(type).invoke())

    fun nextIdsResponse(type: String, size: Int): IdBatchResponse {
        require(size in 1..MAX_BATCH_SIZE) {
            "Batch size must be between 1 and $MAX_BATCH_SIZE: $size"
        }
        return IdBatchResponse(
            type = type,
            size = size,
            ids = List(size) { entry(type).invoke() },
        )
    }

    private fun entry(type: String): () -> String =
        entries[type] ?: throw IllegalArgumentException("Unsupported generator type: $type")

    companion object {
        fun default(): BenchmarkIdRegistry {
            val uuidV4 = UuidGenerator(Uuid.V4)
            val uuidV7 = UuidGenerator(Uuid.V7)
            val ulid = UlidGenerator()
            val ksuid = KsuidGenerator()
            val snowflake = SnowflakeGenerator()
            val flake = Flake()

            return BenchmarkIdRegistry(
                linkedMapOf(
                    "uuid-v4" to { uuidV4.nextIdAsString() },
                    "uuid-v7" to { uuidV7.nextIdAsString() },
                    "ulid" to { ulid.nextIdAsString() },
                    "ksuid" to { ksuid.nextIdAsString() },
                    "snowflake" to { snowflake.nextIdAsString() },
                    "flake" to { flake.nextIdAsString() },
                )
            )
        }
    }
}

@Serializable
private data class HealthResponse(
    val status: String,
    val supportedTypes: List<String>,
): JavaSerializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
private data class IdResponse(
    val type: String,
    val id: String,
): JavaSerializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
private data class IdBatchResponse(
    val type: String,
    val size: Int,
    val ids: List<String>,
): JavaSerializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
private data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: String = Instant.now().toString(),
): JavaSerializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private data class BenchmarkHttpResponse(
    val statusCode: Int,
    val bodyLength: Int,
): JavaSerializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private fun usedMemoryBytes(): Long {
    val runtime = Runtime.getRuntime()
    return runtime.totalMemory() - runtime.freeMemory()
}
