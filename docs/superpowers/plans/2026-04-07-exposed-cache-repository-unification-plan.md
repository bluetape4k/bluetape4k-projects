# Exposed Cache Repository 인터페이스 통일 및 테스트 표준화 구현 플랜

**작성일**: 2026-04-07
**스펙**: `docs/superpowers/specs/2026-04-07-exposed-cache-repository-unification-design.md`
**레퍼런스**: `javax.cache.Cache` API 관례, 기존 4개 Cache Repository 모듈

---

## 소스 참조

- `data/exposed-jdbc-lettuce/` -- Lettuce JDBC 캐시 Repository (인터페이스 + Abstract + 테스트)
- `data/exposed-jdbc-redisson/` -- Redisson JDBC 캐시 Repository (인터페이스 + Abstract + 테스트)
- `data/exposed-r2dbc-lettuce/` -- Lettuce R2DBC 캐시 Repository
- `data/exposed-r2dbc-redisson/` -- Redisson R2DBC 캐시 Repository
- `buildSrc/Libs.kt` -- 의존성 버전 관리
- `settings.gradle.kts` -- `includeModules("data", withBaseDir = false)` 자동 등록

---

## 태스크 요약

| Complexity | 태스크 수 | 설명 |
|---|---|---|
| **low** | 1 | T0: 외부 참조 영향 분석 (선행) |
| **high** | 1 | T1: exposed-redis-api 신규 모듈 (인터페이스 설계 + testFixtures) |
| **medium** | 6 | T2-T5: 4개 구현 모듈 마이그레이션, T6-T7: 테스트 표준화 |
| **low** | 1 | T8: README 및 KDoc, CLAUDE.md 업데이트 + bluetape4k-patterns 체크리스트 |
| **high** | 1 | T9: 스펙 대비 구현 검증 (verifier) |
| **합계** | **10** | |

## 병렬 실행 그룹

```
Group 0 (선행): Task 0 (외부 참조 영향 분석)
    |
Group A (선행): Task 1 (exposed-redis-api 모듈 생성 + 인터페이스 + testFixtures)
    |
Group B (병렬): Task 2 (jdbc-lettuce) | Task 3 (jdbc-redisson) | Task 4 (r2dbc-lettuce) | Task 5 (r2dbc-redisson)
    |
Group C (병렬): Task 6 (JDBC 테스트 표준화)  |  Task 7 (R2DBC 테스트 표준화)
    |
Group D: Task 8 (README / KDoc / CLAUDE.md)
```

---

## Task 0: 외부 참조 영향 분석 [complexity: low]

인터페이스 변경 전 기존 인터페이스를 외부 모듈에서 참조하는지 확인합니다.

### 0-1. 참조 탐색

다음 인터페이스가 `data/` 디렉터리 외부에서 직접 사용되는지 확인:
- `JdbcLettuceRepository` / `SuspendedJdbcLettuceRepository`
- `JdbcRedissonRepository` / `SuspendedJdbcRedissonRepository`
- `R2dbcLettuceRepository`
- `R2dbcRedissonRepository`

**탐색 방법:**
```bash
# ide_find_references 또는 grep으로 확인
rg "JdbcLettuceRepository|SuspendedJdbcLettuceRepository|JdbcRedissonRepository|SuspendedJdbcRedissonRepository|R2dbcLettuceRepository|R2dbcRedissonRepository" \
  --type kotlin \
  --include-glob "!data/exposed-jdbc-lettuce/**" \
  --include-glob "!data/exposed-jdbc-redisson/**" \
  --include-glob "!data/exposed-r2dbc-lettuce/**" \
  --include-glob "!data/exposed-r2dbc-redisson/**"
```

### 0-2. 확인 대상 모듈

- `spring-boot3/exposed-jdbc/`, `spring-boot3/exposed-r2dbc/`
- `spring-boot4/exposed-jdbc/`, `spring-boot4/exposed-r2dbc/`
- `examples/`, `workshop/` 등 데모 모듈

### 0-3. 외부 참조 발견 시 처리

외부 참조가 존재하면 Task 2–5 진행 전에 해당 모듈도 함께 수정 계획에 포함.
외부 참조가 없으면 그대로 진행.

### AC
- 탐색 결과를 간략히 기록 (참조 없음 확인 또는 외부 참조 목록 명시)
- 외부 참조 발견 시 Task 2–5 AC에 해당 파일 수정 포함

---

## Task 1: `exposed-redis-api` 신규 모듈 생성 [complexity: high]

이 모듈은 4개 Cache Repository 구현체가 공유하는 공통 인터페이스와 테스트 인프라를 제공합니다.

### 1-1. 모듈 초기화

**생성할 파일:**
- `data/exposed-redis-api/build.gradle.kts`

```kotlin
plugins {
    `java-test-fixtures`
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // 인터페이스 정의에 필요한 최소 의존성
    api(Libs.exposed_core)

    // testFixtures -- 공통 테스트 시나리오
    testFixturesApi(project(":bluetape4k-junit5"))
    testFixturesApi(Libs.kluent)
    testFixturesApi(Libs.kotlinx_coroutines_core)
    testFixturesApi(Libs.kotlinx_coroutines_test)
}
```

- `settings.gradle.kts` 변경 불필요 (`includeModules("data")` 자동 등록)

### 1-2. 공통 열거형

