package io.bluetape4k.examples.idgenerator.config

import io.bluetape4k.idgenerators.flake.Flake
import io.bluetape4k.idgenerators.ksuid.KsuidGenerator
import io.bluetape4k.idgenerators.snowflake.SnowflakeGenerator
import io.bluetape4k.idgenerators.ulid.UlidGenerator
import io.bluetape4k.idgenerators.uuid.Uuid
import io.bluetape4k.idgenerators.uuid.UuidGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * idgenerator 구현체를 Spring bean으로 등록하는 example configuration입니다.
 *
 * ## Behavior
 * - UUID generators of the same type are distinguished by bean name.
 * - Controllers and services map string types to generators through the registry instead of concrete generators.
 *
 * ```kotlin
 * @Autowired
 * lateinit var uuidV7Generator: UuidGenerator
 * ```
 */
@Configuration(proxyBeanMethods = false)
class IdGeneratorConfiguration {

    @Bean
    fun uuidV4Generator(): UuidGenerator =
        UuidGenerator(Uuid.V4)

    @Bean
    fun uuidV7Generator(): UuidGenerator =
        UuidGenerator(Uuid.V7)

    @Bean
    fun ulidGenerator(): UlidGenerator =
        UlidGenerator()

    @Bean
    fun ksuidGenerator(): KsuidGenerator =
        KsuidGenerator()

    @Bean
    fun snowflakeGenerator(): SnowflakeGenerator =
        SnowflakeGenerator()

    @Bean
    fun flakeGenerator(): Flake =
        Flake()
}
