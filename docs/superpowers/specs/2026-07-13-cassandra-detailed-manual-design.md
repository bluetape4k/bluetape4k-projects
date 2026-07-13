# Cassandra 상세 매뉴얼 설계

**날짜:** 2026-07-13
**상태:** 기본 구조 구현 완료, 학습 경로 소개 확장안 승인됨
**대상 저장소:** `bluetape4k-projects`, `bluetape4k.github.io`
**기준 버전:** `bluetape4k-projects` `1.11.0`
**기준 commit:** `6187173b58e8b4c5c435c145e00e94708f31ef75`

## 배경

현재 `bluetape4k-cassandra` 매뉴얼은 모듈의 entry point와 테스트 링크를 한 페이지에 나열한다. 어떤 API가 있는지는 알 수 있지만, 세션을 누가 만들고 닫아야 하는지, 캐시 identity를 어떻게 정해야 하는지, 비동기 결과의 다음 페이지는 언제 가져오는지처럼 실제 도입에 필요한 판단은 설명하지 못한다.

README에는 더 많은 예제가 있지만 기능별 예제 모음에 가깝다. README를 읽은 뒤에도 세션 lifecycle, 실패 전파, 운영 경계와 테스트 방법을 다시 source와 test에서 조합해야 한다. 매뉴얼은 이 판단 과정을 대신할 수 있어야 한다.

따라서 `docs/manual`을 기술 설명의 source of truth로 두고, 기존 landing 아래에 주제별 chapter를 추가한다. README와 기존 blog는 초기 자료로만 사용하며, 설명이 다르면 `1.11.0` tag의 source와 representative test를 우선한다.

## 목표

1. `bluetape4k-cassandra`를 README보다 상세한 총 6개 페이지의 매뉴얼로 만든다.
2. 사용자가 세션 생성부터 비동기 조회, row 변환, statement 작성, 운영과 테스트까지 순서대로 익힐 수 있게 한다.
3. 모든 동작 설명을 `1.11.0` source 또는 representative test와 연결한다.
4. 한국어 원본을 자연스러운 기술 문장으로 작성하고 동일한 영문 정보 구조를 제공한다.
5. Site가 같은 release provenance를 가진 deterministic snapshot을 게시하게 한다.

## 비목표

- Kotlin production source, public API, dependency 또는 runtime behavior를 변경하지 않는다.
- Cassandra Java Driver 자체의 전체 사용법을 다시 작성하지 않는다.
- Spring Data Cassandra나 별도 repository abstraction을 다루지 않는다.
- README를 삭제하거나 매뉴얼과 동일한 분량으로 확장하지 않는다.
- 현재 site blog의 Cassandra 단락을 상세 article로 확장하지 않는다.
- 이번 단계에서 새로운 diagram asset을 만들거나 기존 README diagram을 canonical manual asset으로 승격하지 않는다.
- push, PR, merge, release 또는 GitHub Pages deploy를 수행하지 않는다.

## 선택한 구조

단일 장문 페이지와 recipe-only 구조 대신 `module landing + focused chapters`를 사용한다.

- 단일 페이지는 처음 읽기는 쉽지만 lifecycle, query, mapping, 운영 내용이 한 파일에 섞인다.
- recipe-only 구조는 복사 가능한 예제에는 유리하지만 API 선택과 실패 계약이 흩어진다.
- landing과 주제별 chapter를 나누면 학습 순서를 제공하면서도 필요한 계약을 다시 찾기 쉽다.

기존 module landing URL은 유지한다. 하위 chapter만 추가하므로 현재 inbound link와 versioned route가 깨지지 않는다.

## 문서 구조

```text
docs/manual/
├── ko/modules/
│   ├── bluetape4k-cassandra.md
│   └── bluetape4k-cassandra/
│       ├── session-lifecycle.md
│       ├── coroutine-queries.md
│       ├── rows-data-mapping.md
│       ├── statements-query-builder.md
│       └── operations-testing.md
├── en/modules/
│   └── <한국어와 동일한 landing 및 chapter inventory>
└── manifest.yaml
```

Landing과 5개 chapter를 합쳐 locale별 6개 페이지를 제공한다.

## Landing — 선택 기준과 학습 경로

Landing은 기능 목록을 반복하지 않고 다음 질문에 답한다.