**생성할 파일:**
- `data/exposed-redis-api/src/main/kotlin/io/bluetape4k/exposed/redis/CacheMode.kt`
  - `enum class CacheMode { READ_ONLY, READ_WRITE }`
- `data/exposed-redis-api/src/main/kotlin/io/bluetape4k/exposed/redis/CacheWriteMode.kt`
  - `enum class CacheWriteMode { NONE, WRITE_THROUGH, WRITE_BEHIND }`

### 1-3. 공통 인터페이스 (3개)

**생성할 파일:**
- `data/exposed-redis-api/src/main/kotlin/io/bluetape4k/exposed/redis/repository/JdbcCacheRepository.kt`
  - `interface JdbcCacheRepository<ID: Any, E: Serializable> : Closeable`
  - 프로퍼티: `table`, `cacheName`, `cacheMode`, `cacheWriteMode`
  - 메서드: `ResultRow.toEntity()`, `extractId()`, `findByIdFromDb()`, `findAllFromDb()`, `countFromDb()`, `containsKey()`, `get()`, `getAll()`, `findAll()`, `put()`, `putAll()`, `invalidate()`, `invalidateAll()`, `clear()`, `close()`
  - `companion object { const val DEFAULT_BATCH_SIZE = 500 }`

- `data/exposed-redis-api/src/main/kotlin/io/bluetape4k/exposed/redis/repository/SuspendedJdbcCacheRepository.kt`
  - `interface SuspendedJdbcCacheRepository<ID: Any, E: Serializable> : Closeable`
  - `JdbcCacheRepository`와 동일 메서드 시그니처, 모든 메서드가 `suspend`
  - `toEntity()`는 non-suspend (JDBC `ResultRow` 처리)

- `data/exposed-redis-api/src/main/kotlin/io/bluetape4k/exposed/redis/repository/R2dbcCacheRepository.kt`
  - `interface R2dbcCacheRepository<ID: Any, E: Serializable> : Closeable`
  - `SuspendedJdbcCacheRepository`와 동일 메서드, `toEntity()`가 `suspend`
  - DB 접근은 R2DBC `suspendTransaction` 사용

### 1-4. Redisson 전용 확장 인터페이스 (2개)

**생성할 파일:**
- `data/exposed-redis-api/src/main/kotlin/io/bluetape4k/exposed/redis/repository/JdbcRedissonCacheRepository.kt`
  - `interface JdbcRedissonCacheRepository<ID: Any, E: Serializable> : JdbcCacheRepository<ID, E>`
  - 추가 메서드: `fun invalidateByPattern(pattern: String, count: Int = DEFAULT_BATCH_SIZE): Long`

- `data/exposed-redis-api/src/main/kotlin/io/bluetape4k/exposed/redis/repository/R2dbcRedissonCacheRepository.kt`
  - `interface R2dbcRedissonCacheRepository<ID: Any, E: Serializable> : R2dbcCacheRepository<ID, E>`
  - 추가 메서드: `suspend fun invalidateByPattern(pattern: String, count: Int = DEFAULT_BATCH_SIZE): Long`

### 1-5. testFixtures: 공통 테스트 시나리오 (12개)

**패키지**: `io.bluetape4k.exposed.redis.test`

**JDBC 동기 시나리오 (4개):**
- `data/exposed-redis-api/src/testFixtures/kotlin/io/bluetape4k/exposed/redis/test/CacheTestScenario.kt`
  - `repository: JdbcCacheRepository<ID, E>` 참조
  - `getExistingId()`, `getExistingIds()`, `getNonExistentId()`, `buildEntityForId()`, `mutateEntity()`, `createNewEntity()`
  - `@BeforeEach clearCacheBeforeEach()` -- `repository.clear()`
- `data/exposed-redis-api/src/testFixtures/kotlin/io/bluetape4k/exposed/redis/test/ReadThroughScenario.kt`
  - 11개 테스트 메서드: `get` 미스 시 RT, `get` null, `containsKey`, `getAll`, `findAll(where)`, `clear`, `invalidate`, `invalidateAll`, `countFromDb` 등
- `data/exposed-redis-api/src/testFixtures/kotlin/io/bluetape4k/exposed/redis/test/ReadWriteThroughScenario.kt`
  - 3개 테스트: `put` 동시반영, `putAll` 동시반영, `invalidate` 캐시만 제거
- `data/exposed-redis-api/src/testFixtures/kotlin/io/bluetape4k/exposed/redis/test/WriteBehindScenario.kt`
  - 4개 테스트: `put` 캐시 즉시, `put` DB 비동기, `putAll` 배치, 대량 신규 추가
  - `awaitDbReflection()` 유틸리티 포함

**JDBC Suspend 시나리오 (4개):**
- `data/exposed-redis-api/src/testFixtures/kotlin/io/bluetape4k/exposed/redis/test/SuspendedCacheTestScenario.kt`
  - `repository: SuspendedJdbcCacheRepository<ID, E>` 참조
  - 동기 버전과 동일한 abstract 메서드 + `@BeforeEach`에서 `runTest { repository.clear() }`
- `data/exposed-redis-api/src/testFixtures/kotlin/io/bluetape4k/exposed/redis/test/SuspendedReadThroughScenario.kt`
- `data/exposed-redis-api/src/testFixtures/kotlin/io/bluetape4k/exposed/redis/test/SuspendedReadWriteThroughScenario.kt`
- `data/exposed-redis-api/src/testFixtures/kotlin/io/bluetape4k/exposed/redis/test/SuspendedWriteBehindScenario.kt`

