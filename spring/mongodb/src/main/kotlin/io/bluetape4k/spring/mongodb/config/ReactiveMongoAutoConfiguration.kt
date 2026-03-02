package io.bluetape4k.spring.mongodb.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
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
 * - 이 자동 구성보다 `spring-boot-autoconfigure`의 `ReactiveMongoAutoConfiguration`이
 *   우선 적용되므로, 사용자 설정이 있으면 무시됩니다.
 * - `spring.data.mongodb.uri` 또는 `spring.data.mongodb.host` 프로퍼티로 MongoDB URI를 지정하세요.
 *
 * ```yaml
 * spring:
 *   data:
 *     mongodb:
 *       uri: mongodb://localhost:27017/test
 * ```
 */
@AutoConfiguration
@ConditionalOnClass(ReactiveMongoOperations::class)
class ReactiveMongoAutoConfiguration {

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
