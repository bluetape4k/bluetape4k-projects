# spring/mongodb 모듈 구현 계획

## Context

bluetape4k-projects는 Kotlin 기반 공용 라이브러리 모음으로, `spring/` 하위에 Spring Data 통합 모듈들을 제공합니다. 현재 `spring/cassandra`,
`spring/r2dbc` 모듈이 Reactive Operations를 Kotlin Coroutines `suspend`/`Flow`로 래핑하는 패턴을 사용 중입니다. 또한
`data/mongodb` 모듈이 MongoDB Kotlin Coroutine Driver 기반의 저수준 확장함수를 이미 제공하고 있습니다.

이 플랜은 `spring-data-mongodb-reactive`를 Kotlin Coroutines에서 쉽게 사용할 수 있는 `spring/mongodb` 모듈을 새로 만드는 작업입니다.

### 참조 패턴

- **spring/cassandra**: `ReactiveCassandraOperations` 확장함수 (`xxxSuspending`, `xxxAsFlow` 네이밍), `Criteria` infix DSL (
  `eq`), 테스트 베이스 클래스 (`AbstractCassandraCoroutineTest`)
- **spring/r2dbc**: `R2dbcEntityOperations` 확장함수 (`existsSuspending`, `countSuspending`, `selectSuspending` 등)
- **data/mongodb**: MongoDB Kotlin Coroutine Driver 기반 저수준 확장함수 (`findFirst`, `exists`, `upsert`, `findAsFlow`)
- **spring/retrofit2**: `AutoConfiguration.imports` 파일을 통한 Spring Boot 자동 구성 패턴

### 모듈 네이밍 규칙

`settings.gradle.kts`의 `includeModules("spring", withProjectName = true, withBaseDir = true)` 규칙에 따라:

- 디렉토리: `spring/mongodb/`
- 프로젝트명: `bluetape4k-spring-mongodb`
- 패키지: `io.bluetape4k.spring.mongodb`

---

## Work Objectives

1. `ReactiveMongoOperations` / `ReactiveMongoTemplate`의 주요 CRUD 메서드를 `suspend`/`Flow`로 래핑
2. `org.springframework.data.mongodb.core.query.Criteria` 위에 Kotlin infix DSL 구축
3. Spring Boot Auto-configuration으로 간단 설정 지원
4. Testcontainers MongoDB 기반 통합 테스트 인프라 구축

---

## Guardrails

### Must Have

- `spring/cassandra` 모듈의 네이밍 컨벤션 준수 (`xxxSuspending`, `xxxAsFlow`)
- 모든 public API에 KDoc 한국어 주석
- `awaitSingle`/`awaitSingleOrNull`/`asFlow` 패턴 사용 (kotlinx-coroutines-reactor)
- Testcontainers MongoDB 기반 테스트
- Spring Boot 3.4+ 호환

### Must NOT Have

- `data/mongodb` 모듈과 기능 중복 (이 모듈은 Spring Data MongoDB 레이어, data/mongodb는 Native Driver 레이어)
- Deprecated `suspendXxx` 별칭 (신규 모듈이므로 레거시 별칭 불필요)
- 불필요한 아키텍처 변경

---

## Task Flow

```
[Step 1] 모듈 골격 생성
    |
    v
[Step 2] ReactiveMongoOperations Coroutines 확장함수
    |
    v
[Step 3] Criteria/Query infix DSL
    |
    v
[Step 4] Auto-configuration 및 설정 지원
    |
    v
[Step 5] 테스트 인프라 및 통합 테스트
```

---

## Detailed TODOs

### Step 1: 모듈 골격 생성

**파일 목록:**

- `spring/mongodb/build.gradle.kts`
- 소스 디렉토리 구조 생성

**build.gradle.kts 구성:**

```
plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-spring-core"))
    api(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-jackson"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_mongodb)

    // Spring Data MongoDB Reactive
    api(Libs.springBootStarter("data-mongodb-reactive"))

    compileOnly(Libs.springBoot("autoconfigure"))
    compileOnly(Libs.springBoot("configuration-processor"))
    annotationProcessor(Libs.springBoot("configuration-processor"))

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    // Coroutines
    api(Libs.kotlinx_coroutines_core)
    api(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    implementation(Libs.reactor_core)
    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)
}
```

**디렉토리 구조:**

