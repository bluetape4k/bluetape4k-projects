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
- **Converter 지원**: Locale/암복호화(Google Tink)/압축/직렬화 기반 converter
- **StatelessSession 지원**: 트랜잭션 처리와 reified 헬퍼 제공

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-hibernate:${version}")

    // Hibernate (필요한 버전 선택)
    implementation("org.hibernate.orm:hibernate-core:6.6.41")

    // Querydsl (선택)
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")

    // 암호화 Converter 사용 시 (Google Tink)
    compileOnly("io.github.bluetape4k:bluetape4k-tink:${version}")

    // JSON Converter 사용 시
    compileOnly("io.github.bluetape4k:bluetape4k-jackson:${version}")

    // 직렬화 Converter 사용 시 (Kryo 또는 Apache Fory)
    compileOnly("com.esotericsoftware:kryo:5.6.2")
    compileOnly("org.apache.fury:fury-kotlin:0.10.0")
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
class Category: LongJpaTreeEntity<Category>() {
    var name: String = ""
    var description: String = ""
}

// 자식 추가/제거
val parent = Category()
val child = Category()
parent.addChildren(child)    // child.parent = parent 자동 설정
parent.removeChildren(child) // child.parent = null 자동 설정
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

// Long 결과 변환
val longQuery = em.createQuery("SELECT u.id FROM User u WHERE u.active = true", java.lang.Long::class.java)
val ids: LongArray = longQuery.longArray()
val idList: List<Long> = longQuery.longList()
val singleId: Long? = longQuery.longResult()

// Int 결과 변환
val intQuery = em.createQuery("SELECT COUNT(*) FROM User u", java.lang.Integer::class.java)
val count: Int? = intQuery.intResult()

// 단일 결과 (없으면 null)
val typedQuery = em.createQuery("SELECT u FROM User u WHERE u.id = :id", User::class.java)
val user: User? = typedQuery.findOneOrNull()
```

### 5. StatelessSession 지원

대량 배치 작업에 적합한 StatelessSession을 지원합니다.

```kotlin
import io.bluetape4k.hibernate.stateless.*

// SessionFactory 기반
sessionFactory.withStateless { stateless ->
    largeDataList.forEach { data ->
        stateless.insert(data)
    }
}