**R2DBC Suspend 시나리오 (4개):**
- `data/exposed-redis-api/src/testFixtures/kotlin/io/bluetape4k/exposed/redis/test/R2dbcCacheTestScenario.kt`
  - `repository: R2dbcCacheRepository<ID, E>` 참조
- `data/exposed-redis-api/src/testFixtures/kotlin/io/bluetape4k/exposed/redis/test/R2dbcReadThroughScenario.kt`
- `data/exposed-redis-api/src/testFixtures/kotlin/io/bluetape4k/exposed/redis/test/R2dbcReadWriteThroughScenario.kt`
- `data/exposed-redis-api/src/testFixtures/kotlin/io/bluetape4k/exposed/redis/test/R2dbcWriteBehindScenario.kt`

### AC (완료 기준)
- `./gradlew :bluetape4k-exposed-redis-api:build` 성공
- 인터페이스 5개 + 열거형 2개 + testFixtures 시나리오 12개 = 총 19개 파일
- `JdbcCacheRepository` / `SuspendedJdbcCacheRepository` / `R2dbcCacheRepository` 메서드 시그니처가 스펙 섹션 4.2, 4.3, 5.1과 일치
- `JdbcRedissonCacheRepository` / `R2dbcRedissonCacheRepository`에만 `invalidateByPattern` 포함 (LSP 준수)

---

## Task 2: `exposed-jdbc-lettuce` 모듈 수정 [complexity: medium]

기존 `JdbcLettuceRepository` / `SuspendedJdbcLettuceRepository` 인터페이스를 제거하고, Abstract 클래스가 공통 인터페이스를 직접 구현합니다.

### 2-1. build.gradle.kts 변경

**수정할 파일:** `data/exposed-jdbc-lettuce/build.gradle.kts`
- `api(project(":bluetape4k-exposed-redis-api"))` 추가
- `testImplementation(testFixtures(project(":bluetape4k-exposed-redis-api")))` 추가

### 2-2. 인터페이스 파일 삭제

**삭제할 파일:**
- `data/exposed-jdbc-lettuce/src/main/kotlin/io/bluetape4k/exposed/lettuce/repository/JdbcLettuceRepository.kt`
- `data/exposed-jdbc-lettuce/src/main/kotlin/io/bluetape4k/exposed/lettuce/repository/SuspendedJdbcLettuceRepository.kt`

### 2-3. Abstract 클래스 수정 (동기)

**수정할 파일:** `data/exposed-jdbc-lettuce/src/main/kotlin/io/bluetape4k/exposed/lettuce/repository/AbstractJdbcLettuceRepository.kt`

주요 변경:
- `JdbcLettuceRepository<ID, E>` 상속 -> `JdbcCacheRepository<ID, E>` 상속
- 제네릭 바운드: `E: Any` -> `E: Serializable`
- `cacheName` 프로퍼티 추가: `override val cacheName get() = config.keyPrefix`
- `cacheMode` 프로퍼티 추가: `WriteMode.NONE -> CacheMode.READ_ONLY, else -> CacheMode.READ_WRITE`
- `cacheWriteMode` 프로퍼티 추가: `WriteMode.NONE -> NONE, WRITE_THROUGH -> WRITE_THROUGH, WRITE_BEHIND -> WRITE_BEHIND`
- `containsKey(id)` 신규 구현: `cache[id] != null`
- `ResultRow.toEntity()` -- `abstract override` (인터페이스 메서드로 승격)
- `extractId(entity)` -- `abstract override` (protected -> public override)
- 메서드명 변경:
  - `findById(id)` -> `get(id)`
  - `findAll(ids)` -> `getAll(ids): Map<ID, E>`
  - `save(id, entity)` -> `put(id, entity)`
  - `saveAll(entities)` -> `putAll(entities, batchSize)` (batchSize 파라미터 추가, Lettuce 내부에서는 무시 가능)
  - `delete(id)` -> `invalidate(id)`
  - `deleteAll(ids)` -> `invalidateAll(ids)`
  - `clearCache()` -> `clear()`

### 2-4. Abstract 클래스 수정 (Suspend)

**수정할 파일:** `data/exposed-jdbc-lettuce/src/main/kotlin/io/bluetape4k/exposed/lettuce/repository/AbstractSuspendedJdbcLettuceRepository.kt`

주요 변경:
- `SuspendedJdbcLettuceRepository<ID, E>` -> `SuspendedJdbcCacheRepository<ID, E>`
- `E: Any` -> `E: Serializable`
- 동기 버전과 동일한 프로퍼티 추가 (`cacheName`, `cacheMode`, `cacheWriteMode`)
- `containsKey(id)` 신규: nearCache 우선 확인 후 cache 확인
- `extractId(entity)` -- `abstract override` (protected -> public override)
- 메서드명 변경 (동기 버전과 동일 매핑):
  - `findById` -> `get`, `findAll(ids)` -> `getAll`, `save` -> `put`, `saveAll` -> `putAll`, `delete` -> `invalidate`, `deleteAll` -> `invalidateAll`, `clearCache` -> `clear`

### 2-5. 테스트 코드 수정

**수정할 파일들 (테스트 참조 변경):**
- `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/repository/scenarios/CacheTestScenario.kt`
  - `JdbcLettuceRepository` -> `JdbcCacheRepository` 참조 변경
  - `clearCache()` -> `clear()`
