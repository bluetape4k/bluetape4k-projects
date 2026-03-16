# bluetape4k 모듈 통합 설계

**날짜:** 2026-03-17
**목표:** 유지보수 부담 및 사용자 의존성 선언 수 감소
**범위:** 내부 전용 라이브러리 (하위 호환성 불필요)
**현황:** ~117개 모듈 → ~82개 (-35)

---

## 설계 원칙

- **패키지 구분 유지**: 통합 후에도 서비스별 패키지(`io.bluetape4k.aws.dynamodb.*`)를 유지
- **compileOnly 패턴**: 선택적 의존성은 `compileOnly`로 선언, 사용자가 필요한 런타임 의존성을 직접 추가
- **유지**: `virtualthread` 분리 (Java 21/25 런타임 선택 구조), `exposed-*` 분리 (jdbc/r2dbc 사용처 명확히 분리), `io/json` 유지 (fastjson2/jackson 공통 인터페이스)
- **Deprecated 유지 (이번 Phase)**: `io/crypto`, `utils/units` — @Deprecated 마킹만, 다음 Phase 삭제

---

## 통합 목록

### 1. `aws/` → `bluetape4k-aws` (10개 → 1개, -9)

| 흡수 대상 | 패키지 |
|-----------|--------|
| aws/core | `io.bluetape4k.aws.core` |
| aws/dynamodb | `io.bluetape4k.aws.dynamodb` |
| aws/s3 | `io.bluetape4k.aws.s3` |
| aws/ses | `io.bluetape4k.aws.ses` |
| aws/sns | `io.bluetape4k.aws.sns` |
| aws/sqs | `io.bluetape4k.aws.sqs` |
| aws/kms | `io.bluetape4k.aws.kms` |
| aws/cloudwatch | `io.bluetape4k.aws.cloudwatch` |
| aws/kinesis | `io.bluetape4k.aws.kinesis` |
| aws/sts | `io.bluetape4k.aws.sts` |

