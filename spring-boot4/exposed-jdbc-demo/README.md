# bluetape4k-spring-boot4-exposed-jdbc-demo

Exposed DAO + Spring Data JDBC Repository + Spring MVC 통합 데모 (Spring Boot 4.x)

## 개요

`bluetape4k-spring-boot4-exposed-jdbc` 라이브러리를 활용한 실제 동작 예제입니다.
Spring Boot 3 버전과 동일한 기능을 제공하며, Spring Boot 4 BOM을 사용합니다.

## spring-boot3 버전과의 차이점

| 항목 | spring-boot3 | spring-boot4 |
|------|-------------|-------------|
| BOM | 자동 관리 | `implementation(platform(Libs.spring_boot4_dependencies))` 필수 |
| `SpringTransactionManager` | `exposed.v1.spring.transaction` | `exposed.v1.spring7.transaction` |
| Exposed 스타터 | `exposed-spring-boot-starter` | 수동 `ExposedConfig` (`exposed-spring-boot4-starter` 미사용) |

> **주의**: `exposed-spring-boot4-starter`의 `ExposedAutoConfiguration`은 Spring Boot 4 전용
> `DataSourceAutoConfiguration`을 참조합니다. 현재 런타임이 Spring Boot 3.x 기반이므로
> 수동 `ExposedConfig`로 `SpringTransactionManager`를 구성합니다.

## 주요 기술 스택

- Spring Boot 4 BOM + Spring MVC
- Exposed DAO (`LongEntity`, `LongIdTable`)
- `bluetape4k-spring-boot4-exposed-jdbc` (ExposedJdbcRepository)
- H2 인메모리 데이터베이스

## REST API

| Method | Path              | 설명           |
|--------|-------------------|----------------|
| GET    | /products         | 전체 목록 조회 |
| GET    | /products/{id}    | 단건 조회      |
| POST   | /products         | 생성           |
| PUT    | /products/{id}    | 수정           |
| DELETE | /products/{id}    | 삭제           |
| GET    | /products/search?name=... | 이름 검색 |

## 테스트

```bash
./gradlew :bluetape4k-spring-boot4-exposed-jdbc-demo:test
```
