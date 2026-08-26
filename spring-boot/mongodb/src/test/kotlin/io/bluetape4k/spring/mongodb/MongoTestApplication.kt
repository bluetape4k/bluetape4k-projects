package io.bluetape4k.spring.mongodb

import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Spring Boot 4 MongoDB 통합 테스트용 애플리케이션 클래스입니다.
 *
 * Spring Boot 4에는 `@DataMongoTest` 슬라이스가 아직 없으므로 `@SpringBootTest`로 대체합니다.
 * MongoDB Testcontainer URL과 singleton lifecycle은 [AbstractReactiveMongoTest]의
 * [org.springframework.test.context.DynamicPropertySource]가 소유하므로,
 * 테스트 애플리케이션은 Spring Boot의 Mongo auto-configuration을 그대로 사용합니다.
 */
@SpringBootApplication
class MongoTestApplication