```
spring/mongodb/
  src/main/kotlin/io/bluetape4k/spring/mongodb/
    coroutines/          -- ReactiveMongoOperations 확장함수
    query/               -- Criteria/Query infix DSL
    config/              -- Auto-configuration
  src/main/resources/
    META-INF/spring/
  src/test/kotlin/io/bluetape4k/spring/mongodb/
    coroutines/          -- 확장함수 테스트
    query/               -- DSL 테스트
    model/               -- 테스트용 도메인 모델
```

**Acceptance Criteria:**

- `./gradlew :bluetape4k-spring-mongodb:compileKotlin` 성공
- 프로젝트가 settings.gradle.kts에 의해 자동 감지됨

---

### Step 2: ReactiveMongoOperations Coroutines 확장함수

**파일 목록:**

- `coroutines/ReactiveMongoOperationsCoroutines.kt` -- 핵심 CRUD suspend/Flow 확장
- `coroutines/ReactiveMongoTemplateCoroutines.kt` -- ReactiveMongoTemplate 전용 확장 (필요시)
- `coroutines/ReactiveFluentMongoOperationsCoroutines.kt` -- Fluent API 확장 (find/insert/update/remove/aggregation)

**ReactiveMongoOperationsCoroutines.kt 주요 함수:**

| 원본 메서드                                                 | 확장함수                                         | 반환 타입                       |
|--------------------------------------------------------|----------------------------------------------|-----------------------------|
| `find(query, entityClass)`                             | `findAsFlow<T>(query)`                       | `Flow<T>`                   |
| `findOne(query, entityClass)`                          | `findOneSuspending<T>(query)`                | `T`                         |
| `findOne(query, entityClass)`                          | `findOneOrNullSuspending<T>(query)`          | `T?`                        |
| `findById(id, entityClass)`                            | `findByIdSuspending<T>(id)`                  | `T`                         |
| `findById(id, entityClass)`                            | `findByIdOrNullSuspending<T>(id)`            | `T?`                        |
| `findAll(entityClass)`                                 | `findAllAsFlow<T>()`                         | `Flow<T>`                   |
| `count(query, entityClass)`                            | `countSuspending<T>(query)`                  | `Long`                      |
| `exists(query, entityClass)`                           | `existsSuspending<T>(query)`                 | `Boolean`                   |
| `insert(entity)`                                       | `insertSuspending<T>(entity)`                | `T`                         |
| `insertAll(entities)`                                  | `insertAllAsFlow<T>(entities)`               | `Flow<T>`                   |
| `save(entity)`                                         | `saveSuspending<T>(entity)`                  | `T`                         |
| `upsert(query, update, entityClass)`                   | `upsertSuspending<T>(query, update)`         | `UpdateResult`              |
| `updateFirst(query, update, entityClass)`              | `updateFirstSuspending<T>(query, update)`    | `UpdateResult`              |
| `updateMulti(query, update, entityClass)`              | `updateMultiSuspending<T>(query, update)`    | `UpdateResult`              |
| `remove(query, entityClass)`                           | `removeSuspending<T>(query)`                 | `DeleteResult`              |
| `remove(entity)`                                       | `removeSuspending(entity)`                   | `DeleteResult`              |
| `findAndModify(query, update, entityClass)`            | `findAndModifySuspending<T>(query, update)`  | `T?`                        |
| `findAndRemove(query, entityClass)`                    | `findAndRemoveSuspending<T>(query)`          | `T?`                        |
| `findDistinct(query, field, entityClass, resultClass)` | `findDistinctAsFlow<T, R>(query, field)`     | `Flow<R>`                   |
| `aggregate(aggregation, inputType, outputType)`        | `aggregateAsFlow<I, O>(aggregation)`         | `Flow<O>`                   |
| `tail(query, entityClass)`                             | `tailAsFlow<T>(query)`                       | `Flow<T>` (Tailable cursor) |
| `collectionExists(collectionName)`                     | `collectionExistsSuspending(collectionName)` | `Boolean`                   |
| `dropCollection(entityClass)`                          | `dropCollectionSuspending<T>()`              | `Unit`                      |
| `createCollection(entityClass)`                        | `createCollectionSuspending<T>()`            | `MongoCollection<Document>` |

**구현 패턴 (spring/cassandra 참고):**

