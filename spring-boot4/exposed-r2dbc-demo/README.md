# bluetape4k-spring-boot4-exposed-r2dbc-demo

Exposed R2DBC + suspend Repository + Spring WebFlux 통합 데모 (Spring Boot 4.x)

## 개요

`bluetape4k-spring-boot4-exposed-r2dbc` 라이브러리를 활용한 실제 동작 예제입니다.
Spring Boot 3 버전과 동일한 기능을 제공하며, Spring Boot 4 BOM을 사용합니다.

## spring-boot3 버전과의 차이점

| 항목 | spring-boot3 | spring-boot4 |
|------|-------------|-------------|
| BOM | 자동 관리 | `implementation(platform(Libs.spring_boot4_dependencies))` 필수 |
| 의존 라이브러리 | `bluetape4k-spring-boot3-exposed-r2dbc` | `bluetape4k-spring-boot4-exposed-r2dbc` |

## 주요 기술 스택

- Spring Boot 4 BOM + Spring WebFlux (Reactor + Coroutines)
- Exposed R2DBC (`suspendTransaction`, `R2dbcDatabase`)
- `bluetape4k-spring-boot4-exposed-r2dbc` (ExposedR2dbcRepository)
- H2 인메모리 데이터베이스 (R2DBC + JDBC 모두 필요)

## 구조

```
src/main/kotlin/io/bluetape4k/examples/exposed/webflux/
├── WebfluxDemoApplication.kt            # @SpringBootApplication + @EnableExposedR2dbcRepositories
├── domain/
│   └── ProductEntity.kt                 # Exposed 테이블 + ProductDto
├── repository/
│   └── ProductR2dbcRepository.kt        # ExposedR2dbcRepository 구현 (suspend)
├── controller/
│   └── ProductController.kt             # WebFlux REST API (suspend fun)
└── config/
    ├── DataInitializer.kt               # 스키마 생성 + 샘플 데이터 삽입
    └── ExposedR2dbcConfig.kt            # R2dbcDatabase 빈 설정
```

## 테스트

```bash
./gradlew :bluetape4k-spring-boot4-exposed-r2dbc-demo:test
```