// EntityManager 기반
em.withStateless { stateless ->
    // reified 조회
    val entity = stateless.getAs<User>(userId)

    // 쿼리 실행
    val results = stateless.createQueryAs<User>("FROM User WHERE active = true").list()

    // Native 쿼리
    val users = stateless.createNativeQueryAs<User>("SELECT * FROM users").list()
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

객체를 직렬화하여 ByteArray(Base64 인코딩)로 DB에 저장합니다.
JDK / Kryo / Apache Fory 직렬화와 LZ4, Snappy, Zstd 압축을 조합할 수 있습니다.

```kotlin
import io.bluetape4k.hibernate.converters.*

@Entity
class UserData {
    @Id
    var id: Long? = null

    // JDK 직렬화 → Base64 인코딩 → ByteArray
    @Convert(converter = JdkObjectAsByteArrayConverter::class)
    @Column(length = 4000)
    var metadata: Any? = null

    // Kryo 직렬화 + LZ4 압축 → Base64 인코딩 → ByteArray
    @Convert(converter = LZ4KryoObjectAsByteArrayConverter::class)
    @Column(length = 4000)
    var largeData: Any? = null

    // Apache Fory 직렬화 + Zstd 압축 → Base64 인코딩 → ByteArray
    @Convert(converter = ZstdForyObjectAsByteArrayConverter::class)
    @Column(length = 4000)
    var compressedData: Any? = null
}
```

#### 암호화 Converter

[Google Tink](https://github.com/google/tink) 기반의 AES 암호화 컨버터를 제공합니다.

- `AESStringConverter`: AES-256-GCM (비결정적, 매번 다른 암호문)
- `DeterministicAESStringConverter`: AES-256-SIV (결정적, 동일 평문 → 동일 암호문, WHERE 절 조회 가능)

```kotlin
import io.bluetape4k.hibernate.converters.AESStringConverter
import io.bluetape4k.hibernate.converters.DeterministicAESStringConverter

@Entity
class SecureData {
    @Id
    var id: Long? = null

    // AES-256-GCM 암호화 (비결정적)
    @Convert(converter = AESStringConverter::class)
    var creditCard: String? = null

    // AES-256-SIV 결정적 암호화 (WHERE 절로 조회 가능)
    @Convert(converter = DeterministicAESStringConverter::class)
    var password: String? = null
}
```

#### 압축 Converter

문자열을 압축하여 저장합니다. 지원 알고리즘: BZip2, Deflate, GZip, LZ4, Snappy, Zstd.

```kotlin
import io.bluetape4k.hibernate.converters.ZstdStringConverter
import io.bluetape4k.hibernate.converters.LZ4StringConverter

@Entity
class Document {
    @Id
    var id: Long? = null

    // Zstd 압축 (높은 압축률)
    @Convert(converter = ZstdStringConverter::class)
    @Lob
    var content: String = ""

    // LZ4 압축 (높은 속도)
    @Convert(converter = LZ4StringConverter::class)
    @Column(length = 8000)
    var summary: String = ""
}
```

#### 기타 Converter

```kotlin
import io.bluetape4k.hibernate.converters.*

@Entity
class Event {
    @Id
    var id: Long? = null

    // Locale -> BCP 47 language tag 문자열
    @Convert(converter = LocaleAsStringConverter::class)
    var locale: Locale = Locale.getDefault()

    // Duration -> Timestamp (밀리초)
    @Convert(converter = DurationAsTimestampConverter::class)
    var duration: Duration = Duration.ZERO
}
```

#### JSON Converter

```kotlin
import io.bluetape4k.hibernate.converters.AbstractObjectAsJsonConverter

data class Option(val name: String, val value: String): Serializable

// 커스텀 JSON Converter 정의
class OptionAsJsonConverter: AbstractObjectAsJsonConverter<Option>(Option::class.java)

@Entity
class Purchase {
    @Id
    var id: Long? = null

    @Convert(converter = OptionAsJsonConverter::class)
    var option: Option? = null
}
```

## 주요 파일/클래스 목록

### Model (model/)

| 파일                     | 설명                |
|------------------------|-------------------|
| `JpaEntity.kt`         | JPA 엔티티 인터페이스     |
| `AbstractJpaEntity.kt` | JPA 엔티티 추상 클래스    |
| `IntJpaEntity.kt`      | Int ID 엔티티        |
| `LongJpaEntity.kt`     | Long ID 엔티티       |
| `UuidJpaEntity.kt`     | UUID (Timebased) 엔티티 |
| `JpaTreeEntity.kt`     | Tree 구조 엔티티 인터페이스 |
| `IntJpaTreeEntity.kt`  | Int ID Tree 엔티티   |
| `LongJpaTreeEntity.kt` | Long ID Tree 엔티티  |
| `TreeNodePosition.kt`  | Tree 노드 위치 값 객체   |

### EntityManager 확장

| 파일                               | 설명                      |
|----------------------------------|-------------------------|
| `EntityManagerSupport.kt`        | EntityManager 확장 함수     |
| `EntityManagerFactorySupport.kt` | EntityManagerFactory 확장 |

### Session 확장

| 파일                    | 설명                   |
|-----------------------|----------------------|
| `SessionSupport.kt`   | Hibernate Session 확장 |
| `HibernateConsts.kt`  | Hibernate 기본 설정 상수   |

### Criteria (criteria/)

| 파일                     | 설명              |
|------------------------|-----------------|
| `CriteriaSupport.kt`   | Criteria API 확장 |
| `TypedQuerySupport.kt` | TypedQuery 확장   |

### Stateless Session (stateless/)

| 파일                              | 설명                     |
|---------------------------------|------------------------|
| `StatelessSesisonSupport.kt`    | withStateless 트랜잭션 래퍼  |
| `StatelessSessionExtensions.kt` | StatelessSession reified 확장 함수 |

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

| 파일                                 | 설명                                       |
|------------------------------------|------------------------------------------|
| `LocaleAsStringConverter.kt`       | Locale ↔ BCP 47 문자열                     |
| `DurationAsTimestampConverter.kt`  | Duration ↔ Timestamp                     |
| `EncryptedStringConverters.kt`     | Google Tink AES-GCM / AES-SIV 암호화      |
| `CompressedStringConverter.kt`     | BZip2/Deflate/GZip/LZ4/Snappy/Zstd 압축  |
| `ObjectAsByteArrayConverter.kt`    | Jdk/Kryo/Fory 직렬화 + 압축 → ByteArray   |
| `ObjectAsBase64StringConverter.kt` | 객체 직렬화 → Base64 문자열                    |
| `AbstractObjectAsJsonConverter.kt` | 객체 → JSON 문자열 변환 베이스 클래스               |

### Listeners (listeners/)

| 파일                           | 설명                                        |
|------------------------------|-------------------------------------------|
| `HibernateEntityListener.kt` | PostCommit 이벤트 리스너 (insert/update/delete) |
| `JpaEntityEventLogger.kt`    | Pre/Post JPA 이벤트 로깅 리스너                   |

## 테스트

```bash
# 모든 테스트 실행
./gradlew :bluetape4k-hibernate:test

# 특정 테스트 실행
./gradlew :bluetape4k-hibernate:test --tests "io.bluetape4k.hibernate.*"

# Converter 단위 테스트
./gradlew :bluetape4k-hibernate:test --tests "io.bluetape4k.hibernate.converter.*"
```

## 참고

- [Hibernate ORM](https://hibernate.org/orm/)
- [Hibernate ORM Documentation](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html)
- [Jakarta Persistence](https://jakarta.ee/specifications/persistence/)
- [Querydsl](http://querydsl.com/)
- [Google Tink](https://github.com/google/tink)
