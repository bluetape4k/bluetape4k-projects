# Cassandra Detailed Manual Implementation Plan

> **For agentic
workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `bluetape4k-cassandra`를 1.11.0 source/test에 고정된 한국어·영문 landing과 5개 상세 chapter를 갖춘 versioned manual로 게시한다.

**Architecture:** `bluetape4k-projects/docs/manual`이 기술 설명의 source of truth이며 landing은 선택 지도, 하위 chapter는 session, coroutine query, data mapping, statement 작성, 운영·테스트 책임을 각각 소유한다. Manifest가 bilingual inventory를 선언하고 Projects validator가 1.11.0 release path를 검증한 뒤, `bluetape4k.github.io`가 같은 release commit을 유지한 채 1.11 snapshot을 editorial refresh한다.

**Tech
Stack:** Markdown, YAML, Kotlin/Apache Cassandra Java Driver API examples, Ruby/Minitest manual validators, Gradle/JUnit 5/Testcontainers, Node.js manual snapshot tests, Astro/Starlight, browser visual QA

---

## 실행 경계

- Projects worktree: `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feature-all-module-manuals`
- Site worktree: `/Users/debop/work/bluetape4k/bluetape4k.github.io/.worktrees/feature-ecosystem-atlas-manual`
- Projects branch: `feature/all-module-manuals`
- Site branch: `feature/ecosystem-atlas-manual`
- Release ref: `1.11.0`
- Release commit: `6187173b58e8b4c5c435c145e00e94708f31ef75`
- Production Kotlin source, dependency, runtime behavior는 변경하지 않는다.
- 새 diagram asset을 만들거나 README diagram을 manual로 복사하지 않는다.
- 각 문서 task는 한국어 원본, 영문 parity, source/test link를 한 commit에 함께 넣는다.
- Cassandra/Testcontainers test는 다른 heavyweight test와 병렬로 실행하지 않는다.
- Site sync 전에는 Projects의 `docs/manual`과 release validator가 commit에 포함되고 clean해야 한다.
- Push, PR, merge, release와 deploy는 실행 범위가 아니다.

## 파일 책임 지도

### `bluetape4k-projects`

- `docs/manual/ko/modules/bluetape4k-cassandra.md`: 한국어 선택 지도, 설치, 첫 쿼리와 학습 경로.
- `docs/manual/en/modules/bluetape4k-cassandra.md`: 한국어 landing과 동일한 기술 계약을 가진 영문 landing.
- `docs/manual/{ko,en}/modules/bluetape4k-cassandra/session-lifecycle.md`: session creation, identity cache, bootstrap limitation과 shutdown ownership.
- `docs/manual/{ko,en}/modules/bluetape4k-cassandra/coroutine-queries.md`: suspend query, prepared statement, multi-page `Flow`와 cancellation.
- `docs/manual/{ko,en}/modules/bluetape4k-cassandra/rows-data-mapping.md`: row conversion, gettable/settable, UDT/tuple/codec와 mapper helper.
- `docs/manual/{ko,en}/modules/bluetape4k-cassandra/statements-query-builder.md`: statement factories, prepared/bound/batch와 QueryBuilder 선택.
- `docs/manual/{ko,en}/modules/bluetape4k-cassandra/operations-testing.md`: admin side effect, 운영 진단, Testcontainers와 representative tests.
- `docs/manual/manifest.yaml`: Cassandra landing 아래 5개 bilingual chapter inventory.
- `docs/manual/generated/manifest.json`: exporter가 만든 deterministic manifest snapshot.
- `docs/superpowers/checklists/2026-07-13-cassandra-manual-checklist.md`: gate evidence와 현재 완료 집계.

### `bluetape4k.github.io`

- `src/content/docs/ko/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra.md`: sync가 생성하는 한국어 versioned landing.
- `src/content/docs/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra.md`: sync가 생성하는 영문 versioned landing.
- `src/content/docs/{ko/,}manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra/*.md`: sync가 생성하는 5개 localized chapter.
- `src/data/manual/bluetape4k-projects.versions.json`: release/source commit과 document inventory.
- `src/data/manual/bluetape4k-projects.snapshot.json`: content digest와 publication metadata.
- `src/content/docs/{ko/,}manual/bluetape4k-projects/1.11/data.json`: locale별 navigation data.

Site의 generated manual 파일과 data JSON은 직접 편집하지 않고 `npm run sync:manual`로만 갱신한다.

## Release evidence ledger