```kotlin
// aws/build.gradle.kts
dependencies {
    api(project(":bluetape4k-core"))
    api(Libs.aws_core)
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

> **참고**: `aws/`는 장기적으로 `aws-kotlin/`으로 이전 예정. 이번 통합은 중간 정리 단계.

---

### 2. `aws-kotlin/` → `bluetape4k-aws-kotlin` (12개 → 1개, -11)

| 흡수 대상 | 패키지 |
|-----------|--------|
| aws-kotlin/core | `io.bluetape4k.aws.kotlin.core` |
| aws-kotlin/dynamodb | `io.bluetape4k.aws.kotlin.dynamodb` |
| aws-kotlin/s3 | `io.bluetape4k.aws.kotlin.s3` |
| aws-kotlin/ses | `io.bluetape4k.aws.kotlin.ses` |
| aws-kotlin/sesv2 | `io.bluetape4k.aws.kotlin.sesv2` |
| aws-kotlin/sns | `io.bluetape4k.aws.kotlin.sns` |
| aws-kotlin/sqs | `io.bluetape4k.aws.kotlin.sqs` |
| aws-kotlin/kms | `io.bluetape4k.aws.kotlin.kms` |
| aws-kotlin/cloudwatch | `io.bluetape4k.aws.kotlin.cloudwatch` |
| aws-kotlin/kinesis | `io.bluetape4k.aws.kotlin.kinesis` |
| aws-kotlin/sts | `io.bluetape4k.aws.kotlin.sts` |
| aws-kotlin/tests | `io.bluetape4k.aws.kotlin.tests` |

```kotlin
// aws-kotlin/build.gradle.kts
dependencies {
    api(project(":bluetape4k-coroutines"))
    api(Libs.aws_kotlin_core)
    compileOnly(Libs.aws_kotlin_dynamodb)
    compileOnly(Libs.aws_kotlin_s3)
    compileOnly(Libs.aws_kotlin_ses)
    compileOnly(Libs.aws_kotlin_sesv2)
    compileOnly(Libs.aws_kotlin_sns)
    compileOnly(Libs.aws_kotlin_sqs)
    compileOnly(Libs.aws_kotlin_kms)
    compileOnly(Libs.aws_kotlin_cloudwatch)
    compileOnly(Libs.aws_kotlin_kinesis)
    compileOnly(Libs.aws_kotlin_sts)
}
```

---

### 3. `infra/cache` 통합 (4개 + umbrella → 1개, -4)

기존 umbrella `cache`(0kt)를 실제 구현 모듈로 전환. `cache-core`, `cache-hazelcast`, `cache-lettuce`, `cache-redisson` 흡수.

```kotlin
// infra/cache/build.gradle.kts
dependencies {
    api(project(":bluetape4k-coroutines"))
    // 캐시 코어 (Caffeine, JCache)
    api(Libs.caffeine)
    api(Libs.javax_cache_api)
    // 백엔드 - 사용자가 필요한 것만 runtime 추가
    compileOnly(Libs.hazelcast)
    compileOnly(Libs.lettuce_core)
    compileOnly(Libs.redisson)
}
```

내부 패키지:
- `io.bluetape4k.cache.core` (기존 cache-core)
- `io.bluetape4k.cache.hazelcast` (기존 cache-hazelcast)
- `io.bluetape4k.cache.lettuce` (기존 cache-lettuce)
- `io.bluetape4k.cache.redisson` (기존 cache-redisson)

---

### 4. `io/jackson` 통합 (3개 → 1개, -2)

`jackson-binary` + `jackson-text` → `jackson` 흡수.

내부 패키지:
- `io.bluetape4k.jackson` (기존 jackson)
- `io.bluetape4k.jackson.binary` (기존 jackson-binary)
- `io.bluetape4k.jackson.text` (기존 jackson-text)

---

### 5. `io/jackson3` 통합 (3개 → 1개, -2)

`jackson3-binary` + `jackson3-text` → `jackson3` 흡수. jackson과 동일 패턴.

---

### 6. `utils/geo` 통합 (3개 → 1개, -2)

`geocode` + `geohash` + `geoip2` → `geo`.

내부 패키지:
- `io.bluetape4k.geo.geocode`
- `io.bluetape4k.geo.geohash`
- `io.bluetape4k.geo.geoip2`

```kotlin
// utils/geo/build.gradle.kts
dependencies {
    api(project(":bluetape4k-core"))
    compileOnly(Libs.geocoder_java)
    compileOnly(Libs.geohash)
    compileOnly(Libs.geoip2)
}
```

---

### 7. `vertx/` 통합 (3개 → 1개, -2)

`vertx/core` + `vertx/resilience4j` + `vertx/sqlclient` → `vertx`.

내부 패키지:
- `io.bluetape4k.vertx.core`
- `io.bluetape4k.vertx.resilience4j`
- `io.bluetape4k.vertx.sqlclient`

---

### 8. `spring/` 재편 (9개 → 5개 + hibernate 이동, -5)

#### 8-1. `bluetape4k-spring-boot3` 신규 생성

`spring/core` + `spring/webflux` + `spring/tests` + `spring/retrofit2` 통합.

> 향후 Spring Boot 4 대응 시 `bluetape4k-spring-boot4` 신규 생성 예정.

내부 패키지:
- `io.bluetape4k.spring.core`
- `io.bluetape4k.spring.webflux`
- `io.bluetape4k.spring.tests`
- `io.bluetape4k.spring.retrofit2`

#### 8-2. `spring/jpa` → `data/hibernate` 이동

`spring/jpa`의 `StatelessSession` 관련 코드(1kt)를 `data/hibernate` 패키지로 이동.
- 이동 후: `io.bluetape4k.hibernate.spring`

#### 8-3. 유지

| 모듈 | 이유 |
|------|------|
| `spring/cassandra` | spring-data-cassandra 전용 |
| `spring/mongodb` | spring-data-mongodb-reactive 전용 |
| `spring/r2dbc` | spring-data-r2dbc 전용 |
| `spring/data-redis` | spring-data-redis 직렬화 전용 |

---

## 유지 모듈 (변경 없음)

| 모듈 | 이유 |
|------|------|
| `virtualthread/api`, `jdk21`, `jdk25` | Java 21/25 런타임 선택 — ServiceLoader 구조상 분리 필수 |
| `data/exposed-*` 전체 | jdbc/r2dbc 사용처 명확히 분리 |
| `io/json` | fastjson2/jackson 공통 인터페이스 |
| `io/protobuf` | gRPC 없이 독립 사용 가능 (Kafka, REST 등) — grpc가 protobuf에 의존하는 구조 유지 |
| `io/grpc` | protobuf 의존, gRPC 서버/클라이언트 전용 — 분리 유지 |
| `io/netty` | 네트워킹(ByteBuf/Channel) — io/ 직렬화 계열과 성격이 달라 분리 유지. webflux/redisson/lettuce 전이 의존성으로 제공됨 |
| `io/crypto` | @Deprecated 마킹만 — 다음 Phase 삭제 |
| `utils/units` | @Deprecated 마킹만 — 다음 Phase 삭제 |

---

## 통합 후 모듈 수 요약

| 영역 | Before | After | 절감 |
|------|--------|-------|------|
| aws/ | 10 | 1 | -9 |
| aws-kotlin/ | 12 | 1 | -11 |
| infra/cache | 5 (4+umbrella) | 1 | -4 |
| io/jackson | 3 | 1 | -2 |
| io/jackson3 | 3 | 1 | -2 |
| utils/geo* | 3 | 1 | -2 |
| vertx/ | 3 | 1 | -2 |
| spring/ | 9 | 5 (+hibernate 이동) | -5 |
| **합계** | **~117** | **~82** | **-35** |

---

## 작업 우선순위 (Phase 1)

독립성이 높고 위험이 낮은 순서:

1. `io/jackson` + `io/jackson3` 통합 (의존성 단순, 위험 낮음)
2. `utils/geo` 통합
3. `vertx/` 통합
4. `spring/` 재편 (`spring-boot3` 생성, `spring/jpa` → hibernate 이동)
5. `infra/cache` 통합 (compileOnly 패턴 적용)
6. `aws/` 통합 (규모 최대, 마지막)
7. `aws-kotlin/` 통합

---

## 다음 Phase (별도 계획)

- `aws/` → `aws-kotlin/`으로 코드 이전 및 `aws/` 삭제
- `io/crypto` 삭제
- `utils/units` 삭제
