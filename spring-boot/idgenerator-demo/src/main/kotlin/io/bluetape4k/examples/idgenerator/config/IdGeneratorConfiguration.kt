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
 * idgenerator 구현체를 Spring Bean으로 등록하는 예제 설정입니다.
 *
 * ## 동작/계약
 * - 같은 타입의 UUID generator는 Bean 이름으로 구분합니다.
 * - Controller와 Service는 concrete generator 대신 registry를 통해 문자열 type을 generator에 매핑합니다.
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