- `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/repository/scenarios/ReadThroughScenario.kt`
  - `findById` -> `get`, `findAll(ids)` -> `getAll(ids)` 등 메서드명 변경
- `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/repository/scenarios/WriteThroughScenario.kt`
  - `save` -> `put`, `saveAll` -> `putAll` 등
- `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/repository/scenarios/WriteBehindScenario.kt`
  - 동일 메서드명 변경
- `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/repository/scenarios/SuspendedCacheTestScenario.kt`
  - `SuspendedJdbcLettuceRepository` -> `SuspendedJdbcCacheRepository`
- `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/repository/scenarios/SuspendedReadThroughScenario.kt`
- `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/repository/scenarios/SuspendedWriteThroughScenario.kt`
- `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/repository/scenarios/SuspendedWriteBehindScenario.kt`
- 테스트 Repository 클래스들 (`UserRepository.kt`, `SuspendedUserRepository.kt` 등)의 상위 타입 변경
- 모든 테스트 클래스에서 호출하는 기존 메서드명 일괄 교체

### AC
- `./gradlew :bluetape4k-exposed-jdbc-lettuce:build` 성공
- 기존 `JdbcLettuceRepository`, `SuspendedJdbcLettuceRepository` 인터페이스 파일 삭제 완료
- Abstract 클래스가 `JdbcCacheRepository` / `SuspendedJdbcCacheRepository` 직접 구현
- 모든 기존 테스트 통과 (메서드명 변경 반영)

---

## Task 3: `exposed-jdbc-redisson` 모듈 수정 [complexity: medium]

기존 `JdbcRedissonRepository` / `SuspendedJdbcRedissonRepository` 인터페이스를 제거하고, Abstract 클래스가 Redisson 전용 확장 인터페이스를 구현합니다.

### 3-1. build.gradle.kts 변경

**수정할 파일:** `data/exposed-jdbc-redisson/build.gradle.kts`
- `api(project(":bluetape4k-exposed-redis-api"))` 추가
- `testImplementation(testFixtures(project(":bluetape4k-exposed-redis-api")))` 추가

### 3-2. 인터페이스 파일 삭제

**삭제할 파일:**
- `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/repository/JdbcRedissonRepository.kt`
- `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/repository/SuspendedJdbcRedissonRepository.kt`

### 3-3. Abstract 클래스 수정 (동기)

**수정할 파일:** `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/repository/AbstractJdbcRedissonRepository.kt`

주요 변경:
- `JdbcRedissonRepository<ID, E>` -> `JdbcRedissonCacheRepository<ID, E>` 상속
- `E: Any` -> `E: Serializable`
- `cacheMode` 프로퍼티 추가: `RedissonCacheConfig.CacheMode` -> `CacheMode` 변환
- `cacheWriteMode` 프로퍼티 추가: `isReadOnly -> NONE`, `isWriteBehind -> WRITE_BEHIND`, `else -> WRITE_THROUGH`
- `containsKey(id)` 신규 구현: `cache.containsKey(id)`
- `getAll(ids): Map<ID, E>` 신규 구현 (기존 `getAll(ids, batchSize): List<E>`를 대체)
  - 공통 인터페이스는 `Map<ID, E>` 반환, `batchSize` 파라미터 제거
  - 내부 구현에서 `DEFAULT_BATCH_SIZE` 상수 사용 (`JdbcCacheRepository.DEFAULT_BATCH_SIZE`)
- `countFromDb()` 신규 구현: `transaction { table.selectAll().count() }`
- `put(id, entity)` 구현: `cache.fastPut(id, entity)`
- `putAll(entities: Map<ID, E>, batchSize)` 구현: `cache.putAll(entities, batchSize)`
- `invalidate(id)` 구현: `cache.fastRemove(id)` (기존 `vararg` -> 단건)
- `invalidateAll(ids: Collection<ID>)` 구현: `cache.fastRemove(*ids.toTypedArray())`
- `clear()` 구현: `cache.clear()`
- `invalidateByPattern(pattern, count)` 구현 유지 (Redisson 전용)
- `close()` 구현: no-op (Redisson client는 외부 관리)
- 기존 `exists(id)` 기본 구현 제거 (인터페이스의 `containsKey`로 대체)
- `put(id: ID, entity: E)` 시그니처로 변경 (`put(entity: E)` 제거)
- `findAllFromDb(vararg ids: ID)` vararg 오버로드 제거:
  - 기존 Redisson은 `findAllFromDb(vararg ids: ID)` + `findAllFromDb(ids: Collection<ID>)` 두 오버로드가 존재
  - 새 인터페이스는 `findAllFromDb(ids: Collection<ID>)` 하나만 유지
  - 기존 vararg 호출 (`findAllFromDb(id1, id2, id3)`) → `findAllFromDb(listOf(id1, id2, id3))` 또는 `findAllFromDb(ids.toList())`로 변환

### 3-4. Abstract 클래스 수정 (Suspend)

**수정할 파일:** `data/exposed-jdbc-redisson/src/main/kotlin/io/bluetape4k/exposed/redisson/repository/AbstractSuspendedJdbcRedissonRepository.kt`

주요 변경:
- `SuspendedJdbcRedissonRepository<ID, E>` -> `SuspendedJdbcCacheRepository<ID, E>` 상속
  - 참고: Redisson Suspend JDBC는 `invalidateByPattern`이 인터페이스에 없으므로 `SuspendedJdbcCacheRepository` 사용
  - `invalidateByPattern`은 Abstract 클래스 자체 메서드로 유지 (기존 테스트에서 직접 참조)
