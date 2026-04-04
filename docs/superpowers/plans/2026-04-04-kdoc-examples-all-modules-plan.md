# 전체 모듈 KDoc 예제 추가 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** bluetape4k 전체 publish 모듈 (~75개)의 모든 public API에 인라인 KDoc 예제를 추가하여 라이브러리 사용성 통일

**Architecture:** 9개 카테고리를 순서대로 처리하며, 카테고리 내에서는 모듈을 5개씩 묶어 병렬 서브에이전트로 처리한다. 카테고리 완료 시 커밋한다.

**Tech Stack:** Kotlin 2.3, KDoc 인라인 예제, git

---

## CLI 도구 규칙 (전 에이전트 공통 적용)

모든 서브에이전트는 다음 CLI 도구를 사용해야 한다:

| 작업 | 사용 명령 | 금지 명령 |
|------|---------|---------|
| 파일 탐색 | `fd -e kt -t f <경로>` | `find` |
| 텍스트 검색 | `rg "pattern" --type kotlin` | `grep -r` |
| 파일 내용 확인 | `bat <파일>` | `cat` |
| 디렉토리 목록 | `eza -la` | `ls -la` |
| 코드 구조 검색 | `ast-grep -p 'fun $NAME($$$)' -l kotlin` | — |

---

## KDoc 작성 규칙 (전 에이전트 공통 적용)

모든 서브에이전트는 다음 규칙을 반드시 준수한다:

### 구조 순서 (필수)
```
설명 (description)
  ↓
인라인 예제 (```kotlin 블록)
  ↓