| 계약                     | 1.11.0 근거                                                                  | 매뉴얼 결론                                                                                                        |
|--------------------------|------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| Session cache identity   | `CqlSessionProvider.kt`, `CqlSessionProviderTest.kt`, PR #919                | keyspace만으로 cache하지 않고 connection/tenant context를 명시한다.                                                |
| Bootstrap builder        | 1.11.0 `CqlSessionProvider.kt`; post-release PR #986                         | admin session은 builder block을 받지 않는다. bootstrap 설정은 `builderSupplier`에 넣거나 keyspace를 별도 관리한다. |
| Direct session ownership | `CqlSessionSupport.kt`, `CqlSessionSupportTest.kt`                           | `cqlSession`/`cqlSessionOf`로 만든 session은 caller가 닫는다.                                                      |
| Provider shutdown        | `CqlSessionProvider.kt`, `ShutdownQueue` registration                        | provider가 만든 final session은 process shutdown queue에 등록된다.                                                 |
| Suspend query            | `AsyncCqlSessionSupport.kt`와 test                                           | `executeSuspending`/`prepareSuspending`을 사용하고 deprecated alias는 migration note로만 둔다.                     |
| Multi-page Flow          | `AsyncResultSetSupport.kt`와 unit/integration test                           | 현재 page를 방출한 뒤 next page를 순차 fetch하며 cancellation을 재전파한다.                                        |
| Row/value mapping        | `RowSupport.kt`, `GettableSupport.kt`, `SettableSupport.kt`와 tests/examples | dynamic map과 typed domain mapping의 용도를 구분한다.                                                              |
| Statements/QueryBuilder  | `StatementSupport.kt`, `querybuilder/*.kt`와 examples                        | raw, prepared/bound, builder 방식을 task와 safety 기준으로 비교한다.                                               |
| Admin/testing            | `CassandraAdmin.kt`, `AbstractCassandraTest.kt`, admin/provider/async tests  | bootstrap side effect와 Testcontainers 검증 경계를 문서화한다.                                                     |

### Task 1: Landing과 session lifecycle을 작성한다

**Files:**

- Modify: `docs/manual/ko/modules/bluetape4k-cassandra.md`
- Modify: `docs/manual/en/modules/bluetape4k-cassandra.md`
- Create: `docs/manual/ko/modules/bluetape4k-cassandra/session-lifecycle.md`
- Create: `docs/manual/en/modules/bluetape4k-cassandra/session-lifecycle.md`

- [ ] **Step 1: 1.11.0 session 계약을 다시 읽는다**

Run:

```bash
git show 1.11.0:data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt
git show 1.11.0:data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionSupport.kt
git show 1.11.0:data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt
git merge-base --is-ancestor 9ad16dc09 1.11.0
git merge-base --is-ancestor 0ad15119d 1.11.0
```

Expected: cache identity fix command exits 0, bootstrap builder fix command exits 1. `resolveSession`에서 admin session은 `builderSupplier().build()`, final session은 `withKeyspace(...).apply(builder).build()`를 사용한다.

- [ ] **Step 2: 한국어 landing을 선택 지도 중심으로 교체한다**

Frontmatter는 다음 값을 사용한다.

```yaml
---
manualId: bluetape4k-cassandra
title: "Module bluetape4k-cassandra"
description: "Apache Cassandra Java Driver를 Kotlin의 세션 수명주기, 코루틴 쿼리와 타입 변환 관점에서 사용하는 방법을 설명합니다."
kind: library
group: data
---
```

본문 heading은 다음 순서로 고정한다.

```markdown
# Module bluetape4k-cassandra
## 이 라이브러리가 맡는 일
## 사용하기 전에 결정할 것
## 의존성 추가
## 첫 쿼리
## API 선택 지도
## 학습 경로
## 1.11.0에서 알아둘 제한
## Source와 tests
```

의존성 예제는 소비자에게 중앙 BOM 버전 하나만 노출한다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cassandra")
}
```

첫 쿼리는 `cqlSessionOf(...).use { session -> session.execute(...) }`로 session ownership을 보이도록 작성한다. 학습 경로는 정확히 `session-lifecycle`, `coroutine-queries`, `rows-data-mapping`, `statements-query-builder`, `operations-testing` 순서로 연결한다.

- [ ] **Step 3: 영문 landing을 같은 계약으로 작성한다**

영문 heading은 다음 순서로 고정하고 한국어와 code block 및 link inventory를 동일하게 유지한다.

```markdown
# Module bluetape4k-cassandra
## What this library owns
## Decisions before adopting it
## Add the dependency
## First query
## API decision map
## Learning path
## 1.11.0 limitation
## Sources and tests
```

Description은 `Use the Apache Cassandra Java Driver from Kotlin with explicit session ownership, coroutine queries, and typed value mapping.`으로 쓴다.

- [ ] **Step 4: 한국어와 영문 session chapter를 완성한다**

두 파일은 다음 frontmatter를 사용한다.

```yaml
# ko
title: CqlSession 수명주기와 캐시 경계
description: 직접 만든 세션과 provider가 관리하는 세션의 소유권, identity와 1.11.0 bootstrap 제한을 설명합니다.
manualId: bluetape4k-cassandra
chapterId: session-lifecycle