- `E: Any` -> `E: Serializable`
- 동기 버전과 동일한 프로퍼티 추가 (`cacheName`, `cacheMode`, `cacheWriteMode`)
- `containsKey(id)` suspend: `cache.containsKeyAsync(id).await()`
- `get(id)` suspend: `cache.getAsync(id).await()`
- `getAll(ids): Map<ID, E>` suspend: batch chunked + `cache.getAllAsync(chunk.toSet()).await()`
  - batchSize 파라미터 제거, 내부에서 `DEFAULT_BATCH_SIZE` 사용
- `countFromDb()` suspend: `suspendedTransactionAsync(IO) { table.selectAll().count() }.await()`
- `put(id: ID, entity: E)` suspend: `cache.fastPutAsync(id, entity).await()` (`put(entity)` 제거)
- `putAll(entities: Map<ID, E>, batchSize)` suspend
- `invalidate(id)` suspend: `cache.fastRemoveAsync(id).await()`
- `invalidateAll(ids)` suspend
- `clear()` suspend: `cache.clearAsync().await()`
- 기존 `exists`, `get`, `put(entity)` -> `put(id, entity)`, `invalidate(vararg)`, `invalidateAll(): Boolean`, `invalidateByPattern` 시그니처 변경

### 3-5. 테스트 코드 수정

**수정할 파일들:**
- `data/exposed-jdbc-redisson/src/test/kotlin/io/bluetape4k/exposed/redisson/repository/scenarios/CacheTestScenario.kt`
  - `JdbcRedissonRepository` -> `JdbcRedissonCacheRepository` (또는 `JdbcCacheRepository`)
  - `invalidateAll()` -> `clear()` (BeforeEach)
  - `cacheConfig` 프로퍼티 유지 (구현 모듈 고유)
- `data/exposed-jdbc-redisson/src/test/kotlin/io/bluetape4k/exposed/redisson/repository/scenarios/ReadThroughScenario.kt`
  - `exists(id)` -> `containsKey(id)`
  - `invalidate(*ids)` -> `invalidateAll(ids)`
  - `getAll(ids, batchSize)` -> `getAll(ids)` (batchSize 파라미터 제거, Map 반환으로 변경, size 비교 조정)
  - `invalidateByPattern` -- Redisson 전용이므로 repository를 `JdbcRedissonCacheRepository`로 캐스팅하거나 별도 테스트로 분리
- `data/exposed-jdbc-redisson/src/test/kotlin/io/bluetape4k/exposed/redisson/repository/scenarios/WriteThroughScenario.kt`
  - `put(entity)` -> `put(extractId(entity), entity)` (id를 명시적으로 전달)
  - `putAll(entities, batchSize)` -> `putAll(entities.associateBy { extractId(it) }, batchSize)`
- `data/exposed-jdbc-redisson/src/test/kotlin/io/bluetape4k/exposed/redisson/repository/scenarios/WriteBehindScenario.kt`
  - `put(entity)` -> `put(extractId(entity), entity)` 동일 변경
  - `getAll(ids, batchSize)` -> `getAll(ids)` 동일 변경
- Suspended 시나리오 6개도 동일 변경
- 테스트 Repository 클래스들 (`UserCacheRepository.kt`, `UserCredentialCacheRepository.kt`, `SuspendedUserCacheRepository.kt` 등) 상위 타입 변경

### AC
- `./gradlew :bluetape4k-exposed-jdbc-redisson:build` 성공
- 기존 `JdbcRedissonRepository`, `SuspendedJdbcRedissonRepository` 인터페이스 파일 삭제 완료
- Abstract 클래스가 `JdbcRedissonCacheRepository` / `SuspendedJdbcCacheRepository` 직접 구현
- 모든 기존 테스트 통과

---

## Task 4: `exposed-r2dbc-lettuce` 모듈 수정 [complexity: medium]

기존 `R2dbcLettuceRepository` 인터페이스를 제거하고, Abstract 클래스가 `R2dbcCacheRepository`를 직접 구현합니다.

### 4-1. build.gradle.kts 변경

**수정할 파일:** `data/exposed-r2dbc-lettuce/build.gradle.kts`
- `api(project(":bluetape4k-exposed-redis-api"))` 추가
- `testImplementation(testFixtures(project(":bluetape4k-exposed-redis-api")))` 추가

### 4-2. 인터페이스 파일 삭제

**삭제할 파일:**
- `data/exposed-r2dbc-lettuce/src/main/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/repository/R2dbcLettuceRepository.kt`

### 4-3. Abstract 클래스 수정

**수정할 파일:** `data/exposed-r2dbc-lettuce/src/main/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/repository/AbstractR2dbcLettuceRepository.kt`

주요 변경:
- `R2dbcLettuceRepository<ID, E>` -> `R2dbcCacheRepository<ID, E>` 상속
- `E: Any` -> `E: Serializable`
- `cacheName`, `cacheMode`, `cacheWriteMode` 프로퍼티 추가 (T2와 동일 로직)
- `containsKey(id)` suspend 신규: nearCache 우선 확인 후 cache 확인
- `extractId(entity)` -- `abstract override` (protected -> public override)
- `ResultRow.toEntity()` -- `abstract suspend override`
- 메서드명 변경:
  - `findById` -> `get`, `findAll(ids)` -> `getAll`, `save` -> `put`, `saveAll` -> `putAll`, `delete` -> `invalidate`, `deleteAll` -> `invalidateAll`, `clearCache` -> `clear`