@param / @return / @throws
```

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

### 규칙 목록
- 모든 public 클래스/인터페이스/함수/프로퍼티에 예제 1개 이상
- 코드 블록 언어 태그 ` ```kotlin ` 필수 (bare ` ``` ` 금지)
- import 문 포함 금지
- `@sample` 태그 미사용 (인라인 예제만)
- 결과값 주석 표시: `// "result"` 또는 `// [a, b, c]` 또는 `// true`
- 기존 KDoc 텍스트 삭제/변경 금지
- 코드 로직 변경 금지

---

## 카테고리 1: Core 잔여

### Task 1: coroutines + logging KDoc 예제 추가

**대상 모듈:**
- `bluetape4k/coroutines/src/main/kotlin/` (98파일)
- `bluetape4k/logging/src/main/kotlin/` (9파일)

- [ ] **Step 1: coroutines 모듈 KDoc 예제 추가 (서브에이전트)**

  서브에이전트에게 다음 지시 전달:
  ```
  bluetape4k/coroutines/src/main/kotlin/ 아래 모든 .kt 파일을 읽고,
  각 파일의 모든 public 클래스/인터페이스/함수/프로퍼티에 인라인 KDoc 예제를 추가하라.
  KDoc 순서: 설명 → ```kotlin 예제 → @param/@return/@throws
  import 금지, @sample 금지, 기존 텍스트 보존, 코드 로직 변경 금지.
  참고 테스트: bluetape4k/coroutines/src/test/kotlin/
  ```

- [ ] **Step 2: logging 모듈 KDoc 예제 추가 (서브에이전트)**

  서브에이전트에게 다음 지시 전달:
  ```
  bluetape4k/logging/src/main/kotlin/ 아래 모든 .kt 파일을 읽고,
  각 파일의 모든 public 클래스/인터페이스/함수/프로퍼티에 인라인 KDoc 예제를 추가하라.
  KDoc 순서: 설명 → ```kotlin 예제 → @param/@return/@throws
  import 금지, @sample 금지, 기존 텍스트 보존, 코드 로직 변경 금지.
  참고 테스트: bluetape4k/logging/src/test/kotlin/
  ```

- [ ] **Step 3: 카테고리 1 커밋**

  ```bash
  git add bluetape4k/coroutines/ bluetape4k/logging/
  git commit -m "docs: core 잔여 KDoc 예제 추가 (2개 모듈, 107개 파일)"
  ```

---

## 카테고리 2: utils (13개 모듈, ~441파일)

### Task 2: utils Batch 1 — javatimes, math, rule-engine, idgenerators, geo

**대상 모듈:**
- `utils/javatimes/src/main/kotlin/` (92파일)
- `utils/math/src/main/kotlin/` (86파일)
- `utils/rule-engine/src/main/kotlin/` (57파일)
- `utils/idgenerators/src/main/kotlin/` (43파일)
- `utils/geo/src/main/kotlin/` (35파일)

- [ ] **Step 1: 5개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5개 동시)**

  각 모듈별 서브에이전트:
  ```
  <모듈>/src/main/kotlin/ 아래 모든 .kt 파일에 인라인 KDoc 예제 추가.
  KDoc 순서: 설명 → ```kotlin 예제 → @param/@return/@throws
  import 금지, @sample 금지, 기존 텍스트 보존, 코드 로직 변경 금지.
  참고 테스트: <모듈>/src/test/kotlin/
  ```

### Task 3: utils Batch 2 — science, images, jwt, leader, measured

**대상 모듈:**
- `utils/science/src/main/kotlin/` (25파일)
- `utils/images/src/main/kotlin/` (24파일)
- `utils/jwt/src/main/kotlin/` (22파일)
- `utils/leader/src/main/kotlin/` (22파일)
- `utils/measured/src/main/kotlin/` (16파일)

- [ ] **Step 1: 5개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5개 동시)**

  각 모듈 동일 지시 적용 (Task 2 Step 1 참고)

### Task 4: utils Batch 3 — states, money, mutiny

**대상 모듈:**
- `utils/states/src/main/kotlin/` (10파일)
- `utils/money/src/main/kotlin/` (6파일)
- `utils/mutiny/src/main/kotlin/` (3파일)

- [ ] **Step 1: 3개 모듈 병렬 KDoc 예제 추가 (서브에이전트 3개 동시)**

  각 모듈 동일 지시 적용

- [ ] **Step 2: 카테고리 2 커밋**

  ```bash
  git add utils/
  git commit -m "docs: utils KDoc 예제 추가 (13개 모듈, 441개 파일)"
  ```

---

## 카테고리 3: io 잔여 (15개 모듈, ~334파일)

### Task 5: io Batch 1 — http, okio, jackson3, jackson, tink

**대상 모듈:**
- `io/http/src/main/kotlin/` (69파일)
- `io/okio/src/main/kotlin/` (49파일)
- `io/jackson3/src/main/kotlin/` (37파일)
- `io/jackson/src/main/kotlin/` (33파일)
- `io/tink/src/main/kotlin/` (26파일)

- [ ] **Step 1: 5개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5개 동시)**

### Task 6: io Batch 2 — vertx, csv, netty, feign, retrofit2

**대상 모듈:**
- `io/vertx/src/main/kotlin/` (24파일)
- `io/csv/src/main/kotlin/` (17파일)
- `io/netty/src/main/kotlin/` (14파일)
- `io/feign/src/main/kotlin/` (12파일)
- `io/retrofit2/src/main/kotlin/` (11파일)

- [ ] **Step 1: 5개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5개 동시)**

### Task 7: io Batch 3 — protobuf, grpc, avro, fastjson2, json

**대상 모듈:**
- `io/protobuf/src/main/kotlin/` (10파일)
- `io/grpc/src/main/kotlin/` (8파일)
- `io/avro/src/main/kotlin/` (7파일)
- `io/fastjson2/src/main/kotlin/` (5파일)
- `io/json/src/main/kotlin/` (2파일)

- [ ] **Step 1: 5개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5개 동시)**

- [ ] **Step 2: 카테고리 3 커밋**

  ```bash
  git add io/http/ io/okio/ io/jackson/ io/jackson3/ io/tink/ io/vertx/ io/csv/ io/netty/ io/feign/ io/retrofit2/ io/protobuf/ io/grpc/ io/avro/ io/fastjson2/ io/json/
  git commit -m "docs: io 잔여 KDoc 예제 추가 (15개 모듈, 334개 파일)"
  ```

---

## 카테고리 4: testing (2개 모듈, 97파일)

### Task 8: testcontainers + junit5 KDoc 예제 추가

**대상 모듈:**
- `testing/testcontainers/src/main/kotlin/` (56파일)
- `testing/junit5/src/main/kotlin/` (41파일)

- [ ] **Step 1: 2개 모듈 병렬 KDoc 예제 추가 (서브에이전트 2개 동시)**

  각 모듈 동일 지시 적용

- [ ] **Step 2: 카테고리 4 커밋**

  ```bash
  git add testing/
  git commit -m "docs: testing KDoc 예제 추가 (2개 모듈, 97개 파일)"
  ```

---

## 카테고리 5: infra (11개 모듈, ~227파일)

### Task 9: infra Batch 1 — cache-core, lettuce, kafka, resilience4j, bucket4j

**대상 모듈:**
- `infra/cache-core/src/main/kotlin/` (48파일)
- `infra/lettuce/src/main/kotlin/` (40파일)
- `infra/kafka/src/main/kotlin/` (30파일)
- `infra/resilience4j/src/main/kotlin/` (22파일)
- `infra/bucket4j/src/main/kotlin/` (19파일)

- [ ] **Step 1: 5개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5개 동시)**

### Task 10: infra Batch 2 — cache-lettuce, redisson, micrometer, opentelemetry, cache-hazelcast, cache-redisson

**대상 모듈:**
- `infra/cache-lettuce/src/main/kotlin/` (18파일)
- `infra/redisson/src/main/kotlin/` (21파일)
- `infra/micrometer/src/main/kotlin/` (14파일)
- `infra/opentelemetry/src/main/kotlin/` (14파일)
- `infra/cache-hazelcast/src/main/kotlin/` (13파일)
- `infra/cache-redisson/src/main/kotlin/` (9파일)

- [ ] **Step 1: 6개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5+1)**

- [ ] **Step 2: 카테고리 5 커밋**

  ```bash
  git add infra/
  git commit -m "docs: infra KDoc 예제 추가 (11개 모듈, 227개 파일)"
  ```

---

## 카테고리 6: data (26개 모듈, ~316파일)

### Task 11: data Batch 1 — exposed-core, exposed-dao, exposed-jdbc, exposed-r2dbc, exposed-jdbc-lettuce

**대상 모듈:**
- `data/exposed-core/src/main/kotlin/` (31파일)
- `data/exposed-dao/src/main/kotlin/` (13파일)
- `data/exposed-jdbc/src/main/kotlin/` (8파일)
- `data/exposed-r2dbc/src/main/kotlin/` (8파일)
- `data/exposed-jdbc-lettuce/src/main/kotlin/` (12파일)

- [ ] **Step 1: 5개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5개 동시)**

### Task 12: data Batch 2 — exposed-r2dbc-lettuce, exposed-jdbc-redisson, exposed-r2dbc-redisson, exposed-jackson, exposed-jackson3

**대상 모듈:**
- `data/exposed-r2dbc-lettuce/src/main/kotlin/` (6파일)
- `data/exposed-jdbc-redisson/src/main/kotlin/` (12파일)
- `data/exposed-r2dbc-redisson/src/main/kotlin/` (7파일)
- `data/exposed-jackson/src/main/kotlin/` (7파일)
- `data/exposed-jackson3/src/main/kotlin/` (7파일)

- [ ] **Step 1: 5개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5개 동시)**

### Task 13: data Batch 3 — exposed-fastjson2, exposed-jasypt, exposed-tink, exposed-postgresql, exposed-mysql8

**대상 모듈:**
- `data/exposed-fastjson2/src/main/kotlin/` (6파일)
- `data/exposed-jasypt/src/main/kotlin/` (4파일)
- `data/exposed-tink/src/main/kotlin/` (7파일)
- `data/exposed-postgresql/src/main/kotlin/` (7파일)
- `data/exposed-mysql8/src/main/kotlin/` (6파일)

- [ ] **Step 1: 5개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5개 동시)**

### Task 14: data Batch 4 — exposed-duckdb, exposed-trino, exposed-bigquery, exposed-measured, hibernate

**대상 모듈:**
- `data/exposed-duckdb/src/main/kotlin/` (5파일)
- `data/exposed-trino/src/main/kotlin/` (7파일)
- `data/exposed-bigquery/src/main/kotlin/` (3파일)
- `data/exposed-measured/src/main/kotlin/` (1파일)
- `data/hibernate/src/main/kotlin/` (43파일)

- [ ] **Step 1: 5개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5개 동시)**

### Task 15: data Batch 5 — hibernate-reactive, hibernate-cache-lettuce, mongodb, cassandra, jdbc, r2dbc

**대상 모듈:**
- `data/hibernate-reactive/src/main/kotlin/` (8파일)
- `data/hibernate-cache-lettuce/src/main/kotlin/` (3파일)
- `data/mongodb/src/main/kotlin/` (7파일)
- `data/cassandra/src/main/kotlin/` (18파일)
- `data/jdbc/src/main/kotlin/` (13파일)
- `data/r2dbc/src/main/kotlin/` (23파일)

- [ ] **Step 1: 6개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5+1)**

- [ ] **Step 2: 카테고리 6 커밋**

  ```bash
  git add data/
  git commit -m "docs: data KDoc 예제 추가 (26개 모듈, 316개 파일)"
  ```

---

## 카테고리 7: aws (2개 모듈, 292파일)

### Task 16: aws + aws-kotlin KDoc 예제 추가

**대상 모듈:**
- `aws/aws/src/main/kotlin/` (168파일)
- `aws-kotlin/aws-kotlin/src/main/kotlin/` (124파일)

  > aws 모듈은 파일 수가 많으므로 각 에이전트가 서비스별 패키지로 나눠 처리하는 것을 고려한다.
  > (예: s3, sqs, sns, dynamodb, ec2 등 서비스 패키지 단위로 분할)

- [ ] **Step 1: aws 모듈 KDoc 예제 추가 (서브에이전트)**

  ```
  aws/aws/src/main/kotlin/ 아래 모든 .kt 파일에 인라인 KDoc 예제 추가.
  파일 수가 많으므로 서비스 패키지별로 순차 처리.
  KDoc 순서: 설명 → ```kotlin 예제 → @param/@return/@throws
  import 금지, @sample 금지, 기존 텍스트 보존, 코드 로직 변경 금지.
  참고 테스트: aws/aws/src/test/kotlin/
  ```

