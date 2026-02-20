# Module bluetape4k-hibernate

Hibernate ORM/JPA 사용 시 반복 코드를 줄이는 Kotlin 확장 라이브러리입니다.

## 개요

`bluetape4k-hibernate`는 [Hibernate ORM](https://hibernate.org/orm/)과 Jakarta Persistence API를 Kotlin 환경에서 더 편리하게 사용할 수 있도록 다양한 확장 함수와 유틸리티를 제공합니다.

### 주요 기능

- **JPA 엔티티 베이스 클래스**: `IntJpaEntity`, `LongJpaEntity`, `UuidJpaEntity`, Tree 계열 엔티티
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

    // Hibernate (필요한 버전 선택)
    implementation("org.hibernate.orm:hibernate-core:6.6.41")
    
    // Querydsl (선택)
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
}
```

## 기본 사용법

### 1. JPA 엔티티 베이스 클래스

미리 정의된 추상 클래스를 상속받아 엔티티를 쉽게 정의할 수 있습니다.

```kotlin
import io.bluetape4k.hibernate.model.LongJpaEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User: LongJpaEntity() {
    var name: String = ""
    var email: String = ""
    var active: Boolean = true
}

@Entity
@Table(name = "products")
class Product: IntJpaEntity() {
    var name: String = ""
    var price: BigDecimal = BigDecimal.ZERO
}

@Entity
@Table(name = "sessions")
class Session: UuidJpaEntity() {
    var userId: Long = 0
    var token: String = ""
    var expiresAt: Instant = Instant.now()
}
```

#### Tree 구조 엔티티

계층형 데이터를 위한 Tree 엔티티 베이스 클래스를 제공합니다.

```kotlin
import io.bluetape4k.hibernate.model.LongJpaTreeEntity

@Entity
@Table(name = "categories")
class Category: LongJpaTreeEntity() {
    var name: String = ""
    var description: String = ""
}
```

### 2. EntityManager 확장 함수

#### CRUD 작업

```kotlin
import io.bluetape4k.hibernate.*

// 저장 (persist 또는 merge 자동 선택)
val savedUser = em.save(user)

// 삭제
em.delete(user)
em.deleteById<User>(1L)

// 조회
val user = em.findAs<User>(1L)
val user = em.findOne<User>(1L)
val exists = em.exists<User>(1L)

// 전체 조회
val users = em.findAll(User::class.java)

// 카운트
val count = em.countAll<User>()

// 전체 삭제
val deletedCount = em.deleteAll<User>()
```

#### Query 생성

```kotlin
import io.bluetape4k.hibernate.*

// TypedQuery 생성
val query = em.newQuery<User>()
val query = em.createQueryAs<User>("SELECT u FROM User u WHERE u.active = true")

// 페이징 설정
val pagedQuery = query.setPaging(firstResult = 0, maxResults = 10)
```

#### Session 접근

```kotlin
import io.bluetape4k.hibernate.*

// Hibernate Session 가져오기
val session = em.currentSession()
val session = em.asSession()

// SessionFactory 가져오기
val sessionFactory = em.sessionFactory()

// JDBC Connection 가져오기
val connection = em.currentConnection()

// 로드 여부 확인
val isLoaded = em.isLoaded(user)
val isPropertyLoaded = em.isLoaded(user, "orders")
```

### 3. Criteria API 확장

```kotlin
import io.bluetape4k.hibernate.criteria.*

val cb = em.criteriaBuilder

// CriteriaQuery 생성
val query = cb.createQueryAs<User>()
val root = query.from<User>()

// 속성 참조
val namePath = root.attribute(User::name)

// Predicate 생성
val predicate = cb.eq(root.get<String>("name"), "John")
val predicate2 = cb.ne(root.get<Boolean>("active"), false)
val inPredicate = cb.inValues(root.get<Long>("id")).apply {
    value(1L)
    value(2L)
    value(3L)
}

query.where(predicate)
val users = em.createQuery(query).resultList
```

### 4. TypedQuery 확장

```kotlin
import io.bluetape4k.hibernate.criteria.*

val query = em.createQuery("SELECT u.id FROM User u WHERE u.active = true")

// 결과 타입 변환
val ids: LongArray = query.longArray()
val ids: IntArray = query.intArray()

// 단일 값 조회
val count = query.longValue()
val count = query.intValue()
```

### 5. StatelessSession 지원

대량 배치 작업에 적합한 StatelessSession을 지원합니다.

```kotlin
import io.bluetape4k.hibernate.stateless.*

em.withStateless { stateless ->
    // 대량 삽입
    largeDataList.forEach { data ->
        stateless.insert(data)
    }
    
    // 대량 업데이트
    stateless.executeUpdate("UPDATE User u SET u.active = false WHERE u.lastLogin < :date", params)
}
```

### 6. Querydsl 확장

```kotlin
import io.bluetape4k.hibernate.querydsl.core.*

val qUser = QUser.user

// BooleanExpression 결합
val predicate = qUser.active.eq(true)
    .and(qUser.email.endsWith("@example.com"))
    .and(qUser.createdAt.gt(LocalDate.of(2024, 1, 1)))

// 쿼리 실행
val users = queryFactory
    .selectFrom(qUser)
    .where(predicate)
    .fetch()
```

### 7. Converter 사용

다양한 AttributeConverter를 제공합니다.

#### 직렬화 Converter

```kotlin
import io.bluetape4k.hibernate.converters.*