### 4-4. 테스트 코드 수정

**수정할 파일들:**
- `data/exposed-r2dbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/repository/scenarios/R2DbcLettuceJCacheTestScenario.kt`
  - `R2dbcLettuceRepository` -> `R2dbcCacheRepository`
- `data/exposed-r2dbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/repository/scenarios/R2dbcLettuceReadThroughScenario.kt`
- `data/exposed-r2dbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/repository/scenarios/R2dbcLettuceWriteThroughScenario.kt`
- `data/exposed-r2dbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/repository/scenarios/R2dbcLettuceWriteBehindScenario.kt`
- 테스트 Repository 클래스들 상위 타입 변경
- 메서드명 일괄 변경

### AC
- `./gradlew :bluetape4k-exposed-r2dbc-lettuce:build` 성공
- 기존 `R2dbcLettuceRepository` 인터페이스 파일 삭제 완료
- Abstract 클래스가 `R2dbcCacheRepository` 직접 구현

---

## Task 5: `exposed-r2dbc-redisson` 모듈 수정 [complexity: medium]

기존 `R2dbcRedissonRepository` 인터페이스를 제거하고, Abstract 클래스가 `R2dbcRedissonCacheRepository`를 직접 구현합니다.

### 5-1. build.gradle.kts 변경

**수정할 파일:** `data/exposed-r2dbc-redisson/build.gradle.kts`
- `api(project(":bluetape4k-exposed-redis-api"))` 추가
- `testImplementation(testFixtures(project(":bluetape4k-exposed-redis-api")))` 추가

### 5-2. 인터페이스 파일 삭제

**삭제할 파일:**
- `data/exposed-r2dbc-redisson/src/main/kotlin/io/bluetape4k/exposed/r2dbc/redisson/repository/R2dbcRedissonRepository.kt`

### 5-3. Abstract 클래스 수정

**수정할 파일:** `data/exposed-r2dbc-redisson/src/main/kotlin/io/bluetape4k/exposed/r2dbc/redisson/repository/AbstractR2dbcRedissonRepository.kt`

주요 변경:
- `R2dbcRedissonRepository<ID, E>` -> `R2dbcRedissonCacheRepository<ID, E>` 상속
- `E: Any` -> `E: Serializable`
- `cacheMode`, `cacheWriteMode` 프로퍼티 추가 (T3와 동일 변환 로직)
- `containsKey(id)` suspend: `cache.containsKeyAsync(id).await()`
- `get(id)` suspend: `cache.getAsync(id).await()`
- `getAll(ids): Map<ID, E>` suspend -- batch chunked + `cache.getAllAsync`
  - batchSize 파라미터 제거, 내부에서 `DEFAULT_BATCH_SIZE` 사용
- `countFromDb()` suspend: `suspendTransaction { table.selectAll().count() }`
- `suspend fun put(id: ID, entity: E)` 시그니처로 변경 (`suspend fun put(entity: E)` 제거)
  - `cache.fastPutAsync(id, entity).await()` 구현
- `putAll(entities: Map<ID, E>, batchSize)` suspend
- `invalidate(id)` suspend, `invalidateAll(ids)` suspend
- `clear()` suspend: `cache.clearAsync().await()`
- `invalidateByPattern(pattern, count)` suspend 유지 (Redisson 전용)
- 기존 `exists(id)`, `get(id)`, `put(entity)` -> `put(id, entity)`, `invalidate(vararg)`, `invalidateAll(): Boolean` 시그니처 변경
- `ResultRow.toEntity()` -- `abstract suspend override`

### 5-4. 테스트 코드 수정

**수정할 파일들:**
- `data/exposed-r2dbc-redisson/src/test/kotlin/io/bluetape4k/exposed/r2dbc/redisson/repository/scenario/R2dbcCacheTestScenario.kt`
  - `R2dbcRedissonRepository` -> `R2dbcRedissonCacheRepository`
- `data/exposed-r2dbc-redisson/src/test/kotlin/io/bluetape4k/exposed/r2dbc/redisson/repository/scenario/R2dbcReadThroughScenario.kt`
- `data/exposed-r2dbc-redisson/src/test/kotlin/io/bluetape4k/exposed/r2dbc/redisson/repository/scenario/R2dbcWriteThroughScenario.kt`
- `data/exposed-r2dbc-redisson/src/test/kotlin/io/bluetape4k/exposed/r2dbc/redisson/repository/scenario/R2dbcWriteBehindScenario.kt`
- 테스트 Repository 클래스들 상위 타입 변경
- 메서드명 일괄 변경:
  - `exists` -> `containsKey`
  - `suspend fun put(entity)` -> `suspend fun put(extractId(entity), entity)` (id를 명시적으로 전달)
  - `invalidate(vararg)` -> `invalidate(id)` / `invalidateAll(ids)`
  - `invalidateAll()` -> `clear()`
  - `getAll(ids, batchSize)` -> `getAll(ids)` (batchSize 파라미터 제거)

### AC
- `./gradlew :bluetape4k-exposed-r2dbc-redisson:build` 성공
- 기존 `R2dbcRedissonRepository` 인터페이스 파일 삭제 완료
- Abstract 클래스가 `R2dbcRedissonCacheRepository` 직접 구현

---

## Task 6: JDBC 모듈 테스트 표준화 [complexity: medium]