# en
title: CqlSession lifecycle and cache boundaries
description: Understand ownership, cache identity, and the 1.11.0 bootstrap limitation for direct and provider-managed sessions.
manualId: bluetape4k-cassandra
chapterId: session-lifecycle
```

각 frontmatter는 `---`로 감싼다. 본문은 문제 → 직접 생성 → provider identity → bootstrap limitation → shutdown → failure table → source/test → 이어 읽기 순서로 작성한다.

직접 소유 예제는 다음 cleanup을 포함한다.

```kotlin
cqlSessionOf(
    contactPoint = InetSocketAddress("localhost", 9042),
    localDatacenter = "datacenter1",
    keyspaceName = "orders",
).use { session ->
    session.execute("SELECT release_version FROM system.local").one()
}
```

재사용 예제는 `CqlSessionIdentity.of`의 context를 고정한다.

```kotlin
val identity = CqlSessionIdentity.of(
    keyspace = "tenant_orders",
    contextParts = listOf(
        "contactPoint=cassandra-a:9042",
        "localDatacenter=dc-a",
        "tenant=tenant-a",
        "clientId=order-reader",
    ),
)

val session = CqlSessionProvider.getOrCreateSession(
    identity = identity,
    builderSupplier = {
        CqlSession.builder()
            .addContactPoint(InetSocketAddress("cassandra-a", 9042))
            .withLocalDatacenter("dc-a")
            .withAuthCredentials(username, password)
    },
) {
    withApplicationName("order-reader")
}
```

1.11.0 note는 `builderSupplier`가 admin/final builder 모두에 필요한 설정을 제공하고 builder block은 final session에만 적용된다고 명시한다. `CqlSessionProvider`가 만든 session에 `use`를 적용하는 예제를 기본 패턴으로 쓰지 않는다.

- [ ] **Step 5: source link와 한국어 문장을 확인한다**

Run:

```bash
ruby scripts/manual/validate_release_manuals.rb 1.11.0 6187173b58e8b4c5c435c145e00e94708f31ef75
rg -n "~를 통해|중요합니다|강력한|할 수 있을 것으로" docs/manual/ko/modules/bluetape4k-cassandra.md docs/manual/ko/modules/bluetape4k-cassandra/session-lifecycle.md
git diff --check -- docs/manual/ko/modules/bluetape4k-cassandra.md docs/manual/en/modules/bluetape4k-cassandra.md docs/manual/ko/modules/bluetape4k-cassandra/session-lifecycle.md docs/manual/en/modules/bluetape4k-cassandra/session-lifecycle.md
```

Expected: release validator reports 0 missing. Korean phrase search returns no translated/promotional wording; diff check exits 0.

- [ ] **Step 6: landing과 session chapter를 커밋한다**

```bash
git add docs/manual/ko/modules/bluetape4k-cassandra.md \
  docs/manual/en/modules/bluetape4k-cassandra.md \
  docs/manual/ko/modules/bluetape4k-cassandra/session-lifecycle.md \
  docs/manual/en/modules/bluetape4k-cassandra/session-lifecycle.md
git commit -m "Teach Cassandra session ownership before query helpers" \
  -m "Constraint: The 1.11 manual must preserve the pre-PR-986 bootstrap behavior." \
  -m "Rejected: Describing current main | versioned manuals follow the release tag." \
  -m "Confidence: high" -m "Scope-risk: narrow" \
  -m "Tested: release manual links; Korean prose scan; git diff check"
```

### Task 2: Coroutine query와 multi-page Flow chapter를 작성한다

**Files:**

- Create: `docs/manual/ko/modules/bluetape4k-cassandra/coroutine-queries.md`
- Create: `docs/manual/en/modules/bluetape4k-cassandra/coroutine-queries.md`

- [ ] **Step 1: 1.11.0 async 계약을 읽는다**

Run:

```bash
git show 1.11.0:data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupport.kt
git show 1.11.0:data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupport.kt
git show 1.11.0:data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupportTest.kt
git show 1.11.0:data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupportUnitTest.kt
```

Expected: current page emission precedes `fetchNextPage().await()`, `CancellationException` is rethrown, and deprecated aliases point to `executeSuspending`/`prepareSuspending`.

- [ ] **Step 2: bilingual frontmatter와 section inventory를 작성한다**

```yaml
# ko
title: 코루틴 쿼리와 여러 페이지 읽기
description: Java Driver의 async query를 suspend 함수와 취소 가능한 Flow로 실행합니다.
manualId: bluetape4k-cassandra
chapterId: coroutine-queries