- `bluetape4k-cassandra`가 Cassandra Java Driver에 더하는 기능은 무엇인가?
- Java Driver API를 그대로 쓰는 편이 나은 경우는 언제인가?
- 세션을 직접 생성할지 `CqlSessionProvider`로 관리할지 어떻게 고르는가?
- 동기 호출, suspend 호출과 `Flow` 중 어떤 실행 모델을 선택하는가?
- statement, QueryBuilder와 mapper helper는 각각 어느 경계에서 쓰는가?

첫 실행 예제는 중앙 BOM인 `bluetape4k-dependencies`와 artifact를 추가하고, 명시적으로 소유한 `CqlSession`에서 가장 작은 쿼리를 수행한 뒤 닫는 흐름을 보여준다. 이후 학습 경로를 세션 → 비동기 쿼리 → 데이터 변환 → statement 작성 → 운영과 테스트 순으로 연결한다.

### 학습 경로 소개 확장

Landing의 첫 기능 소개 heading은 역할의 소유권을 묻는 `이 라이브러리가 맡는 일` 대신 사용자가 바로 이해할 수 있는 `제공하는 기능`으로 바꾼다. 본문은 세션 생성, coroutine query, row mapping, statement 확장처럼 라이브러리가 실제로 더하는 기능을 먼저 설명하고, cluster와 schema 운영은 애플리케이션 책임이라는 경계를 이어서 밝힌다.

학습 경로는 링크 제목만 나열하지 않는다. 목록 앞에서 다섯 장이 상세 설명, 실행 가능한 예제, API 선택 기준, 실패와 운영 경계를 함께 다룬다는 점과 권장 순서를 설명한다. 각 항목에는 다음 정보를 1~2문장으로 덧붙인다.

- 해당 장에서 해결하는 실제 문제
- 직접 따라 할 수 있는 대표 예제나 API 흐름
- 읽고 난 뒤 내릴 수 있어야 하는 설계 또는 운영 판단

세션 수명주기 → coroutine query → row mapping → statement 작성 → 운영과 테스트라는 순서는 유지한다. 처음 도입하는 사용자는 앞에서부터 읽고, 이미 사용 중인 사용자는 필요한 장으로 바로 이동할 수 있다고 안내한다. Landing에 chapter 본문을 복제하거나 긴 코드 예제를 추가하지는 않는다.

한국어를 먼저 자연스럽게 다듬은 뒤 영문에 같은 정보 밀도와 목록 순서를 반영한다. Site는 이 canonical source를 다시 snapshot하며, 기존 chapter URL과 anchor ID는 유지한다.

## Chapter 1 — Session lifecycle

`session-lifecycle`은 세션 생성, 재사용과 종료 책임을 설명한다.

- `cqlSession`과 `cqlSessionOf`가 새 세션을 직접 만드는 방식
- 직접 만든 세션을 `use` 또는 component lifecycle에서 닫는 방법
- `CqlSessionProvider.getOrCreateSession`의 cache boundary
- `CqlSessionIdentity`의 `keyspace`와 정규화된 `contextParts`
- contact point, datacenter, credential, tenant와 client identity를 context에 포함해야 하는 이유
- 닫힌 session을 cache에서 제거하는 동작
- provider가 만든 final session을 `ShutdownQueue`에 등록하는 lifecycle
- blank keyspace와 blank local datacenter의 fail-fast 계약

### 1.11.0 bootstrap 제한

`1.11.0`의 `CqlSessionProvider`는 keyspace를 만들기 위한 admin session을 먼저 연다. 이 admin session에는 caller의 builder block을 적용하지 않고, keyspace에 연결하는 final session에만 적용한다. 인증이나 SSL처럼 bootstrap에도 필요한 설정을 builder block에만 넣으면 admin session 생성 단계에서 실패할 수 있다.

이 동작은 `1.11.0` 이후 PR #986에서 수정됐지만, 1.11 manual에는 수정 이후 동작을 소급해서 설명하지 않는다. Chapter는 bootstrap에도 필요한 설정을 `builderSupplier`가 만드는 builder에 넣거나 keyspace를 별도 관리하는 회피 방법을 version-specific note로 분리한다.

## Chapter 2 — Coroutine queries

`coroutine-queries`는 Java Driver의 async API를 Kotlin coroutine에서 사용하는 흐름을 설명한다.