T2+T3 완료 후 수행. 스펙 섹션 7.6의 누락 분석표 기준으로 빠진 테스트 조합을 추가합니다.

### 6-1. exposed-jdbc-lettuce 누락 테스트 추가

현재 누락 항목:
- AutoInc + NearCache (RT / RWT / WB) -- 동기 + suspend
- ClientGenerated ID 전체 (Remote + NearCache, RT / RWT / WB) -- 동기 + suspend
- `containsKey()` 테스트
- `countFromDb()` 테스트

**생성/수정할 파일:**
- `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/domain/UserSchema.kt` -- UserCredentialTable (ClientGenerated ID용) 추가 (없는 경우)
- `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/domain/UserCredentialRepository.kt` (동기) -- 이미 존재하면 수정
- 기존 `UserReadThroughCacheTest.kt` 등 -- `@Nested` 클래스 추가 (NearCache 조합)
- `data/exposed-jdbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/lettuce/repository/ReadThroughCacheTest.kt` (리팩토링)
  - `AutoIncIdRemoteCache`, `AutoIncIdNearCache` @Nested
  - `ClientGenIdRemoteCache`, `ClientGenIdNearCache` @Nested
- `ReadWriteThroughCacheTest.kt` -- 동일 4 @Nested 조합
- `WriteBehindCacheTest.kt` -- 동일 4 @Nested 조합
- Suspended 버전 3개 테스트 클래스도 동일하게 @Nested 조합 추가

### 6-2. exposed-jdbc-redisson 누락 테스트 추가

현재 누락 항목:
- AutoInc + NearCache + RWT
- AutoInc + NearCache + WB
- ClientGen + NearCache + RWT
- ClientGen + NearCache + WB
- `countFromDb()` 테스트

**수정할 파일:**
- 기존 `ReadWriteThroughCacheTest.kt` -- NearCache @Nested 추가
- `WriteBehindCacheTest.kt` -- NearCache @Nested 추가
- Suspended 버전도 동일

### AC
- `./gradlew :bluetape4k-exposed-jdbc-lettuce:test` 성공
- `./gradlew :bluetape4k-exposed-jdbc-redisson:test` 성공
- Lettuce JDBC: 테이블 2종 x 캐시 2종 x 기법 3종 = 12 조합 x 2(동기+suspend) = **24개 @Nested 테스트 클래스**
- Redisson JDBC: 동일 24개 @Nested 테스트 클래스

---

## Task 7: R2DBC 모듈 테스트 표준화 [complexity: medium]

T4+T5 완료 후 수행. R2DBC는 suspend only이므로 JDBC 대비 절반의 테스트 클래스.

### 7-1. exposed-r2dbc-lettuce 누락 테스트 추가

현재 누락 항목:
- AutoInc + NearCache (RT / RWT / WB)
- ClientGenerated ID 전체 (Remote + NearCache, RT / RWT / WB)
- `containsKey()`, `countFromDb()` 테스트

**생성/수정할 파일:**
- `data/exposed-r2dbc-lettuce/src/test/kotlin/io/bluetape4k/exposed/r2dbc/lettuce/domain/` -- UserCredential 스키마/Repository 추가 (필요시)
- 기존 `R2dbcLettuceReadThroughCacheTest.kt` 등 -- @Nested 조합 추가

### 7-2. exposed-r2dbc-redisson 누락 테스트 추가

현재 누락 항목:
- AutoInc + NearCache (RT / RWT / WB)
- ClientGenerated ID 전체 (Remote + NearCache, RT / RWT / WB)
- `countFromDb()` 테스트

**수정할 파일:**
- 기존 테스트 클래스들에 @Nested 조합 추가

### AC
- `./gradlew :bluetape4k-exposed-r2dbc-lettuce:test` 성공
- `./gradlew :bluetape4k-exposed-r2dbc-redisson:test` 성공
- 각 R2DBC 모듈: 테이블 2종 x 캐시 2종 x 기법 3종 = **12개 @Nested 테스트 클래스**

---

## Task 8: README 및 KDoc, CLAUDE.md 업데이트 [complexity: low]

### 8-1. exposed-redis-api README

**생성할 파일:**
- `data/exposed-redis-api/README.md` (영어)
- `data/exposed-redis-api/README.ko.md` (한국어)

내용:
- 모듈 목적 (공통 인터페이스 + testFixtures)
- Mermaid 클래스 다이어그램 (스펙 섹션 10 참조)
- 인터페이스 계층 구조 설명
- 각 메서드의 `javax.cache.Cache` 관례 매핑표

### 8-2. 기존 모듈 README 업데이트

**수정할 파일:**
- `data/exposed-jdbc-lettuce/README.md`, `data/exposed-jdbc-lettuce/README.ko.md`
- `data/exposed-jdbc-redisson/README.md`, `data/exposed-jdbc-redisson/README.ko.md`
- `data/exposed-r2dbc-lettuce/README.md`, `data/exposed-r2dbc-lettuce/README.ko.md`
- `data/exposed-r2dbc-redisson/README.md`, `data/exposed-r2dbc-redisson/README.ko.md`

변경 내용:
- `exposed-redis-api` 의존성 추가 설명
- 인터페이스 계층 구조 업데이트 (기존 인터페이스명 -> 새 인터페이스명)
- API 메서드명 변경 반영

### 8-3. CLAUDE.md 업데이트

