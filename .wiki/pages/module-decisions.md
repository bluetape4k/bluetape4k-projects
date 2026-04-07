# Module Decisions

> 마지막 업데이트: 2026-04-07 | 관련 specs: 3개

## 개요

bluetape4k-projects의 모듈 구조 결정, 통합/분리 기준, 빌드 설정 패턴을 다룬다. debop4k에서 bluetape4k로의 마이그레이션 원칙, 모듈 수를 ~117개에서 ~82개로 줄인 통합 전략을 포함한다.

## 핵심 설계 결정 (ADR)

| 결정 | 이유 | 날짜 | 관련 spec |
|------|------|------|----------|
| `includeModules` 자동 등록 | 디렉토리만 만들면 `settings.gradle.kts` 수정 없이 모듈 자동 인식 | 2026-03-17 | module-consolidation-design |
| umbrella 모듈: `infra/cache`, `infra/redis` | 사용자가 하나의 의존성으로 전체 서브모듈 포함 가능 | 2026-03-17 | module-consolidation-design |
| aws/aws-kotlin 각각 단일 통합 모듈 | 10+12개 서브모듈 → 2개. 서비스별 `compileOnly` 선언 | 2026-03-17 | module-consolidation-design |
| virtualthread api/jdk21/jdk25 분리 유지 | ServiceLoader 기반 런타임 선택 구조 — 분리 필수 | 2026-03-17 | module-consolidation-design |
| exposed-* 시리즈 분리 유지 | jdbc/r2dbc 사용처 명확히 구분 필요 | 2026-03-17 | module-consolidation-design |
| debop4k → bluetape4k: ReentrantLock 전환 | `@Synchronized` → `ReentrantLock.withLock {}` 로 thread-safety 개선 | 2026-03-22 | debop4k-migration-design |
| Fork & Adapt (Spring Boot 3/4) | 코드 차이 3~5개 파일 수준 — 공유 모듈 대비 빌드 단순성 우선 | 2026-03-29 | spring-data-exposed-migration-design |

## 패턴 & 사용법

### settings.gradle.kts `includeModules` 자동 등록

```kotlin
// settings.gradle.kts
includeModules("data", withBaseDir = false)           // :bluetape4k-{dirname}
includeModules("spring-boot3", withBaseDir = true)    // :bluetape4k-spring-boot3-{dirname}
includeModules("spring-boot4", withBaseDir = true)    // :bluetape4k-spring-boot4-{dirname}
```

**자동 생성 모듈명 규칙**:

| 디렉토리 | `withBaseDir = false` | `withBaseDir = true` |
|----------|----------------------|---------------------|
| `data/exposed-core/` | `:bluetape4k-exposed-core` | - |
| `spring-boot3/core/` | - | `:bluetape4k-spring-boot3-core` |
| `spring-boot4/hibernate-lettuce/` | - | `:bluetape4k-spring-boot4-hibernate-lettuce` |

**변경 없이 새 모듈 추가하는 방법**: 해당 디렉토리에 `build.gradle.kts`를 생성하면 자동 등록.

### testImplementation extendsFrom 패턴

모든 모듈에서 `compileOnly` 의존성을 테스트에서도 사용하기 위한 설정:

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}
```

**이유**: Spring 모듈처럼 `compileOnly`로 선택적 의존성을 선언할 때, 테스트에서 해당 의존성을 사용하려면 수동으로 `testImplementation`에 추가해야 하는 번거로움 제거.

### compileOnly vs implementation vs api 전략

```kotlin
dependencies {
    // ✅ 라이브러리 인터페이스 — API에 노출되므로 api
    api(project(":bluetape4k-core"))

    // ✅ 선택적 의존성 — 사용자가 필요 시 직접 추가
    compileOnly(Libs.exposed_jdbc)
    compileOnly(Libs.postgresql_driver)
    compileOnly(Libs.postgis_jdbc)

    // ✅ 내부 구현 — 전이 의존성 오염 방지
    implementation(Libs.fory_kotlin)
    implementation(Libs.zstd_jni)

    // ✅ 런타임 전용 (테스트/실행 시만 필요)
    testRuntimeOnly(Libs.h2_v2)
    testRuntimeOnly(Libs.hikaricp)
}
```

**compileOnly 패턴 핵심**:
- DB 드라이버, Spring Boot starters, 선택적 기능 라이브러리
- 사용자 프로젝트에서 필요한 것만 런타임 의존성으로 추가
- `configurations { testImplementation.extendsFrom(compileOnly) }` 로 테스트에서 자동 포함

### 모듈 통합 결정 기준

| 통합 O | 통합 X |
|--------|--------|
| 같은 도메인, 항상 함께 사용 | java/kotlin 런타임 선택 (virtualthread) |
| 코드 차이 3~5개 파일 수준 | jdbc/r2dbc 사용처 명확히 분리 |
| 독립 배포 필요성 없음 | 공통 인터페이스 (io/json) |
| 의존성 그래프 단순화 | gRPC가 protobuf에 의존하는 구조 |

**umbrella 모듈 패턴** (`build.gradle.kts`가 다른 모듈을 api로 포함):

```kotlin
// infra/cache/build.gradle.kts  (umbrella)
dependencies {
    api(project(":bluetape4k-cache-core"))
    api(project(":bluetape4k-cache-hazelcast"))
    api(project(":bluetape4k-cache-lettuce"))
    api(project(":bluetape4k-cache-redisson"))
}
```

### debop4k → bluetape4k 마이그레이션 원칙

1. **Java → Kotlin 2.3 현대화**: Java 코드를 Kotlin 관용 표현으로 변환
2. **Thread-safety 강화**: `@Synchronized` → `ReentrantLock.withLock {}`
3. **Eclipse Collections 의존 제거**: `FastList` → `mutableListOf()` 또는 표준 컬렉션
4. **파일명 규칙**: `Permutationx.kt` → `PermutationSupport.kt` (`x` suffix 제거)
5. **외부 의존성 최소화**: `debop4k.core.utils.hashOf` → `Objects.hash()` 대체

```kotlin
// debop4k 패턴 (Java-style)
@Synchronized
fun push(item: T) { ... }

