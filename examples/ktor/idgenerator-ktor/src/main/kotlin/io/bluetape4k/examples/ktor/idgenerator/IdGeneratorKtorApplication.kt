package io.bluetape4k.examples.ktor.idgenerator

import io.bluetape4k.idgenerators.flake.Flake
import io.bluetape4k.idgenerators.ksuid.KsuidGenerator
import io.bluetape4k.idgenerators.snowflake.SnowflakeGenerator
import io.bluetape4k.idgenerators.ulid.UlidGenerator
import io.bluetape4k.idgenerators.uuid.Uuid
import io.bluetape4k.idgenerators.uuid.UuidGenerator
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val DEFAULT_BATCH_SIZE = 10
private const val MAX_BATCH_SIZE = 100

/**
 * Configures the Ktor idgenerator example application.
 *
 * ## Contract
 * - Explicit `/ids/{type}` routes and generic `/idgen/{type}` routes share the same registry.
 * - Batch generation accepts `size` in `1..100`; omitted `size` defaults to `10`.
 * - Unsupported generator types and invalid batch sizes return HTTP 400 JSON errors.
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
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
        })
    }
    install(StatusPages) {
        exception<UnknownGeneratorTypeException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("unsupported_generator_type", cause.message ?: "Unsupported generator type")
            )
        }
        exception<InvalidBatchSizeException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("invalid_batch_size", cause.message ?: "Invalid batch size")
            )
        }
    }
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

@Serializable
internal data class ErrorResponse(
    val error: String,
    val message: String,
)

private fun ApplicationCall.generatorType(): String =
    parameters["type"] ?: throw UnknownGeneratorTypeException("", emptyList())

private fun ApplicationCall.batchSize(): Int =
    request.batchSize()

private fun ApplicationRequest.batchSize(): Int {
    val rawSize = queryParameters["size"] ?: return DEFAULT_BATCH_SIZE
    return rawSize.toIntOrNull()
        ?.takeIf { it in 1..MAX_BATCH_SIZE }
        ?: throw InvalidBatchSizeException(rawSize)
}