# en
title: Coroutine queries and multi-page reads
description: Execute Java Driver async queries as suspend functions and cancellation-aware Flow pipelines.
manualId: bluetape4k-cassandra
chapterId: coroutine-queries
```

본문은 API 선택 → 단일 query → prepared query → Flow paging → cancellation/error → capacity → tests → 이어 읽기 순서로 작성한다.

- [ ] **Step 3: 실행 가능한 suspend/Flow 예제를 넣는다**

Prepared query 예제는 다음 shape을 사용한다.

```kotlin
suspend fun findUser(session: CqlSession, id: Long): User? {
    val prepared = session.prepareSuspending(
        "SELECT id, name FROM users WHERE id = ?",
    )
    val result = session.executeSuspending(prepared.bind(id))
    return result.one()?.let { row ->
        User(row.getLong("id"), row.getString("name") ?: "")
    }
}
```

여러 page 예제는 initial query가 먼저 실행되고 collection이 현재/다음 page 순회를 시작한다는 경계를 정확히 표현한다.

```kotlin
suspend fun loadActiveUsers(session: CqlSession): List<User> {
    val result = session.executeSuspending(
        statementOf("SELECT id, name FROM users WHERE active = ?", true),
    )
    return result.asFlow { row ->
        User(row.getLong("id"), row.getString("name") ?: "")
    }.toList()
}
```

무제한 `toList()`는 결과 상한이 있는 query에만 사용하고, stream 처리에서는 `collect`로 downstream에 바로 넘기는 대안을 함께 설명한다.

- [ ] **Step 4: source link, parity와 문장을 검증한다**

Run:

```bash
ruby scripts/manual/validate_release_manuals.rb 1.11.0 6187173b58e8b4c5c435c145e00e94708f31ef75
rg -n "suspendExecute|suspendPrepare" docs/manual/{ko,en}/modules/bluetape4k-cassandra/coroutine-queries.md
rg -n "CancellationException|fetchNextPage|asFlow" docs/manual/{ko,en}/modules/bluetape4k-cassandra/coroutine-queries.md
git diff --check -- docs/manual/{ko,en}/modules/bluetape4k-cassandra/coroutine-queries.md
```

Expected: deprecated names appear only in migration text; both locales contain cancellation, paging and Flow contracts; release paths are valid.

- [ ] **Step 5: coroutine chapter를 커밋한다**

```bash
git add docs/manual/ko/modules/bluetape4k-cassandra/coroutine-queries.md \
  docs/manual/en/modules/bluetape4k-cassandra/coroutine-queries.md
git commit -m "Explain Cassandra paging as a coroutine contract" \
  -m "Constraint: Initial query execution and Flow page traversal must not be conflated." \
  -m "Rejected: Listing async helpers only | users need cancellation and capacity behavior." \
  -m "Confidence: high" -m "Scope-risk: narrow" \
  -m "Tested: release manual links; locale contract search; git diff check"
```

### Task 3: Row와 data mapping chapter를 작성한다

**Files:**

- Create: `docs/manual/ko/modules/bluetape4k-cassandra/rows-data-mapping.md`
- Create: `docs/manual/en/modules/bluetape4k-cassandra/rows-data-mapping.md`

- [ ] **Step 1: release source와 example inventory를 읽는다**

Run:

```bash
git show 1.11.0:data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/RowSupport.kt
git show 1.11.0:data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/data/GettableSupport.kt
git show 1.11.0:data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/data/SettableSupport.kt
git show 1.11.0:data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/mapper/EntitySupport.kt
git ls-tree -r --name-only 1.11.0 data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/examples/datatypes data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/mapper
```

Expected: name/index/identifier access, collection/UDT/tuple/codec examples와 `EntityHelper` prepare/bind anchors가 확인된다.

- [ ] **Step 2: bilingual frontmatter와 decision sections를 작성한다**

```yaml
# ko
title: Row와 Cassandra 값을 Kotlin 타입으로 옮기기
description: Row map 변환, 타입 안전 getter/setter, UDT·Tuple·Codec과 mapper helper의 경계를 설명합니다.
manualId: bluetape4k-cassandra
chapterId: rows-data-mapping

# en
title: Map rows and Cassandra values into Kotlin types
description: Choose among row maps, typed getters and setters, UDT and tuple codecs, and mapper helpers.
manualId: bluetape4k-cassandra
chapterId: rows-data-mapping
```

본문은 typed domain mapping을 기본값으로 두고 `toNamedMap()`은 동적 boundary와 진단에만 권장한다. `getStringOrEmpty`가 null과 빈 문자열을 합치므로 domain에서 둘을 구분해야 하면 nullable getter를 유지한다고 명시한다.

- [ ] **Step 3: typed row와 mapper helper 예제를 넣는다**

```kotlin
fun Row.toUser(): User = User(
    id = getLong("id"),
    name = getString("name") ?: error("users.name must not be null"),
    tags = getList<String>("tags")?.toList().orEmpty(),
)
```

`EntityHelper.prepareInsert`, `prepareInsertIfNotExists`, `bind`는 mapper-generated `EntityHelper<T>`가 있을 때 사용하는 선택지로 설명하고, mapper runtime이 artifact에 포함된다는 dependency boundary를 source/build와 연결한다.

- [ ] **Step 4: parity와 release path를 검증한다**

Run:

```bash
ruby scripts/manual/validate_release_manuals.rb 1.11.0 6187173b58e8b4c5c435c145e00e94708f31ef75
rg -n "toMap|toNamedMap|CqlIdentifier|UDT|Tuple|Codec|EntityHelper" docs/manual/{ko,en}/modules/bluetape4k-cassandra/rows-data-mapping.md
git diff --check -- docs/manual/{ko,en}/modules/bluetape4k-cassandra/rows-data-mapping.md
```

Expected: 두 locale에 모든 decision anchor가 있고 release validator와 diff check가 통과한다.

- [ ] **Step 5: mapping chapter를 커밋한다**

```bash
git add docs/manual/ko/modules/bluetape4k-cassandra/rows-data-mapping.md \
  docs/manual/en/modules/bluetape4k-cassandra/rows-data-mapping.md
