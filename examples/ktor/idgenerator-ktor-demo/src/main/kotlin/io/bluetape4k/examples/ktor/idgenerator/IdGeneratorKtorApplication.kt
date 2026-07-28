package io.bluetape4k.examples.ktor.idgenerator

import io.bluetape4k.idgenerators.flake.Flake
import io.bluetape4k.idgenerators.ksuid.KsuidGenerator
import io.bluetape4k.idgenerators.snowflake.SnowflakeGenerator
import io.bluetape4k.idgenerators.ulid.UlidGenerator
import io.bluetape4k.idgenerators.uuid.Uuid
import io.bluetape4k.idgenerators.uuid.UuidGenerator
import io.bluetape4k.ktor.core.installBluetape4kKtorCore
import io.bluetape4k.ktor.core.intQueryParameter
import io.bluetape4k.ktor.core.requiredPathParameter
import io.bluetape4k.ktor.observability.installBluetape4kKtorObservability
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

private const val DEFAULT_BATCH_SIZE = 10
private const val MAX_BATCH_SIZE = 100

/**
 * Ktor idgenerator example application을 설정합니다.
 *
 * ## 계약
 * - 명시적 `/ids/{type}` route와 generic `/idgen/{type}` route는 같은 registry를 공유합니다.
 * - batch generation은 `1..100` 범위의 `size`를 허용하며, `size`를 생략하면 기본값 `10`을 사용합니다.
 * - 지원하지 않는 generator type과 잘못된 batch size는 HTTP 400 JSON error를 반환합니다.
 *
 * ```kotlin
 * embeddedServer(CIO, port = 8080) {
 *     idGeneratorKtorModule()
 * }.start(wait = true)
 * ```
 */
internal fun Application.idGeneratorKtorModule(
    registry: IdGeneratorRegistry = IdGeneratorRegistry.default(),
) {
    installBluetape4kKtorCore()
    installBluetape4kKtorObservability()
    routing {
        idGeneratorRoutes(registry)
    }
}

fun main() {
    embeddedServer(CIO, host = "0.0.0.0", port = 8080) {
        idGeneratorKtorModule()
    }.start(wait = true)
}

internal fun Routing.idGeneratorRoutes(registry: IdGeneratorRegistry) {
    get("/health") {
        call.respond(HealthResponse())
    }
    get("/generators") {
        call.respond(registry.toResponse())
    }

    registry.types.forEach { type ->
        get("/ids/$type") {
            call.respond(registry.nextIdResponse(type))
        }
        get("/ids/$type/batch") {
            call.respond(registry.nextIdsResponse(type, call.batchSize()))
        }
    }

    get("/idgen/{type}") {
        call.respond(registry.nextIdResponse(call.generatorType()))
    }
    get("/idgen/{type}/batch") {
        call.respond(registry.nextIdsResponse(call.generatorType(), call.batchSize()))
    }
}

internal class IdGeneratorRegistry(
    generators: Map<String, () -> String>,
) {
    private val generators: Map<String, () -> String> = generators.toMap()
    val types: List<String> = generators.keys.toList()

    init {
        require(generators.isNotEmpty()) { "generators must not be empty" }
        require(generators.keys.all { it.isNotBlank() }) { "generator type must not be blank" }
    }

    fun nextId(type: String): String =
        generator(type).invoke()

    fun nextIds(type: String, size: Int): List<String> {
        if (size !in 1..MAX_BATCH_SIZE) {
            throw InvalidBatchSizeException(size.toString())
        }
        return List(size) { nextId(type) }
    }

    fun nextIdResponse(type: String): IdResponse =
        IdResponse(type = type, id = nextId(type))

    fun nextIdsResponse(type: String, size: Int): IdBatchResponse =
        IdBatchResponse(type = type, size = size, ids = nextIds(type, size))

    fun toResponse(): GeneratorsResponse =
        GeneratorsResponse(
            generators = types.map { type ->
                GeneratorResponse(
                    type = type,
                    endpoint = "/ids/$type",
                    batchEndpoint = "/ids/$type/batch?size=$DEFAULT_BATCH_SIZE"
                )
            },
            genericEndpoints = listOf(
                "/idgen/{type}",
                "/idgen/{type}/batch?size=$DEFAULT_BATCH_SIZE"
            )
        )

    private fun generator(type: String): () -> String =
        generators[type] ?: throw UnknownGeneratorTypeException(type, types)

    companion object {
        fun default(): IdGeneratorRegistry {
            val uuidV4 = UuidGenerator(Uuid.V4)
            val uuidV7 = UuidGenerator(Uuid.V7)
            val ulid = UlidGenerator()
            val ksuid = KsuidGenerator()
            val snowflake = SnowflakeGenerator()
            val flake = Flake()

            return IdGeneratorRegistry(
                linkedMapOf(
                    "uuid-v4" to { uuidV4.nextUUID().toString() },
                    "uuid-v7" to { uuidV7.nextUUID().toString() },
                    "ulid" to { ulid.nextId() },
                    "ksuid" to { ksuid.nextId() },
                    "snowflake" to { snowflake.nextId().toString() },
                    "flake" to { flake.nextIdAsString() }
                )
            )
        }
    }
}

internal class UnknownGeneratorTypeException(
    type: String,
    supportedTypes: List<String>,
): IllegalArgumentException("Unsupported generator type: $type. Supported types: ${supportedTypes.joinToString()}")

internal class InvalidBatchSizeException(
    size: String,
): IllegalArgumentException("Invalid batch size: $size. Expected range is 1..$MAX_BATCH_SIZE")

@Serializable
internal data class IdResponse(
    val type: String,
    val id: String,
)

@Serializable
internal data class IdBatchResponse(
    val type: String,
    val size: Int,
    val ids: List<String>,
)

@Serializable
internal data class GeneratorResponse(
    val type: String,
    val endpoint: String,
    val batchEndpoint: String,
)

@Serializable
internal data class GeneratorsResponse(
    val generators: List<GeneratorResponse>,
    val genericEndpoints: List<String>,
)

@Serializable
internal data class HealthResponse(
    val status: String = "UP",
)

private fun ApplicationCall.generatorType(): String =
    requiredPathParameter("type")

private fun ApplicationCall.batchSize(): Int =
    intQueryParameter(
        name = "size",
        defaultValue = DEFAULT_BATCH_SIZE,
        range = 1..MAX_BATCH_SIZE
    ) ?: DEFAULT_BATCH_SIZE
