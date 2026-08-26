package io.bluetape4k.spring.mongodb.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.MongoConverter

/**
 * Spring Data MongoDB Reactive Auto-configuration.
 *
 * ## 동작/계약
 * - `spring-boot-starter-data-mongodb-reactive` 의존성과 함께 `ReactiveMongoOperations` Bean이
 *   이미 등록되어 있지 않은 경우에만 [ReactiveMongoTemplate]을 등록합니다.
 * - Spring Boot 4.1의 `MongoReactiveAutoConfiguration`과
 *   `DataMongoReactiveAutoConfiguration` 이후에 적용되어 기본 template과 충돌하지 않습니다.
 * - Spring Boot 4.1의 `MongoProperties` binding namespace인 `spring.mongodb.*`를 사용합니다.
 *   legacy-only `spring.data.mongodb.uri`는 조용한 localhost fallback을 막기 위해 fail-fast합니다.
 *
 * ```yaml
 * spring:
 *   mongodb:
 *     uri: mongodb://localhost:27017/test
 * ```
 */
@AutoConfiguration(
    afterName = [
        "org.springframework.boot.data.mongodb.autoconfigure.DataMongoReactiveAutoConfiguration",
    ],
)
@ConditionalOnClass(ReactiveMongoOperations::class)
class ReactiveMongoAutoConfiguration(
    environment: Environment,
) {

    init {
        if (environment.containsProperty(LEGACY_URI_PROPERTY) &&
            !environment.containsProperty(CURRENT_URI_PROPERTY)
        ) {
            throw IllegalStateException(LEGACY_URI_MESSAGE)
        }
    }

    private companion object {
        const val CURRENT_URI_PROPERTY = "spring.mongodb.uri"
        const val LEGACY_URI_PROPERTY = "spring.data.mongodb.uri"
        const val LEGACY_URI_MESSAGE =
            "Unsupported legacy MongoDB property 'spring.data.mongodb.uri'; use 'spring.mongodb.uri' on Spring Boot 4.1+"
    }

    /**
     * [ReactiveMongoOperations] Bean이 없는 경우 [ReactiveMongoTemplate]을 자동으로 등록합니다.
     *
     * ## 동작/계약
     * - `ReactiveMongoDatabaseFactory`와 `MongoConverter`가 이미 Bean으로 등록되어 있어야 합니다.
     * - Spring Boot의 `ReactiveMongoAutoConfiguration`이 이미 이 Bean을 등록하므로,
     *   일반적으로 이 메서드는 실행되지 않습니다.
     *
     * @param databaseFactory MongoDB reactive 데이터베이스 팩토리
     * @param mongoConverter MongoDB 변환기
     * @return [ReactiveMongoTemplate] 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean(ReactiveMongoOperations::class)
    fun reactiveMongoTemplate(
        databaseFactory: ReactiveMongoDatabaseFactory,
        mongoConverter: MongoConverter,
    ): ReactiveMongoTemplate = ReactiveMongoTemplate(databaseFactory, mongoConverter)
}