git commit -m "Separate Cassandra typed mapping from dynamic row views" \
  -m "Constraint: Null and empty values must not be collapsed without an explicit domain decision." \
  -m "Rejected: Recommending Any maps as the default | stable domains should remain typed." \
  -m "Confidence: high" -m "Scope-risk: narrow" \
  -m "Tested: release manual links; locale API inventory; git diff check"
```

### Task 4: Statement와 QueryBuilder chapter를 작성한다

**Files:**

- Create: `docs/manual/ko/modules/bluetape4k-cassandra/statements-query-builder.md`
- Create: `docs/manual/en/modules/bluetape4k-cassandra/statements-query-builder.md`

- [ ] **Step 1: release statement/query builder API와 examples를 읽는다**

Run:

```bash
git show 1.11.0:data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt
git show 1.11.0:data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/querybuilder/QueryBuilderSupport.kt
git show 1.11.0:data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/querybuilder/RelationBuilderSupport.kt
git show 1.11.0:data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/querybuilder/TermSupport.kt
git ls-tree -r --name-only 1.11.0 data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/querybuilder
```

Expected: raw/positional/named `statementOf`, simple/bound/batch factories와 CRUD/schema QueryBuilder examples가 확인된다.

- [ ] **Step 2: bilingual frontmatter와 선택 표를 작성한다**

```yaml
# ko
title: Statement와 QueryBuilder 선택
description: Raw CQL, prepared·bound statement, batch와 QueryBuilder를 작업과 안전성 기준으로 선택합니다.
manualId: bluetape4k-cassandra
chapterId: statements-query-builder

# en
title: Choose statements and QueryBuilder APIs
description: Select raw CQL, prepared and bound statements, batches, or QueryBuilder by task and safety boundary.
manualId: bluetape4k-cassandra
chapterId: statements-query-builder
```

선택 표는 다음 결론을 사용한다.

| 상황                         | 선택                                          |
|------------------------------|-----------------------------------------------|
| 고정 CQL을 한 번 실행        | `statementOf`                                 |
| 같은 CQL을 다른 값으로 반복  | prepared + bound statement                    |
| 조건부 CRUD를 조립           | QueryBuilder                                  |
| 같은 partition의 원자적 묶음 | Cassandra semantics를 확인한 `BatchStatement` |
| 값이 섞인 문자열 조합        | 사용하지 않고 bind marker 사용                |

- [ ] **Step 3: prepared와 QueryBuilder 비교 예제를 작성한다**

Prepared example은 value를 문자열 interpolation하지 않는다.

```kotlin
val prepared = session.prepareSuspending(
    "UPDATE users SET name = ? WHERE id = ?",
)
val result = session.executeSuspending(prepared.bind(newName, userId))
```

QueryBuilder example은 bind marker를 사용한다.

```kotlin
val update = update("users")
    .setColumn("name", bindMarker("name"))
    .whereColumn("id").isEqualTo(bindMarker("id"))
    .build()
```

Raw snippet과 logged batch는 library가 성능·원자성 범위를 넓히지 않는다고 명시하고 Apache Cassandra semantics와 구분한다.

- [ ] **Step 4: source link와 locale parity를 검증한다**

Run:

```bash
ruby scripts/manual/validate_release_manuals.rb 1.11.0 6187173b58e8b4c5c435c145e00e94708f31ef75
rg -n "statementOf|Prepared|Bound|Batch|QueryBuilder|bindMarker" docs/manual/{ko,en}/modules/bluetape4k-cassandra/statements-query-builder.md
git diff --check -- docs/manual/{ko,en}/modules/bluetape4k-cassandra/statements-query-builder.md
```

Expected: 두 locale가 다섯 선택 항목과 안전성 경계를 모두 포함하고 release validator가 통과한다.

- [ ] **Step 5: statement chapter를 커밋한다**

```bash
git add docs/manual/ko/modules/bluetape4k-cassandra/statements-query-builder.md \
  docs/manual/en/modules/bluetape4k-cassandra/statements-query-builder.md
git commit -m "Choose Cassandra statement APIs by binding boundary" \
  -m "Constraint: Helper APIs do not broaden Cassandra batch or consistency guarantees." \
  -m "Rejected: String interpolation for values | bind markers preserve the data boundary." \
  -m "Confidence: high" -m "Scope-risk: narrow" \
  -m "Tested: release manual links; locale API inventory; git diff check"
