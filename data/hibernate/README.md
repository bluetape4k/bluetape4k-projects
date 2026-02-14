# Module bluetape4k-hibernate

Hibernate ORM/JPA 사용 시 반복 코드를 줄이는 Kotlin 확장 라이브러리입니다.

## 주요 기능

- **JPA 엔티티 베이스 클래스**: `IntJpaEntity`, `LongJpaEntity`, Tree 계열 엔티티
- **EntityManager 확장**: `save`, `delete`, `findAs`, `countAll`, `deleteAll` 등
- **Session/SessionFactory 확장**: 배치/리스너/세션 보조 기능
- **Criteria/TypedQuery 확장**: `createQueryAs`, `attribute`, `long/int` 변환 유틸
- **Querydsl 확장**: BooleanExpression 결합, 연산자 보조
- **Converter 지원**: Locale/암복호화/압축/직렬화 기반 converter
- **StatelessSession 지원**: 트랜잭션 처리와 reified 헬퍼 제공

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-hibernate:${version}")
}
```

## 주요 기능 상세

### 1. JPA 엔티티 베이스

- `model/IntJpaEntity.kt`
- `model/LongJpaEntity.kt`
- `model/JpaTreeEntity.kt`
- `model/IntJpaTreeEntity.kt`, `model/LongJpaTreeEntity.kt`

### 2. EntityManager / Session 확장

- `EntityManagerSupport.kt`
- `EntityManagerFactorySupport.kt`
- `SessionSupport.kt`
- `SessionFactorySupport.kt`

예시:

```kotlin
val loaded = em.findAs<MyEntity>(id)
val count = em.countAll<MyEntity>()
val deleted = em.deleteAll<MyEntity>()
```

### 3. Criteria / TypedQuery 확장

- `criteria/CriteriaSupport.kt`
- `criteria/TypedQuerySupport.kt`

예시:

```kotlin
val cq = em.criteriaBuilder.createQueryAs<MyEntity>()
val root = cq.from<MyEntity>()

val ids: LongArray = em.createQueryAs<java.lang.Long>("select e.id from my_entity e")
    .longArray()
```

### 4. Stateless Session 지원

- `stateless/StatelessSesisonSupport.kt`
- `stateless/StatelessSessionExtensions.kt`

예시:

```kotlin
em.withStateless { stateless ->
    stateless.insert(entity)
}
```

### 5. Querydsl 확장

- `querydsl/core/ExpressionsSupport.kt`
- `querydsl/core/SimpleExpressionSupport.kt`
- `querydsl/core/StringExpressionsSupport.kt`

### 6. Converter

- `converters/LocaleAsStringConverter.kt`
- `converters/*Converter.kt`

## 참고

- [Hibernate ORM](https://hibernate.org/orm/)
- [Jakarta Persistence](https://jakarta.ee/specifications/persistence/)