// bluetape4k 패턴
private val lock = ReentrantLock()
fun push(item: T) = lock.withLock { ... }
```

**thread-safety 변환 대상**:

| 클래스 | 기존 | 변환 후 |
|--------|------|--------|
| `Cons` | `synchronized(lock)` + `@Volatile` | `ReentrantLock.withLock {}` + `@Volatile` |
| `BoundedStack` | `@Synchronized` (15개 메서드) | `ReentrantLock.withLock {}` |
| `RingBuffer` | thread-safe 아님 | `ReentrantLock.withLock {}` 추가 |
| `XXHasher` | thread-safe 아님 | `ThreadLocal<StreamingXXHash32>` |

### aws/aws-kotlin 단일 모듈 compileOnly 패턴

```kotlin
// aws/build.gradle.kts
dependencies {
    api(project(":bluetape4k-core"))
    api(Libs.aws_core)
    // 서비스별 선택적 — 사용자가 필요한 것만 runtime에 추가
    compileOnly(Libs.aws_dynamodb)
    compileOnly(Libs.aws_s3)
    compileOnly(Libs.aws_ses)
    compileOnly(Libs.aws_sns)
    compileOnly(Libs.aws_sqs)
    compileOnly(Libs.aws_kms)
    compileOnly(Libs.aws_cloudwatch)
    compileOnly(Libs.aws_kinesis)
    compileOnly(Libs.aws_sts)
}
```

### 전체 모듈 심층 코드 리뷰 기준

리뷰 항목 우선순위:

1. `bluetape4k-patterns` 준수: `requireNotBlank/Null/PositiveNumber`, `KLogging`, `AtomicFU`
2. 성능 문제: 불필요한 블로킹, 비효율적 자료구조
3. KDoc 누락: public class, interface, extension functions
4. Magic literal 제거: `const` 또는 reflection 활용
5. 테스트: Kluent 우선, `runTest` vs `runSuspendIO` 올바른 사용

**웨이브 순서** (의존성 참조 순서): `logging/junit5` → `core` → `coroutines/testcontainers` → `io/*` → `data/*` → `infra/*` → `utils/*` → `spring/*` → `aws/*`

### Fork & Adapt 패턴 (Spring Boot 3/4)

Spring Boot 3과 4 간 코드 차이가 3~5개 파일, 각 1~3줄 수준인 경우:

```
spring-boot3/exposed-jdbc-spring-data/  (전체 소스 + Spring 6 적응)
spring-boot4/exposed-jdbc-spring-data/  (전체 소스 + Spring 7 적응, experimental과 거의 동일)
```

공유 모듈 추출 방안이 채택되지 않은 이유:

| 공유 방안 | 단점 |
|----------|------|
| Gradle source set 공유 | `includeModules` 함수와 충돌, 빌드 복잡성 증가 |
| 별도 `-core` 모듈 | 3개 모듈 추가 → 의존성 그래프 복잡 |
| 심볼릭 링크 | OS 의존, Git 호환성 문제 |

## 선택하지 않은 방식 / 트레이드오프

| 방식 | 채택하지 않은 이유 |
|------|-----------------|
| 모든 aws 서비스별 독립 모듈 유지 | ~22개 모듈 유지보수 부담. 사용자 의존성 선언 수 감소 위해 통합 |
| `infra/cache-*` 독립 유지 | 항상 함께 사용됨. umbrella로 단순화 |
| `jackson` + `jackson3` 통합 | Spring Boot 3/4가 각각 다른 버전 요구. 분리 유지 |
| io/json 제거 | fastjson2/jackson 공통 인터페이스로 사용됨. 분리 유지 |

## 관련 페이지

- [spring-boot-integration.md](spring-boot-integration.md) — Fork & Adapt 상세
- [dependency-decisions.md](dependency-decisions.md) — 의존성 버전 결정 이력
- [database-dialects.md](database-dialects.md) — data/ 모듈 구조
