# bluetape4k-spring-boot3-exposed-r2dbc-demo

Exposed R2DBC + suspend Repository + Spring WebFlux 통합 데모 (Spring Boot 3.x)

## 개요

`bluetape4k-spring-boot3-exposed-r2dbc` 라이브러리를 활용한 실제 동작 예제입니다.
Exposed R2DBC DSL 기반 코루틴 Repository를 Spring WebFlux REST API로 노출합니다.

## 주요 기술 스택

- Spring Boot 3.x + Spring WebFlux (Reactor + Coroutines)
- Exposed R2DBC (`suspendTransaction`, `R2dbcDatabase`)
- `bluetape4k-spring-boot3-exposed-r2dbc` (ExposedR2dbcRepository)
- H2 인메모리 데이터베이스 (R2DBC + JDBC 모두 필요)
- Jackson Kotlin Module

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

## Repository

```kotlin
interface ProductR2dbcRepository : ExposedR2dbcRepository<ProductDto, Long> {
    override val table: IdTable<Long> get() = Products
    override fun toDomain(row: ResultRow) = ProductDto(...)
    override fun toPersistValues(entity: ProductDto) = mapOf(...)
    override fun extractId(entity: ProductDto) = entity.id
}
```

## REST API (suspend)

```kotlin
@RestController
class ProductController(private val repo: ProductR2dbcRepository) {

    @GetMapping("/products")
    suspend fun findAll(): List<ProductDto> = repo.findAll()

    @PostMapping("/products")
    suspend fun create(@RequestBody dto: ProductDto): ProductDto = repo.save(dto)
}
```

## R2DBC + JDBC 이중 설정

`DataInitializer`는 스키마 생성을 위해 Exposed JDBC `transaction`을 사용하므로,
R2DBC (`r2dbc-h2`) 외에 JDBC (`h2`) 드라이버도 런타임에 필요합니다.

## 테스트

```bash
./gradlew :bluetape4k-spring-boot3-exposed-r2dbc-demo:test
```