```kotlin
// suspend 단건 조회
suspend inline fun <reified T: Any> ReactiveMongoOperations.findOneSuspending(query: Query): T =
    findOne(query, T::class.java).awaitSingle()

// suspend nullable 조회
suspend inline fun <reified T: Any> ReactiveMongoOperations.findOneOrNullSuspending(query: Query): T? =
    findOne(query, T::class.java).awaitSingleOrNull()

// Flow 다건 조회
inline fun <reified T: Any> ReactiveMongoOperations.findAsFlow(query: Query): Flow<T> =
    find(query, T::class.java).asFlow()
```

**Acceptance Criteria:**

- `ReactiveMongoOperations`의 주요 CRUD 메서드가 모두 `suspend`/`Flow` 래핑됨
- 각 함수에 KDoc 한국어 주석 포함 (동작/계약 섹션 및 코드 예제)
- 네이밍이 `spring/cassandra` 패턴과 일관됨 (`xxxSuspending`, `xxxAsFlow`)

---

### Step 3: Criteria/Query infix DSL

**파일 목록:**

- `query/CriteriaExtensions.kt` -- `Criteria` infix 확장
- `query/QueryExtensions.kt` -- `Query` 빌더 확장
- `query/UpdateExtensions.kt` -- `Update` 빌더 확장

**CriteriaExtensions.kt 주요 함수:**

```kotlin
// 비교 연산자
infix fun Criteria.eq(value: Any?): Criteria = `is`(value)
infix fun Criteria.ne(value: Any?): Criteria = ne(value)
infix fun Criteria.gt(value: Any): Criteria = gt(value)
infix fun Criteria.gte(value: Any): Criteria = gte(value)
infix fun Criteria.lt(value: Any): Criteria = lt(value)
infix fun Criteria.lte(value: Any): Criteria = lte(value)

// 컬렉션 연산자
infix fun Criteria.inValues(values: Collection<*>): Criteria = `in`(values)
infix fun Criteria.notInValues(values: Collection<*>): Criteria = nin(values)

// 문자열 연산자
infix fun Criteria.regex(pattern: String): Criteria = regex(pattern)
infix fun Criteria.regex(pattern: Regex): Criteria = regex(pattern.toPattern())

// Null/존재 확인
fun Criteria.isNull(): Criteria = `is`(null)
fun Criteria.exists(): Criteria = exists(true)
fun Criteria.notExists(): Criteria = exists(false)

// 배열 연산자
infix fun Criteria.allValues(values: Collection<*>): Criteria = all(values)
infix fun Criteria.size(size: Int): Criteria = size(size)
infix fun Criteria.elemMatch(criteria: Criteria): Criteria = elemMatch(criteria)

// 논리 연산자 DSL
infix fun Criteria.andOperator(criteria: Criteria): Criteria = andOperator(criteria)
infix fun Criteria.orOperator(criteria: Criteria): Criteria = orOperator(criteria)

// 편의 함수: String에서 바로 Criteria 시작
fun String.criteria(): Criteria = Criteria.where(this)
```

**QueryExtensions.kt:**

```kotlin
// Query 빌더 DSL
fun query(vararg criteria: CriteriaDefinition): Query = Query(Criteria().andOperator(*criteria))

fun Query.sortBy(vararg orders: Sort.Order): Query = with(Sort.by(*orders))
fun Query.limitTo(limit: Int): Query = limit(limit)
fun Query.skipTo(skip: Long): Query = skip(skip)
```

**UpdateExtensions.kt:**

```kotlin
// Update 빌더 infix DSL
infix fun String.setTo(value: Any?): Update = Update.update(this, value)
fun Update.and(field: String, value: Any?): Update = set(field, value)

infix fun String.incBy(value: Number): Update = Update().inc(this, value)
infix fun String.unsetField(dummy: Unit): Update = Update().unset(this)

fun updateOf(vararg pairs: Pair<String, Any?>): Update {
    val update = Update()
    pairs.forEach { (field, value) -> update.set(field, value) }
    return update
}
```

**Acceptance Criteria:**

- `"field" eq value`, `"field" gt value`, `"field" regex "pattern"` 형태로 사용 가능
- `Criteria.where("field")` 대신 `"field".criteria()` 단축 사용 가능
- 모든 연산자에 KDoc 한국어 주석 포함
- 단위 테스트에서 생성된 Criteria가 Spring Data MongoDB 표준 Criteria와 동일함을 검증

---

### Step 4: Auto-configuration 및 설정 지원

**파일 목록:**