```

### Task 5: 운영·테스트 chapter를 작성한다

**Files:**

- Create: `docs/manual/ko/modules/bluetape4k-cassandra/operations-testing.md`
- Create: `docs/manual/en/modules/bluetape4k-cassandra/operations-testing.md`

- [ ] **Step 1: release admin과 representative tests를 읽는다**

Run:

```bash
git show 1.11.0:data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CassandraAdmin.kt
git show 1.11.0:data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/AbstractCassandraTest.kt
git show 1.11.0:data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CassandraAdminTest.kt
git show 1.11.0:data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt
git show 1.11.0:data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupportTest.kt
```

Expected: create/drop/version admin behavior, container/session fixture, identity cache와 multi-page integration test anchors가 확인된다.

- [ ] **Step 2: bilingual frontmatter와 운영 sections를 작성한다**

```yaml
# ko
title: 운영 경계와 Testcontainers 검증
description: Keyspace 관리 side effect, 세션 종료, query·paging 진단과 Cassandra 통합 테스트를 설명합니다.
manualId: bluetape4k-cassandra
chapterId: operations-testing

# en
title: Operational boundaries and Testcontainers verification
description: Diagnose keyspace side effects, session shutdown, query paging, and Cassandra integration tests.
manualId: bluetape4k-cassandra
chapterId: operations-testing
```

본문은 admin side effect → 권한/설정 → 관찰 항목 → failure matrix → test 실행 → source/tests → 이어 읽기 순서로 작성한다.

- [ ] **Step 3: troubleshooting table과 test command를 정확히 넣는다**

표에는 다음 다섯 row가 반드시 있어야 한다.

```markdown
| 증상 | 먼저 확인할 경계 |
| --- | --- |
| bootstrap 인증 또는 연결 실패 | 1.11.0 admin session에 필요한 설정이 builderSupplier에 있는지 확인 |
| 같은 keyspace의 잘못된 session 재사용 | CqlSessionIdentity context에 connection/tenant 경계가 있는지 확인 |
| Flow가 일부 row만 반환 | collection cancellation, mapper exception, next-page fetch failure 확인 |
| 종료 후 connection이 남음 | direct session과 provider-owned session의 종료 책임 구분 |
| batch 지연 또는 timeout | partition, statement 수, consistency와 timeout 확인 |
```

영문 표는 같은 다섯 증상과 경계를 유지한다. 실행 명령은 다음과 같이 제공한다.

```bash
./gradlew :bluetape4k-cassandra:test --no-build-cache --no-configuration-cache
```

Testcontainers가 Docker runtime을 필요로 하고 heavy test는 순차 실행해야 한다고 명시한다.

- [ ] **Step 4: release path와 bilingual troubleshooting을 검증한다**

Run:

```bash
ruby scripts/manual/validate_release_manuals.rb 1.11.0 6187173b58e8b4c5c435c145e00e94708f31ef75
rg -n "bootstrap|CqlSessionIdentity|Flow|connection|batch" docs/manual/{ko,en}/modules/bluetape4k-cassandra/operations-testing.md
git diff --check -- docs/manual/{ko,en}/modules/bluetape4k-cassandra/operations-testing.md
```

Expected: 두 locale 모두 다섯 troubleshooting boundary를 포함하고 validator가 0 missing을 보고한다.

- [ ] **Step 5: operations chapter를 커밋한다**

```bash
git add docs/manual/ko/modules/bluetape4k-cassandra/operations-testing.md \
  docs/manual/en/modules/bluetape4k-cassandra/operations-testing.md
git commit -m "Make Cassandra operations and test boundaries explicit" \
  -m "Constraint: Keyspace bootstrap is an observable side effect requiring cluster permissions." \
  -m "Rejected: Treating docs-only work as unit-test-only | Cassandra examples depend on integration behavior." \
  -m "Confidence: high" -m "Scope-risk: narrow" \
  -m "Tested: release manual links; bilingual troubleshooting inventory; git diff check"
```

### Task 6: Manifest 등록과 Projects 전체 검증을 완료한다

**Files:**

- Modify: `docs/manual/manifest.yaml`
- Modify: `docs/manual/generated/manifest.json`
- Modify: `docs/superpowers/checklists/2026-07-13-cassandra-manual-checklist.md`

- [ ] **Step 1: Cassandra manifest entry에 chapter inventory를 등록한다**

`workshops: []` 다음에 정확히 다음 YAML을 추가한다.

```yaml
  chapters:
  - id: session-lifecycle
    en: en/modules/bluetape4k-cassandra/session-lifecycle.md
    ko: ko/modules/bluetape4k-cassandra/session-lifecycle.md
  - id: coroutine-queries
    en: en/modules/bluetape4k-cassandra/coroutine-queries.md
    ko: ko/modules/bluetape4k-cassandra/coroutine-queries.md
  - id: rows-data-mapping
    en: en/modules/bluetape4k-cassandra/rows-data-mapping.md
    ko: ko/modules/bluetape4k-cassandra/rows-data-mapping.md
  - id: statements-query-builder
    en: en/modules/bluetape4k-cassandra/statements-query-builder.md
    ko: ko/modules/bluetape4k-cassandra/statements-query-builder.md
  - id: operations-testing
    en: en/modules/bluetape4k-cassandra/operations-testing.md
    ko: ko/modules/bluetape4k-cassandra/operations-testing.md
