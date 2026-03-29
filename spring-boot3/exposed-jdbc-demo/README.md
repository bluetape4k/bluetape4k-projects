# bluetape4k-spring-boot3-exposed-jdbc-demo

Exposed DAO + Spring Data JDBC Repository + Spring MVC 통합 데모 (Spring Boot 3.x)

## 개요

`bluetape4k-spring-boot3-exposed-jdbc` 라이브러리를 활용한 실제 동작 예제입니다.
Exposed DAO Entity와 Spring Data Repository를 Spring MVC REST API로 노출합니다.

## 주요 기술 스택

- Spring Boot 3.x + Spring MVC
- Exposed DAO (`LongEntity`, `LongIdTable`)
- `bluetape4k-spring-boot3-exposed-jdbc` (ExposedJdbcRepository)
- H2 인메모리 데이터베이스
- Jackson Kotlin Module

## 구조

```
src/main/kotlin/io/bluetape4k/examples/exposed/mvc/
├── DemoApplication.kt               # @SpringBootApplication + @EnableExposedJdbcRepositories
├── domain/
│   └── ProductEntity.kt             # Exposed DAO Entity + ProductDto
├── repository/
│   └── ProductJdbcRepository.kt     # ExposedJdbcRepository 구현
├── controller/
│   └── ProductController.kt         # REST API (CRUD + 검색)
└── config/
    └── DataInitializer.kt           # 스키마 생성 + 샘플 데이터 삽입
```

## 도메인 모델

```kotlin
object Products : LongIdTable("products") {
    val name  = varchar("name", 255)
    val price = decimal("price", 10, 2)
    val stock = integer("stock").default(0)
}

@ExposedEntity
class ProductEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ProductEntity>(Products)
    var name:  String         by Products.name
    var price: BigDecimal     by Products.price
    var stock: Int            by Products.stock
}
```

## Repository

```kotlin
interface ProductJdbcRepository : ExposedJdbcRepository<ProductEntity, Long> {
    fun findByName(name: String): List<ProductEntity>
    fun findByPriceLessThan(price: BigDecimal): List<ProductEntity>
}
```

## REST API

| Method | Path              | 설명           |
|--------|-------------------|----------------|
| GET    | /products         | 전체 목록 조회 |
| GET    | /products/{id}    | 단건 조회      |
| POST   | /products         | 생성           |
| PUT    | /products/{id}    | 수정           |
| DELETE | /products/{id}    | 삭제           |
| GET    | /products/search?name=... | 이름 검색 |

## 실행

```bash
./gradlew :bluetape4k-spring-boot3-exposed-jdbc-demo:bootRun
```

## 테스트

```bash
./gradlew :bluetape4k-spring-boot3-exposed-jdbc-demo:test
```