- [ ] **Step 2: aws-kotlin 모듈 KDoc 예제 추가 (서브에이전트)**

  ```
  aws-kotlin/aws-kotlin/src/main/kotlin/ 아래 모든 .kt 파일에 인라인 KDoc 예제 추가.
  KDoc 순서: 설명 → ```kotlin 예제 → @param/@return/@throws
  import 금지, @sample 금지, 기존 텍스트 보존, 코드 로직 변경 금지.
  참고 테스트: aws-kotlin/aws-kotlin/src/test/kotlin/
  ```

- [ ] **Step 3: 카테고리 7 커밋**

  ```bash
  git add aws/ aws-kotlin/
  git commit -m "docs: aws KDoc 예제 추가 (2개 모듈, 292개 파일)"
  ```

---

## 카테고리 8: spring-boot3 + spring-boot4 (16개 모듈, ~238파일)

### Task 17: spring-boot3 Batch 1 — core, cassandra, mongodb, redis, r2dbc

**대상 모듈:**
- `spring-boot3/core/src/main/kotlin/` (36파일)
- `spring-boot3/cassandra/src/main/kotlin/` (12파일)
- `spring-boot3/mongodb/src/main/kotlin/` (5파일)
- `spring-boot3/redis/src/main/kotlin/` (4파일)
- `spring-boot3/r2dbc/src/main/kotlin/` (5파일)

- [ ] **Step 1: 5개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5개 동시)**

### Task 18: spring-boot3 Batch 2 — exposed-jdbc, exposed-r2dbc, hibernate-lettuce

**대상 모듈:**
- `spring-boot3/exposed-jdbc/src/main/kotlin/` (24파일)
- `spring-boot3/exposed-r2dbc/src/main/kotlin/` (8파일)
- `spring-boot3/hibernate-lettuce/src/main/kotlin/` (6파일)

- [ ] **Step 1: 3개 모듈 병렬 KDoc 예제 추가 (서브에이전트 3개 동시)**

### Task 19: spring-boot4 Batch 1 — core, cassandra, mongodb, redis, r2dbc

**대상 모듈:**
- `spring-boot4/core/src/main/kotlin/` (41파일)
- `spring-boot4/cassandra/src/main/kotlin/` (12파일)
- `spring-boot4/mongodb/src/main/kotlin/` (5파일)
- `spring-boot4/redis/src/main/kotlin/` (4파일)
- `spring-boot4/r2dbc/src/main/kotlin/` (5파일)

- [ ] **Step 1: 5개 모듈 병렬 KDoc 예제 추가 (서브에이전트 5개 동시)**

  > spring-boot3과 패키지 구조가 동일하므로 spring-boot3 결과물을 참고한다.

### Task 20: spring-boot4 Batch 2 — exposed-jdbc, exposed-r2dbc, hibernate-lettuce

**대상 모듈:**
- `spring-boot4/exposed-jdbc/src/main/kotlin/` (24파일)
- `spring-boot4/exposed-r2dbc/src/main/kotlin/` (8파일)
- `spring-boot4/hibernate-lettuce/src/main/kotlin/` (6파일)

- [ ] **Step 1: 3개 모듈 병렬 KDoc 예제 추가 (서브에이전트 3개 동시)**

  > spring-boot3 동일 모듈 결과물을 참고한다.

- [ ] **Step 2: 카테고리 8 커밋**

  ```bash
  git add spring-boot3/ spring-boot4/
  git commit -m "docs: spring-boot3/4 KDoc 예제 추가 (16개 모듈, 238개 파일)"
  ```

---

## 카테고리 9: virtualthread + timefold (4개 모듈, 17파일)

### Task 21: virtualthread + timefold KDoc 예제 추가

**대상 모듈:**
- `virtualthread/api/src/main/kotlin/` (3파일)
- `virtualthread/jdk21/src/main/kotlin/` (1파일)
- `virtualthread/jdk25/src/main/kotlin/` (1파일)
- `timefold/solver-persistence-exposed/src/main/kotlin/` (12파일)

- [ ] **Step 1: 4개 모듈 병렬 KDoc 예제 추가 (서브에이전트 4개 동시)**

  각 모듈 동일 지시 적용

- [ ] **Step 2: 카테고리 9 커밋**

  ```bash
  git add virtualthread/ timefold/
  git commit -m "docs: virtualthread+timefold KDoc 예제 추가 (4개 모듈, 17개 파일)"
  ```

---

## Task 22: 마무리 — INDEX.md 갱신

- [ ] **Step 1: docs/superpowers/INDEX.md 갱신**

  `docs/superpowers/INDEX.md`에 이번 작업 항목 추가:
  ```
  | 2026-04-04 | KDoc 예제 전체 확장 | 전체 publish 모듈 (~75개, ~1,865파일) public API 인라인 예제 추가 완료 |
  ```

- [ ] **Step 2: 최종 커밋**

  ```bash
  git add docs/superpowers/INDEX.md
  git commit -m "docs: 전체 모듈 KDoc 예제 추가 완료 — INDEX.md 갱신"
  ```

---

## 전체 태스크 요약

| Task | 카테고리 | 배치 | 모듈 수 | 파일 수 |
|------|---------|------|--------|--------|
| 1 | Core 잔여 | 1 | 2 | 107 |
| 2 | utils | Batch 1 | 5 | 313 |
| 3 | utils | Batch 2 | 5 | 109 |
| 4 | utils | Batch 3 | 3 | 19 |
| 5 | io 잔여 | Batch 1 | 5 | 214 |
| 6 | io 잔여 | Batch 2 | 5 | 74 |
| 7 | io 잔여 | Batch 3 | 5 | 32 |
| 8 | testing | 1 | 2 | 97 |
| 9 | infra | Batch 1 | 5 | 159 |
| 10 | infra | Batch 2 | 6 | 89 |
| 11 | data | Batch 1 | 5 | 72 |
| 12 | data | Batch 2 | 5 | 39 |
| 13 | data | Batch 3 | 5 | 30 |
| 14 | data | Batch 4 | 5 | 59 |
| 15 | data | Batch 5 | 6 | 72 |
| 16 | aws | 1 | 2 | 292 |
| 17 | spring-boot3 | Batch 1 | 5 | 62 |
| 18 | spring-boot3 | Batch 2 | 3 | 38 |
| 19 | spring-boot4 | Batch 1 | 5 | 67 |
| 20 | spring-boot4 | Batch 2 | 3 | 38 |
| 21 | virtualthread+timefold | 1 | 4 | 17 |
| 22 | 마무리 | — | — | — |
| **합계** | | | **~75개** | **~1,865파일** |