```

`assets`는 추가하지 않는다.

- [ ] **Step 2: generated manifest를 갱신하고 script tests를 실행한다**

Run:

```bash
ruby scripts/manual/export_manifest.rb
ruby scripts/manual/validate_manuals_test.rb
ruby scripts/manual/export_manifest_test.rb
ruby scripts/manual/release_contract_test.rb
ruby scripts/manual/generate_manuals_test.rb
```

Expected: manifest snapshot이 작성되고 모든 Ruby test process가 0 failures, 0 errors로 종료한다.

- [ ] **Step 3: 실제 inventory와 1.11.0 source links를 검증한다**

Run:

```bash
ruby scripts/manual/validate_manuals.rb
ruby scripts/manual/export_manifest.rb --check
ruby scripts/manual/validate_release_manuals.rb 1.11.0 6187173b58e8b4c5c435c145e00e94708f31ef75
```

Expected: `Manuals are aligned.`, `Manual manifest snapshot is current.`, release validator의 `0 missing`이 출력된다.

- [ ] **Step 4: locale inventory와 한국어 naturalness를 검사한다**

Run:

```bash
find docs/manual/ko/modules/bluetape4k-cassandra -maxdepth 1 -name '*.md' -print | sort
find docs/manual/en/modules/bluetape4k-cassandra -maxdepth 1 -name '*.md' -print | sort
rg -n "~를 통해|~에 있어서|중요합니다|강력한|다양한 장점|할 수 있을 것으로" \
  docs/manual/ko/modules/bluetape4k-cassandra.md \
  docs/manual/ko/modules/bluetape4k-cassandra
git diff --check
```

Expected: KO/EN가 각각 같은 basename 5개를 출력하고 Korean prose search에 수정 대상이 없으며 diff check가 통과한다.

- [ ] **Step 5: Cassandra Testcontainers test를 단독 실행한다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cassandra:test --no-build-cache --no-configuration-cache
```

Expected: Gradle `BUILD SUCCESSFUL`, failed test 0. Docker/Testcontainers failure가 발생하면 환경 실패와 assertion failure를 구분하고 성공으로 간주하지 않는다.

- [ ] **Step 6: checklist의 Projects gate를 갱신한다**

`CAS-04`부터 `CAS-07`, `CG-07`의 Projects 부분을 fresh command 결과로 check한다. `CAS-08`, `CAS-09`, `E-05`, `E-06`, `CG-17`, `WF-05`는 Site 검증 전까지 unchecked로 둔다.

- [ ] **Step 7: Projects manual checkpoint를 커밋한다**

```bash
git add docs/manual/manifest.yaml docs/manual/generated/manifest.json \
  docs/superpowers/checklists/2026-07-13-cassandra-manual-checklist.md
git commit -m "Register Cassandra as a versioned multi-chapter manual" \
  -m "Constraint: Five bilingual chapters must share one release-compatible manifest inventory." \
  -m "Rejected: Site-only navigation | repository docs own the chapter contract." \
  -m "Confidence: high" -m "Scope-risk: narrow" \
  -m "Tested: Ruby manual tests and validators; 1.11.0 release links; Cassandra module test"
```

Run after commit:

```bash
git status --porcelain -- docs/manual scripts/manual/validate_release_manuals.rb
git rev-parse HEAD
```

Expected: scoped status is empty and a 40-character Projects source commit is printed.

### Task 7: 1.11 site snapshot을 refresh하고 browser에서 검증한다

**Repository:** `/Users/debop/work/bluetape4k/bluetape4k.github.io/.worktrees/feature-ecosystem-atlas-manual`

**Files:**

- Generated: `src/content/docs/ko/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra.md`
- Generated: `src/content/docs/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra.md`
- Generated: `src/content/docs/{ko/,}manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra/{session-lifecycle,coroutine-queries,rows-data-mapping,statements-query-builder,operations-testing}.md`
- Generated: `src/data/manual/bluetape4k-projects.versions.json`
- Generated: `src/data/manual/bluetape4k-projects.snapshot.json`
- Generated: `src/content/docs/{ko/,}manual/bluetape4k-projects/1.11/data.json`
- Modify in Projects after Site proof: `docs/superpowers/checklists/2026-07-13-cassandra-manual-checklist.md`

- [ ] **Step 1: Projects source와 release tag를 다시 고정한다**

Run in Projects worktree:

```bash
git status --porcelain -- docs/manual scripts/manual/validate_release_manuals.rb
git rev-parse HEAD
git rev-parse 1.11.0^{}
```