- CQL 문자열, positional values, named values와 `Statement`를 받는 `executeSuspending`
- 문자열과 `SimpleStatement`를 받는 `prepareSuspending`
- deprecated `suspendExecute`, `suspendPrepare`, `execute`, `prepare` 대신 사용할 API
- `AsyncResultSet.asFlow()`가 현재 페이지를 방출한 뒤 `fetchNextPage().await()`를 순차 호출하는 방식
- row mapper가 각 row에서 한 번 실행되고 mapper 예외가 collector로 전파되는 계약
- 페이지 fetch 중 `CancellationException`을 다시 던져 coroutine cancellation을 보존하는 계약
- `Flow`가 cold stream이라 collection 시점에 페이지 순회가 실행되는 점

예제는 단일 결과 조회, prepared statement 실행과 여러 페이지를 `Flow`로 읽는 흐름을 각각 완결된 코드로 제공한다. 무제한 수집을 기본 패턴으로 권장하지 않고, query 조건·page size·downstream 처리량을 함께 설명한다.

## Chapter 3 — Rows and data mapping

`rows-data-mapping`은 Cassandra 값을 Kotlin 값과 domain object로 옮기는 경계를 다룬다.

- `Row.toMap`, `toNamedMap`, `toCqlIdentifierMap`의 key 차이
- `getStringOrEmpty`처럼 null을 기본값으로 바꾸는 helper의 적용 범위
- `GettableSupport`와 `SettableSupport`의 name, index, `CqlIdentifier` overload
- list, set, map, tuple, UDT와 `CqlDuration` 변환
- codec registry를 사용하는 동적 값 조회와 custom codec 예제
- `EntityHelper` 기반 prepare/bind helper와 mapper runtime 경계

예제에서는 nullable column을 domain default로 바꾸는 결정과 값이 없음을 그대로 보존해야 하는 결정을 구분한다. `Any?` map 변환은 진단이나 동적 경계에는 편하지만, 안정된 domain model을 대체하지 않는다고 명시한다.

## Chapter 4 — Statements and QueryBuilder

`statements-query-builder`는 CQL을 어떤 형태로 만들고 parameter를 어디서 bind할지 설명한다.

- `statementOf`의 raw CQL, positional value와 named value overload
- `simpleStatementOf`, `boundStatementOf`, `batchStatementOf`의 사용 경계
- consistency, timeout, page size와 keyspace 설정 위치
- `QueryBuilderSupport`, `RelationBuilderSupport`, `TermSupport`
- SELECT, INSERT, UPDATE, DELETE와 schema statement 예제
- raw CQL snippet을 제한적으로 사용해야 하는 이유
- prepared/bound statement와 문자열 조합의 안전성 차이
- logged batch를 일반적인 bulk 처리 수단으로 오해하지 않도록 Cassandra batch semantics를 driver 범위와 구분

예제는 같은 작업을 raw CQL, prepared statement와 QueryBuilder로 비교해 선택 기준을 보여준다. 라이브러리가 제공하지 않는 Cassandra 성능 보장은 주장하지 않는다.

## Chapter 5 — Operations and testing

`operations-testing`은 관리 작업, 관찰 항목, 실패 진단과 검증 방법을 한데 모은다.

- `CassandraAdmin.createKeyspace`, `dropKeyspace`, `getReleaseVersion`
- keyspace bootstrap의 side effect와 필요한 권한
- session ownership, shutdown order와 connection leak 진단
- query latency, timeout, retry, page fetch와 downstream 처리량 관찰
- schema compatibility, consistency level과 idempotency를 애플리케이션에서 결정해야 하는 이유
- `AbstractCassandraTest`, `CqlSessionProviderTest`, async/flow test와 example source 활용법
- Testcontainers 기반 `:bluetape4k-cassandra:test` 실행과 실패 분류

Troubleshooting 표는 최소한 다음 증상을 다룬다.

| 증상 | 먼저 확인할 경계 |
| --- | --- |
| bootstrap에서 인증 또는 연결 실패 | 1.11.0 admin session에 builder block이 적용되지 않는 제한 |
| 같은 keyspace에서 잘못된 session 재사용 | `CqlSessionIdentity.context`에 connection/tenant 경계가 빠졌는지 확인 |
| Flow가 일부 row만 반환 | page fetch, collection cancellation, mapper exception 확인 |
| 종료 후 connection이 남음 | 직접 생성한 session과 provider-owned session의 소유권 구분 |
| batch 처리 지연 또는 timeout | Cassandra batch semantics와 statement 수, consistency, timeout 확인 |

