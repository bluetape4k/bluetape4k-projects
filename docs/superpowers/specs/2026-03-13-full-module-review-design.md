# bluetape4k-projects 전체 모듈 심층 코드 리뷰 & 테스트 보강 설계

**날짜**: 2026-03-13
**방식**: 의존성 웨이브 병렬 처리 (B안)
**진행**: 자율 진행 (자동 커밋, 승인 없음)

## 목표

모든 모듈을 의존성 참조 순서대로 심층 코드 리뷰하고 테스트를 보강한다.

## 리뷰 기준

### 코드 리뷰 항목

- `bluetape4k-patterns` 준수 (requireNotBlank/Null/PositiveNumber, KLogging/KLoggingChannel, AtomicFU)
- 로직 정확성 & 엣지 케이스 처리
- 성능 문제 (불필요한 블로킹, 비효율적 자료구조)
- KDoc 누락 (public class, interface, extension functions)
- Magic literal 제거 (const 또는 reflection 활용)

### 테스트 보강 항목

- `kotlin-junit5-testing` 패턴 적용 (Kluent 우선, MockK)
- 엣지 케이스 커버 (null, 빈값, 경계값)
- 예외 경로 테스트
- `runTest` vs `runSuspendIO` 올바른 사용
- Coroutines 동시성 테스트 (`SuspendedJobTester`)

## 웨이브 구성

| 웨이브 | 모듈                                                                                                                                                                       |
|-----|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| W1  | bluetape4k/logging, testing/junit5                                                                                                                                       |
| W2  | bluetape4k/core                                                                                                                                                          |
| W3  | bluetape4k/coroutines, testing/testcontainers                                                                                                                            |
| W4  | io/io, io/okio, io/json, io/jackson, io/jackson3, io/tink, io/crypto                                                                                                     |
| W5  | io/jackson-binary, io/jackson-text, io/jackson3-binary, io/jackson3-text, io/fastjson2, io/avro, io/csv, io/protobuf, io/feign, io/http, io/netty, io/retrofit2, io/grpc |
| W6  | data/jdbc, data/r2dbc, data/exposed-core                                                                                                                                 |
| W7  | data/exposed-dao, data/exposed-jdbc, data/exposed-r2dbc, data/exposed-*, data/cassandra, data/mongodb, data/hibernate, data/hibernate-reactive                           |
| W8  | infra/lettuce, infra/redisson                                                                                                                                            |
| W9  | infra/bucket4j, infra/cache-*, infra/kafka, infra/micrometer, infra/nats, infra/opentelemetry, infra/resilience4j                                                        |
| W10 | utils/* (전체)                                                                                                                                                             |
| W11 | spring/*                                                                                                                                                                 |
| W12 | vertx/*                                                                                                                                                                  |
| W13 | aws/*                                                                                                                                                                    |
| W14 | aws-kotlin/*                                                                                                                                                             |
| W15 | examples/*                                                                                                                                                               |

## 커밋 전략

각 모듈 완료 후 `review(모듈명): 코드 리뷰 및 테스트 보강` 형식으로 커밋.