- `config/ReactiveMongoAutoConfiguration.kt`
- `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**ReactiveMongoAutoConfiguration.kt:**

```kotlin
@AutoConfiguration
@ConditionalOnClass(ReactiveMongoOperations::class)
@ConditionalOnBean(ReactiveMongoOperations::class)
class ReactiveMongoAutoConfiguration {
    // ReactiveMongoTemplate 설정 커스터마이징이 필요한 경우 Bean 등록
    // 기본적으로 spring-boot-starter-data-mongodb-reactive가 자동 설정하므로
    // 여기서는 추가 커스터마이징만 제공
}
```

**AutoConfiguration.imports:**

```
io.bluetape4k.spring.mongodb.config.ReactiveMongoAutoConfiguration
```

**Acceptance Criteria:**

- `spring-boot-starter-data-mongodb-reactive` 의존성만으로 `ReactiveMongoOperations` Bean이 자동 구성됨
- Auto-configuration이 Spring Boot 3.4+의 `AutoConfiguration.imports` 방식을 사용

---

### Step 5: 테스트 인프라 및 통합 테스트

**파일 목록:**

- `AbstractReactiveMongoTest.kt` -- 테스트 베이스 클래스
- `AbstractReactiveMongoCoroutineTest.kt` -- Coroutine 테스트 베이스 클래스
- `MongoTestConfiguration.kt` -- Testcontainers MongoDB 설정
- `model/User.kt` -- 테스트 도메인 모델
- `model/Product.kt` -- 테스트 도메인 모델 (다양한 필드 타입)
- `coroutines/ReactiveMongoOperationsCoroutinesTest.kt` -- CRUD 확장함수 테스트
- `query/CriteriaExtensionsTest.kt` -- Criteria DSL 테스트
- `query/QueryExtensionsTest.kt` -- Query DSL 테스트
- `query/UpdateExtensionsTest.kt` -- Update DSL 테스트

**AbstractReactiveMongoCoroutineTest.kt 패턴 (spring/cassandra 참고):**

```kotlin
abstract class AbstractReactiveMongoCoroutineTest(
    private val coroutineName: String = "mongodb",
): AbstractReactiveMongoTest(),
   CoroutineScope by CoroutineScope(Dispatchers.IO + CoroutineName(coroutineName)) {
    companion object: KLoggingChannel()
}
```

**AbstractReactiveMongoTest.kt:**

```kotlin
abstract class AbstractReactiveMongoTest {
    companion object: KLoggingChannel() {
        val faker = Fakers.faker
    }

    @Autowired
    protected lateinit var mongoOperations: ReactiveMongoOperations
}
```

**MongoTestConfiguration.kt:**

```kotlin
@TestConfiguration(proxyBeanMethods = false)
class MongoTestConfiguration {
    // Testcontainers MongoDB 설정
    // MongoDBServer.Launcher 사용 (bluetape4k-testcontainers)
}
```

**주요 테스트 시나리오 (coroutines/ReactiveMongoOperationsCoroutinesTest.kt):**

- insert/save suspend 테스트
- findById suspend 테스트
- findAsFlow 다건 조회 테스트
- countSuspending / existsSuspending 테스트
- updateFirstSuspending / updateMultiSuspending 테스트
- removeSuspending 테스트
- findAndModifySuspending 테스트
- aggregateAsFlow 집계 테스트
- Criteria infix DSL을 활용한 조건부 조회 테스트

**Acceptance Criteria:**

- `./gradlew :bluetape4k-spring-mongodb:test` 전체 테스트 통과
- Testcontainers MongoDB가 자동으로 시작/종료됨
- suspend 함수 테스트는 `runSuspendIO` 사용
- `@BeforeEach`에서 `runBlocking`으로 데이터 초기화

---

## Success Criteria

1. `./gradlew :bluetape4k-spring-mongodb:build` 성공
2. 모든 `ReactiveMongoOperations` 주요 CRUD 메서드가 `suspend`/`Flow` 래핑 제공
3. Criteria infix DSL이 `eq`, `gt`, `lt`, `gte`, `lte`, `ne`, `regex`, `inValues` 등 주요 연산자 지원
4. 통합 테스트가 Testcontainers MongoDB 환경에서 전체 통과
5. 모든 public API에 KDoc 한국어 주석 포함
6. `spring/cassandra` 모듈과 일관된 네이밍 및 구조