Expected: scoped status is empty; HEAD is the Task 6 checkpoint; tag resolves to `6187173b58e8b4c5c435c145e00e94708f31ef75`.

- [ ] **Step 2: 같은 minor release의 editorial snapshot을 refresh한다**

Run in Site worktree:

```bash
npm run sync:manual -- --refresh 1.11.0 \
  --source /Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feature-all-module-manuals
npm run sync:manual -- --check
npm run check:manual
```

Expected: refresh와 check가 exit 0. Version catalog의 `releaseCommit`은 `6187173b58e8b4c5c435c145e00e94708f31ef75`로 유지되고 `sourceCommit`만 Task 6 Projects checkpoint로 갱신된다.

- [ ] **Step 3: Site tests와 Astro build를 실행한다**

Run:

```bash
npm test
npm run build
```

Expected: Node manual/ecosystem tests가 모두 PASS하고 Astro check의 errors/warnings/hints가 0이며 build가 exit 0이다.

- [ ] **Step 4: generated route와 immutable source link를 검사한다**

Run:

```bash
test -f src/content/docs/ko/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra/session-lifecycle.md
test -f src/content/docs/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra/session-lifecycle.md
rg -n 'releaseRef: "1\.11\.0"|releaseCommit: "6187173b58e8b4c5c435c145e00e94708f31ef75"' \
  src/content/docs/ko/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra.md
rg -n 'github.com/bluetape4k/bluetape4k-projects/blob/1\.11\.0/' \
  src/content/docs/ko/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra/session-lifecycle.md
git diff --check
```

Expected: KO/EN chapter가 존재하고 source links는 authoring commit이 아니라 `blob/1.11.0/`을 사용하며 diff check가 통과한다.

- [ ] **Step 5: localhost에서 KO/EN route를 육안 검수한다**

Dev server가 없다면 Site worktree에서 다음 명령으로 시작한다.

```bash
npm run dev -- --host 127.0.0.1 --port 4323
```

다음 route를 desktop width와 좁은 width에서 확인한다.

```text
http://127.0.0.1:4323/ko/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra/
http://127.0.0.1:4323/ko/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra/session-lifecycle/
http://127.0.0.1:4323/ko/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra/coroutine-queries/
http://127.0.0.1:4323/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra/
http://127.0.0.1:4323/manual/bluetape4k-projects/1.11/modules/bluetape4k-cassandra/operations-testing/
```

Expected: navigation에 5개 chapter가 순서대로 보이고 code block이 잘리지 않으며 horizontal overflow, broken link와 browser console error가 없다. 한국어 문장을 실제 화면에서 읽어 번역체나 지나치게 긴 문장을 고친다.

- [ ] **Step 6: Projects checklist를 최종 증거로 닫는다**

Projects worktree에서 `CAS-08`, `CAS-09`, `E-05`, `E-06`, `CG-17`, `WF-05`를 Site sync digest, test/build 결과와 browser route evidence로 check한다. `Required checks: X/Y; N/A: N; Blocked: N`을 다시 계산하고 unchecked ID가 없음을 기록한다.

- [ ] **Step 7: Projects checklist와 Site snapshot을 각각 커밋한다**

Projects worktree:

```bash
git add docs/superpowers/checklists/2026-07-13-cassandra-manual-checklist.md
git commit -m "Close the Cassandra manual verification ledger" \
  -m "Constraint: Completion requires release links, Testcontainers, snapshot, build, and browser evidence." \
  -m "Rejected: Reporting generated files only | the checklist retains the proof chain." \
  -m "Confidence: high" -m "Scope-risk: narrow" \
  -m "Tested: Checklist totals reconcile with Projects and Site results"
```

Site worktree:

```bash
git add src/content/docs src/data/manual
git commit -m "Refresh the Cassandra manual in the 1.11 catalog" \
  -m "Constraint: The release commit remains immutable while editorial source advances." \
  -m "Rejected: Creating a new version for documentation-only changes | minor snapshots support guarded refresh." \
  -m "Confidence: high" -m "Scope-risk: narrow" \
  -m "Tested: manual sync/check; snapshot validation; Node tests; Astro build; browser routes"
```

Do not push either repository.

## Self-review

- Spec coverage: landing, 5 bilingual chapters, 1.11.0 cache/bootstrap distinction, central BOM, Korean naturalness, manifest, Cassandra test, deterministic refresh, immutable release links, build와 browser QA가 Task 1–7에 연결된다.
- Placeholder scan: 임시 표식, 비어 있는 section, 다른 task에 구현을 떠넘기는 문장과 미정 파일 경로가 없다.
- Type/path consistency: chapter IDs는 `session-lifecycle`, `coroutine-queries`, `rows-data-mapping`, `statements-query-builder`, `operations-testing`으로 spec, paths, frontmatter와 manifest에서 동일하다.
- Scope check: production Kotlin, diagrams, blog expansion, push/PR/merge/deploy는 모든 task에서 제외된다.
