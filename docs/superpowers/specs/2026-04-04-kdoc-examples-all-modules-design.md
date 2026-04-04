# KDoc 예제 추가 설계 — 전체 publish 모듈 확장

**날짜**: 2026-04-04  
**선행 작업**: `2026-04-04-kdoc-examples-design.md` (core/io 파일럿 완료)  
**범위**: bluetape4k 전체 publish 모듈 (~75개 모듈, ~1,865개 소스 파일)  
**목표**: 파일럿에서 확립한 KDoc 예제 기준을 전체 모듈로 확장하여 라이브러리 사용성 통일

---

## 1. 범위

### 포함

Maven에 publish되는 모든 모듈의 Kotlin 소스 파일

### 제외

| 제외 대상 | 이유 |
|----------|------|
| `*-demo` 모듈 | publish 비대상 |
| `io/crypto` | deprecated (→ `tink` 사용 권장) |
| `bluetape4k/bom` | 소스 파일 없음 |
| `bluetape4k/core`, `io/io` | 파일럿에서 이미 완료 |

### 이미 완료

| 모듈 | 파일 수 |
|------|--------|
| `bluetape4k/core` | 157 |
| `io/io` | 47 |

---

## 2. KDoc 작성 가이드라인 (파일럿 기준 동일 적용)

### 구조 순서 (필수)

```
설명 (description)
  ↓
인라인 예제 (```kotlin 블록)
  ↓
@param / @return / @throws
```

### 예제 규칙

- 모든 public 클래스/인터페이스/함수/프로퍼티에 예제 1개 이상
- 코드 블록 언어 태그 ` ```kotlin ` 필수
- import 문 포함 금지 (동일/외부 패키지 모두 생략)
- `@sample` 태그 미사용 (인라인 예제만)
- 결과값 주석 표시: `// "result"` 또는 `// [a, b, c]`
- 기존 KDoc 텍스트 삭제/변경 금지 (예제만 추가)
- 코드 로직 변경 금지

### 예시

```kotlin
/**
 * 두 정수의 합을 반환합니다.
 *
 * ```kotlin
 * val result = add(2, 3) // 5
 * ```
 *
 * @param a 첫 번째 정수
 * @param b 두 번째 정수
 * @return 두 정수의 합
 */
fun add(a: Int, b: Int): Int = a + b
```

---

## 3. 카테고리별 모듈 목록

### 카테고리 1: Core 잔여 (107파일)

| 모듈 | 경로 | 소스파일 수 |
|------|------|-----------|
| coroutines | `bluetape4k/coroutines` | 98 |
| logging | `bluetape4k/logging` | 9 |

### 카테고리 2: utils (441파일)

| 모듈 | 경로 | 소스파일 수 |
|------|------|-----------|
| javatimes | `utils/javatimes` | 92 |
| math | `utils/math` | 86 |
| rule-engine | `utils/rule-engine` | 57 |
| idgenerators | `utils/idgenerators` | 43 |
| geo | `utils/geo` | 35 |
| science | `utils/science` | 25 |
| images | `utils/images` | 24 |
| jwt | `utils/jwt` | 22 |
| leader | `utils/leader` | 22 |
| measured | `utils/measured` | 16 |
| states | `utils/states` | 10 |
| money | `utils/money` | 6 |
| mutiny | `utils/mutiny` | 3 |

### 카테고리 3: io 잔여 (334파일)

| 모듈 | 경로 | 소스파일 수 |
|------|------|-----------|
| http | `io/http` | 69 |
| okio | `io/okio` | 49 |
| jackson3 | `io/jackson3` | 37 |
| jackson2 (jackson) | `io/jackson` | 33 |
| tink | `io/tink` | 26 |
| vertx | `io/vertx` | 24 |
| csv | `io/csv` | 17 |
| netty | `io/netty` | 14 |
| feign | `io/feign` | 12 |
| retrofit2 | `io/retrofit2` | 11 |
| protobuf | `io/protobuf` | 10 |
| grpc | `io/grpc` | 8 |
| avro | `io/avro` | 7 |
| fastjson2 | `io/fastjson2` | 5 |
| json | `io/json` | 2 |

### 카테고리 4: testing (97파일)

| 모듈 | 경로 | 소스파일 수 |
|------|------|-----------|
| testcontainers | `testing/testcontainers` | 56 |
| junit5 | `testing/junit5` | 41 |

### 카테고리 5: infra (227파일)

| 모듈 | 경로 | 소스파일 수 |
|------|------|-----------|
| cache-core | `infra/cache-core` | 48 |
| lettuce | `infra/lettuce` | 40 |
| kafka | `infra/kafka` | 30 |
| resilience4j | `infra/resilience4j` | 22 |
| bucket4j | `infra/bucket4j` | 19 |
| cache-lettuce | `infra/cache-lettuce` | 18 |
| micrometer | `infra/micrometer` | 14 |
| opentelemetry | `infra/opentelemetry` | 14 |
| cache-hazelcast | `infra/cache-hazelcast` | 13 |
| redisson | `infra/redisson` | 21 |
| cache-redisson | `infra/cache-redisson` | 9 |

### 카테고리 6: data (316파일)

#### Exposed 계열