## Chapter 작성 계약

각 chapter는 주제에 맞게 다음 순서를 따른다.

1. 해결하려는 문제
2. mental model
3. 가장 작은 API surface
4. 실행 가능한 예제
5. 대안과 선택 기준
6. failure, cancellation, lifecycle 또는 capacity 계약
7. 운영과 문제 진단
8. `1.11.0` source와 representative test 근거
9. 이어 읽을 chapter

모든 항목을 같은 길이로 채우지는 않는다. 다만 source 근거가 없는 동작 설명, 의미 없는 API 나열과 일반적인 Cassandra 설명으로 분량을 채우는 것은 허용하지 않는다.

## 기술 정보 흐름

```text
1.11.0 source + tests + release history
                  ↓ 검증
bluetape4k-projects/docs/manual
                  ↓ deterministic snapshot
bluetape4k.github.io/versioned manual
                  ↓ 선택적 재구성
future blog or workshop article
```

Canonical Markdown의 repository-relative source/test link는 site sync가 `1.11.0` release commit을 가리키는 immutable GitHub link로 변환한다. 현재 branch의 최신 파일로 자동 이동하는 source link를 versioned page에 넣지 않는다.

## 한국어와 영문 parity

한국어 문서를 먼저 작성한다. 문장은 source와 test가 보여주는 동작을 직접 설명하고, `~를 통해`, `중요합니다`, `강력한 기능` 같은 번역체·홍보성 표현은 사용하지 않는다. API 이름과 설정 key는 원문 그대로 유지한다.

영문 문서는 한국어 문장을 직역하지 않고 같은 기술 계약과 예제를 자연스러운 영어로 옮긴다. 두 locale은 다음 항목이 같아야 한다.

- landing과 chapter ID, 순서, heading 구조
- dependency와 code example
- failure/lifecycle/cancellation 계약
- source/test link inventory
- limitation과 troubleshooting 항목

## 오류 처리와 fail-closed 규칙

다음 조건에서는 site snapshot과 완료 보고로 넘어가지 않는다.

- `1.11.0`에 없는 post-release behavior를 현재 기능처럼 설명함
- source/test로 입증하지 못한 cache, retry, consistency 또는 performance 주장
- bootstrap side effect와 session ownership 누락
- coroutine cancellation 또는 multi-page flow 계약 누락
- 한국어·영문 chapter inventory나 예제 drift
- 깨진 source/test/chapter link
- site snapshot의 release commit 불일치
- Cassandra/Testcontainers 검증 실패를 문서 작업이라는 이유로 무시함

## 검증 전략

검증은 가벼운 proof부터 순차적으로 실행한다.

1. spec/checklist placeholder, contradiction와 scope scan
2. manifest와 bilingual manual validator
3. relative source/test link validation against `1.11.0`
4. Korean naturalness와 KO/EN parity review
5. `git diff --check`와 targeted reference search
6. `:bluetape4k-cassandra:test` 단독 실행
7. Projects manual tests/validation
8. committed Projects source를 사용한 Site manual sync/check
9. Site Node tests와 Astro check/build
10. KO/EN landing과 대표 chapter를 browser에서 읽고 link, code block, navigation과 overflow 확인

Testcontainers-backed Cassandra test는 다른 heavyweight check와 병렬로 실행하지 않는다.

## 인수 조건

- 기존 landing을 선택 지도와 학습 경로로 재작성한다.
- KO/EN 각각 5개 chapter가 같은 ID와 순서로 존재한다.
- manifest가 5개 bilingual chapter를 등록하고 validator가 통과한다.
- dependency example은 `bluetape4k-dependencies`만 소비자 버전으로 노출한다.
- 핵심 cache/lifecycle/query/Flow/mapping/statement/admin 설명이 `1.11.0` source 또는 test에 연결된다.
- 1.11.0 이후 bootstrap fix를 versioned limitation과 혼동하지 않는다.
- 한국어 문장이 번역체가 아닌 자연스러운 기술 문장으로 검수된다.
- Cassandra targeted test, manual validation, site snapshot check, site test/build가 통과한다.
- KO/EN landing과 대표 chapter가 versioned route에서 정상적으로 렌더링된다.
- production Kotlin diff는 0이고, 기존 unrelated untracked files는 보존된다.
