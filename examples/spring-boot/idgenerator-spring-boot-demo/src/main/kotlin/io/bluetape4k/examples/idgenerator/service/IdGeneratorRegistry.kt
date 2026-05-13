package io.bluetape4k.examples.idgenerator.service

import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.idgenerators.flake.Flake
import io.bluetape4k.idgenerators.ksuid.KsuidGenerator
import io.bluetape4k.idgenerators.snowflake.SnowflakeGenerator
import io.bluetape4k.idgenerators.ulid.UlidGenerator
import io.bluetape4k.idgenerators.uuid.UuidGenerator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * Maps REST API generator type strings to the actual ID generator beans.
 *
 * ## Behavior
 * - Supported types are `uuid-v4`, `uuid-v7`, `ulid`, `ksuid`, `snowflake`, and `flake`.
 * - The registry wraps generators as string ID suppliers instead of exposing concrete implementations.
 *
 * ```kotlin
 * val next = registry.get("uuid-v7").nextId()
 * ```
 */
@Component
class IdGeneratorRegistry(
    @Qualifier("uuidV4Generator") uuidV4Generator: UuidGenerator,
    @Qualifier("uuidV7Generator") uuidV7Generator: UuidGenerator,
    ulidGenerator: UlidGenerator,
    ksuidGenerator: KsuidGenerator,
    snowflakeGenerator: SnowflakeGenerator,
    flakeGenerator: Flake,
) {
    private val entries: Map<String, IdGeneratorEntry> =
        listOf(
            IdGeneratorEntry(
                type = "uuid-v4",
                description = "Random UUID v4",
                idSupplier = uuidV4Generator::nextIdAsString,
            ),
            IdGeneratorEntry(
                type = "uuid-v7",
                description = "Time-ordered UUID v7",
                idSupplier = uuidV7Generator::nextIdAsString,
            ),
            IdGeneratorEntry(
                type = "ulid",
                description = "Monotonic ULID string",
                idSupplier = ulidGenerator::nextIdAsString,
            ),
            IdGeneratorEntry(
                type = "ksuid",
                description = "K-Sortable Unique Identifier string",
                idSupplier = ksuidGenerator::nextIdAsString,
            ),
            IdGeneratorEntry(
                type = "snowflake",
                description = "Twitter-style Snowflake long ID",
                idSupplier = snowflakeGenerator::nextIdAsString,
            ),
            IdGeneratorEntry(
                type = "flake",
                description = "Boundary-style 128-bit Flake ID encoded as Base62",
                idSupplier = flakeGenerator::nextIdAsString,
            ),
        ).associateBy { it.type }

    val supportedTypes: Set<String>
        get() = entries.keys

    fun all(): List<IdGeneratorEntry> =
        entries.values.toList()

    fun get(type: String): IdGeneratorEntry =
        entries[type] ?: throw UnsupportedGeneratorTypeException(type, supportedTypes)
}

/**
 * ID generator entry used by the REST API.
 *
 * ## Behavior
 * - [nextId] returns a fresh string ID on every call.
 * - [type] and [description] are used by endpoint documentation and the `/generators` response.
 */
class IdGeneratorEntry(
    val type: String,
    val description: String,
    private val idSupplier: () -> String,
) {
    fun nextId(): String =
        idSupplier.invoke()
}

/**
 * Exception raised when a caller requests an unsupported generator type.
 */
class UnsupportedGeneratorTypeException(
    val generatorType: String,
    val supportedTypes: Set<String>,
): IllegalArgumentException("Unsupported generator type: $generatorType")