| 모듈 | 경로 | 소스파일 수 |
|------|------|-----------|
| exposed-core | `data/exposed-core` | 31 |
| exposed-dao | `data/exposed-dao` | 13 |
| exposed-jdbc | `data/exposed-jdbc` | 8 |
| exposed-r2dbc | `data/exposed-r2dbc` | 8 |
| exposed-jdbc-lettuce | `data/exposed-jdbc-lettuce` | 12 |
| exposed-r2dbc-lettuce | `data/exposed-r2dbc-lettuce` | 6 |
| exposed-jdbc-redisson | `data/exposed-jdbc-redisson` | 12 |
| exposed-r2dbc-redisson | `data/exposed-r2dbc-redisson` | 7 |
| exposed-jackson2 | `data/exposed-jackson` | 7 |
| exposed-jackson3 | `data/exposed-jackson3` | 7 |
| exposed-fastjson2 | `data/exposed-fastjson2` | 6 |
| exposed-jasypt | `data/exposed-jasypt` | 4 |
| exposed-tink | `data/exposed-tink` | 7 |
| exposed-postgresql | `data/exposed-postgresql` | 7 |
| exposed-mysql8 | `data/exposed-mysql8` | 6 |
| exposed-duckdb | `data/exposed-duckdb` | 5 |
| exposed-trino | `data/exposed-trino` | 7 |
| exposed-bigquery | `data/exposed-bigquery` | 3 |
| exposed-measured | `data/exposed-measured` | 1 |

#### 기타 ORM/Data

| 모듈 | 경로 | 소스파일 수 |
|------|------|-----------|
| hibernate | `data/hibernate` | 43 |
| hibernate-reactive | `data/hibernate-reactive` | 8 |
| hibernate-cache-lettuce | `data/hibernate-cache-lettuce` | 3 |
| mongodb | `data/mongodb` | 7 |
| cassandra | `data/cassandra` | 18 |
| jdbc | `data/jdbc` | 13 |
| r2dbc | `data/r2dbc` | 23 |

### 카테고리 7: aws (292파일)

| 모듈 | 경로 | 소스파일 수 |
|------|------|-----------|
| aws | `aws/aws` | 168 |
| aws-kotlin | `aws-kotlin/aws-kotlin` | 124 |

### 카테고리 8: spring-boot3 + spring-boot4 (238파일)

| 모듈 | 경로 (boot3 / boot4) | 소스파일 수 |
|------|---------------------|-----------|
| core | `spring-boot3/core` / `spring-boot4/core` | 36 / 41 |
| cassandra | `spring-boot3/cassandra` / `spring-boot4/cassandra` | 12 / 12 |
| mongodb | `spring-boot3/mongodb` / `spring-boot4/mongodb` | 5 / 5 |
| redis | `spring-boot3/redis` / `spring-boot4/redis` | 4 / 4 |
| r2dbc | `spring-boot3/r2dbc` / `spring-boot4/r2dbc` | 5 / 5 |
| exposed-jdbc | `spring-boot3/exposed-jdbc` / `spring-boot4/exposed-jdbc` | 24 / 24 |
| exposed-r2dbc | `spring-boot3/exposed-r2dbc` / `spring-boot4/exposed-r2dbc` | 8 / 8 |
| hibernate-lettuce | `spring-boot3/hibernate-lettuce` / `spring-boot4/hibernate-lettuce` | 6 / 6 |

### 카테고리 9: virtualthread + timefold (17파일)

| 모듈 | 경로 | 소스파일 수 |
|------|------|-----------|
| virtualthread-api | `virtualthread/api` | 3 |
| virtualthread-jdk21 | `virtualthread/jdk21` | 1 |
| virtualthread-jdk25 | `virtualthread/jdk25` | 1 |
| timefold-solver-persistence-exposed | `timefold/solver-persistence-exposed` | 12 |

---

## 4. 실행 전략

### 세마포어 병렬 처리 (N=5)

```
카테고리 시작
  → 모듈 목록을 5개씩 묶어 병렬 서브에이전트 실행
  → 각 에이전트: 해당 모듈 소스 파일 전체 KDoc 예제 추가
  → 5개 완료 → 다음 5개 처리
  → 카테고리 전체 완료 → 커밋
  → 다음 카테고리 진행
```

### 빌드 검증

- KDoc 전용 변경이므로 컴파일/테스트 생략
- 에이전트가 기존 코드 로직을 변경하지 않았는지 diff 검토로 대체

### 커밋 전략

카테고리 완료 시마다 커밋:

```
docs: <카테고리> KDoc 예제 추가 (<n>개 모듈, <m>개 파일)
```

예:
- `docs: utils KDoc 예제 추가 (13개 모듈, 441개 파일)`
- `docs: infra KDoc 예제 추가 (11개 모듈, 227개 파일)`

---

## 5. 성공 기준

1. 75개 모듈 전체 public API에 인라인 예제 존재
2. KDoc 순서 통일: 설명 → ` ```kotlin ` 예제 → `@param`/`@return`/`@throws`
3. import 문 없음, `@sample` 태그 없음, 기존 텍스트 보존, 결과값 주석 포함
4. 코드 로직 변경 없음 (KDoc 전용)
5. `docs/superpowers/INDEX.md` 갱신 (전체 완료 후)

---

## 6. 전체 통계

| 항목 | 수치 |
|------|------|
| 총 카테고리 | 9개 |
| 총 모듈 수 | ~75개 |
| 총 소스 파일 수 | ~1,865개 |
| 최대 카테고리 | utils (441파일) |
| 최소 카테고리 | virtualthread+timefold (17파일) |
