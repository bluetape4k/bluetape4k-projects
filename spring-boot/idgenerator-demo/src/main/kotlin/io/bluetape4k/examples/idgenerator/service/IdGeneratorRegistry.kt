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
 * REST API의 문자열 type을 실제 ID generator Bean에 매핑합니다.
 *
 * ## 동작/계약
 * - 지원 type은 `uuid-v4`, `uuid-v7`, `ulid`, `ksuid`, `snowflake`, `flake`입니다.
 * - registry는 generator 구현체를 직접 노출하지 않고 문자열 ID 생성 함수로 감쌉니다.
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
                nextId = uuidV4Generator::nextIdAsString,
            ),
            IdGeneratorEntry(
                type = "uuid-v7",
                description = "Time-ordered UUID v7",
                nextId = uuidV7Generator::nextIdAsString,
            ),
            IdGeneratorEntry(
                type = "ulid",
                description = "Monotonic ULID string",
                nextId = ulidGenerator::nextIdAsString,
            ),
            IdGeneratorEntry(
                type = "ksuid",
                description = "K-Sortable Unique Identifier string",
                nextId = ksuidGenerator::nextIdAsString,
            ),
            IdGeneratorEntry(
                type = "snowflake",
                description = "Twitter-style Snowflake long ID",
                nextId = snowflakeGenerator::nextIdAsString,
            ),
            IdGeneratorEntry(
                type = "flake",
                description = "Boundary-style 128-bit Flake ID encoded as Base62",
                nextId = flakeGenerator::nextIdAsString,
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
 * REST API에서 사용할 ID generator 항목입니다.
 *
 * ## 동작/계약
 * - [nextId]는 매 호출마다 새 문자열 ID를 반환합니다.
 * - type과 description은 endpoint 문서와 `/generators` 응답에 사용합니다.
 */
data class IdGeneratorEntry(
    val type: String,
    val description: String,
    private val nextId: () -> String,
) {
    fun nextId(): String =
        nextId.invoke()
}

/**
 * 지원하지 않는 generator type을 요청했을 때 발생하는 예외입니다.
 */
class UnsupportedGeneratorTypeException(
    val generatorType: String,
    val supportedTypes: Set<String>,
): IllegalArgumentException("Unsupported generator type: $generatorType")