**수정할 파일:** `/Users/debop/work/bluetape4k/bluetape4k-projects/CLAUDE.md`

변경 내용:
- "Architecture > Data > Exposed" 테이블에 `exposed-redis-api` 행 추가
- 각 cache repository 모듈 설명에서 인터페이스명 업데이트

### 8-4. KDoc 정리

모든 신규/수정 파일에 한국어 KDoc 작성 확인.

### 8-5. bluetape4k-patterns 체크리스트 전항목 점검

모든 구현 완료(T1–T7) 후 아래 항목을 전수 점검:

| 항목 | 확인 내용 |
|---|---|
| `companion object : KLogging()` | 모든 구현 클래스(Abstract + 테스트 Repository) |
| `E: Serializable` | 인터페이스 + Abstract 클래스 제네릭 바운드 |
| `serialVersionUID = 1L` | Record/Model data class (캐시 직렬화 대상) |
| KDoc (한국어) | 모든 public 인터페이스 메서드, 프로퍼티, 클래스 |
| `exposed.model` 패키지 | Record/DTO 클래스 위치 |
| `DEFAULT_BATCH_SIZE` 상수 | 인터페이스 companion object에 정의, 구현체에서 참조 |
| `extractId` 가시성 | `protected` → `public override` 승격 확인 |
| `invalidateByPattern` LSP | `JdbcRedissonCacheRepository` / `R2dbcRedissonCacheRepository` 전용 확인 |

### AC
- README.md + README.ko.md 모두 최신화
- CLAUDE.md에 `exposed-redis-api` 모듈 반영
- 모든 public 인터페이스/클래스에 KDoc 존재
- bluetape4k-patterns 체크리스트 전항목 통과

---

## 전체 파일 수 추정

| 태스크 | 신규 | 수정 | 삭제 | 소계 |
|---|---|---|---|---|
| T1 | 19 | 0 | 0 | 19 |
| T2 | 0 | ~12 | 2 | ~14 |
| T3 | 0 | ~12 | 2 | ~14 |
| T4 | 0 | ~8 | 1 | ~9 |
| T5 | 0 | ~8 | 1 | ~9 |
| T6 | ~8 | ~6 | 0 | ~14 |
| T7 | ~6 | ~4 | 0 | ~10 |
| T8 | 2 | 10 | 0 | 12 |
| **합계** | **~35** | **~60** | **6** | **~101** |

---

## 주요 설계 결정 (Quick Reference)

| 항목 | 결정 |
|---|---|
| 마이그레이션 전략 | 즉시 일괄 교체 (`@Deprecated` 브릿지 없음) |
| 메서드 명명 | `javax.cache.Cache` API 관례 (`get`, `put`, `invalidate`, `clear`, `containsKey`) |
| `invalidateByPattern` 위치 | `JdbcRedissonCacheRepository` / `R2dbcRedissonCacheRepository` 전용 (LSP 준수) |
| `updateEntity` / `insertEntity` | Abstract 클래스 레벨 (인터페이스 통일 불가 -- Lettuce 직접 호출 vs Redisson MapWriter) |
| 엔티티 제네릭 바운드 | `E: Serializable` (분산 캐시 직렬화 요건 명시) |
| `extractId` 가시성 | `protected` -> `public override` (인터페이스 메서드로 승격) |
| testFixtures 구조 | 동기 4개 + Suspend 4개 + R2DBC 4개 = 12개 시나리오 인터페이스 |

---

## T9: 스펙 대비 구현 검증 (complexity: high)

**의존성**: T6, T7, T8 완료 후

**실행**: `oh-my-claudecode:verifier` 에이전트 (Opus 모델)

### 검증 항목

1. **인터페이스 커버리지** — 스펙 섹션 4/5의 모든 메서드가 구현에 존재하는가?
   - `JdbcCacheRepository`: `get`, `getAll`, `put`, `putAll`, `invalidate`, `invalidateAll`, `clear`, `containsKey`, `findByIdFromDb`, `findAllFromDb`, `countFromDb`, `findAll`, `extractId`
   - `SuspendedJdbcCacheRepository`: 동일 메서드 suspend 버전
   - `R2dbcCacheRepository`: 동일 메서드 suspend 버전
   - `JdbcRedissonCacheRepository` / `R2dbcRedissonCacheRepository`: `invalidateByPattern` 추가

2. **제네릭 바운드** — 모든 인터페이스/Abstract 클래스에 `E: Serializable` 적용 확인

3. **마이그레이션 완전성** — 기존 구 인터페이스 파일(`JdbcLettuceRepository.kt`, `JdbcRedissonRepository.kt`, `R2dbcLettuceRepository.kt`, `R2dbcRedissonRepository.kt`) 삭제 확인

4. **테스트 매트릭스** — 각 모듈에 12조합 테스트 존재 확인
   - JDBC 모듈: AutoInc + ClientGenerated × Remote + NearCache × RT + RWT + WB (동기 + suspend)
   - R2DBC 모듈: 동일 조합 (suspend only)

5. **LSP 준수** — `invalidateByPattern`이 기본 인터페이스에 없음 확인

6. **KDoc** — 모든 public 인터페이스 메서드에 한국어 KDoc 존재

7. **빌드 통과** — `exposed-redis-api`, 4개 구현 모듈 컴파일 오류 없음

### AC
- VERIFIED 판정 시 → 완료
- NEEDS_FIX 판정 시 → 해당 파일 즉시 수정 후 재검증