@Entity
class UserData {
    @Id
    var id: Long? = null
    
    // JDK 직렬화 + Base64
    @Convert(converter = JdkObjectAsBase64StringConverter::class)
    @Column(length = 4000)
    var metadata: Any? = null
    
    // Kryo 직렬화 + LZ4 압축 + Base64
    @Convert(converter = LZ4KryoObjectAsBase64StringConverter::class)
    @Column(length = 4000)
    var largeData: Any? = null
}
```

#### 암호화 Converter

```kotlin
import io.bluetape4k.hibernate.converters.EncryptedStringConverters

@Entity
class SecureData {
    @Id
    var id: Long? = null
    
    // AES 암호화
    @Convert(converter = EncryptedStringConverters.AES::class)
    var creditCard: String = ""
    
    // Jasypt 암호화
    @Convert(converter = EncryptedStringConverters.Jasypt::class)
    var ssn: String = ""
}
```

#### 압축 Converter

```kotlin
import io.bluetape4k.hibernate.converters.CompressedStringConverter

@Entity
class Document {
    @Id
    var id: Long? = null
    
    // Zstd 압축
    @Convert(converter = CompressedStringConverter.Zstd::class)
    @Lob
    var content: String = ""
}
```

#### 기타 Converter

```kotlin
import io.bluetape4k.hibernate.converters.*

@Entity
class Event {
    @Id
    var id: Long? = null
    
    // Locale -> String
    @Convert(converter = LocaleAsStringConverter::class)
    var locale: Locale = Locale.getDefault()
    
    // Duration -> Timestamp
    @Convert(converter = DurationAsTimestampConverter::class)
    var duration: Duration = Duration.ZERO
}
```

## 주요 파일/클래스 목록

### Model (model/)

| 파일                      | 설명                |
|-------------------------|-------------------|
| `JpaEntity.kt`          | JPA 엔티티 인터페이스     |
| `AbstractJpaEntity.kt`  | JPA 엔티티 추상 클래스    |
| `IntJpaEntity.kt`       | Int ID 엔티티        |
| `LongJpaEntity.kt`      | Long ID 엔티티       |
| `UuidJpaEntity.kt`      | UUID 엔티티          |
| `JpaTreeEntity.kt`      | Tree 구조 엔티티 인터페이스 |
| `IntJpaTreeEntity.kt`   | Int ID Tree 엔티티   |
| `LongJpaTreeEntity.kt`  | Long ID Tree 엔티티  |
| `JpaLocalizedEntity.kt` | 다국어 지원 엔티티        |
| `TreeNodePosition.kt`   | Tree 노드 위치 계산     |

### EntityManager 확장

| 파일                               | 설명                      |
|----------------------------------|-------------------------|
| `EntityManagerSupport.kt`        | EntityManager 확장 함수     |
| `EntityManagerFactorySupport.kt` | EntityManagerFactory 확장 |

### Session 확장

| 파일                         | 설명                   |
|----------------------------|----------------------|
| `SessionSupport.kt`        | Hibernate Session 확장 |
| `SessionFactorySupport.kt` | SessionFactory 확장    |

### Criteria (criteria/)

| 파일                     | 설명              |
|------------------------|-----------------|
| `CriteriaSupport.kt`   | Criteria API 확장 |
| `TypedQuerySupport.kt` | TypedQuery 확장   |

### Stateless Session (stateless/)

| 파일                              | 설명                     |
|---------------------------------|------------------------|
| `StatelessSessionSupport.kt`    | StatelessSession 확장    |
| `StatelessSessionExtensions.kt` | StatelessSession 확장 함수 |

### Querydsl (querydsl/)

| 파일                                 | 설명                  |
|------------------------------------|---------------------|
| `core/ExpressionsSupport.kt`       | Expression 확장       |
| `core/SimpleExpressionSupport.kt`  | SimpleExpression 확장 |
| `core/StringExpressionsSupport.kt` | StringExpression 확장 |
| `core/MathExpressionsSupport.kt`   | MathExpression 확장   |
| `core/ProjectionsSupport.kt`       | Projections 확장      |
| `jpa/JpaExpressionSupport.kt`      | JPA Expression 확장   |

### Converters (converters/)

| 파일                                 | 설명                 |
|------------------------------------|--------------------|
| `LocaleAsStringConverter.kt`       | Locale 변환          |
| `DurationAsTimestampConverter.kt`  | Duration 변환        |
| `EncryptedStringConverters.kt`     | 문자열 암호화            |
| `CompressedStringConverter.kt`     | 문자열 압축             |
| `ObjectAsBase64StringConverter.kt` | 객체 직렬화 + Base64    |
| `ObjectAsByteArrayConverter.kt`    | 객체 직렬화 + ByteArray |
| `AbstractObjectAsJsonConverter.kt` | JSON 직렬화 베이스       |

### Listeners (listeners/)

| 파일                           | 설명          |
|------------------------------|-------------|
| `HibernateEntityListener.kt` | 엔티티 이벤트 리스너 |

## 테스트

```bash
# 모든 테스트 실행
./gradlew :bluetape4k-hibernate:test

# 특정 테스트 실행
./gradlew :bluetape4k-hibernate:test --tests "io.bluetape4k.hibernate.*"
```

## 참고

- [Hibernate ORM](https://hibernate.org/orm/)
- [Hibernate ORM Documentation](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html)
- [Jakarta Persistence](https://jakarta.ee/specifications/persistence/)
- [Querydsl](http://querydsl.com/)
