package io.bluetape4k.spring.mongodb

import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * `@DataMongoTest` 슬라이스 테스트가 `@SpringBootConfiguration`을 찾을 수 있도록
 * 제공하는 최소 Spring Boot 애플리케이션 클래스입니다.
 *
 * 실제로 실행되지 않으며, 테스트 컨텍스트 설정 목적으로만 사용됩니다.
 */
@SpringBootApplication
class MongoTestApplication
