# NetCDF 작업 경계와 진행 상태 조회 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** timeout 이후에도 보존한 `fileId`로 progress를 진단하고, lease 취득 뒤 cancellation·파일 교체·권한 경계를 결정적으로 검증하는 additive NetCDF 2.0 API를 제공한다.

**Architecture:** 기존 `registerFile()`과 `importGridValues()`를 유지하고 `findImportProgress()`만 추가한다. public 3-인자 constructor는 그대로 두며, spatial 첫 preflight transaction의 lease fence 뒤에 internal checkpoint를 주입해 cancellation rollback을 테스트한다. allowed-root·인증·인가·tenant 정책과 executor lifecycle은 caller 책임으로 두고 KDoc/README EN/KO와 consumer fixture에서 계약을 고정한다.

**Tech Stack:** Kotlin 2.4, Java 25 virtual threads, Exposed JDBC, PostgreSQL/PostGIS Testcontainers, NetCDF-Java, Micrometer, JUnit 5, Kluent/bluetape assertions, Gradle 9.7

---

- **이슈**: [#1561](https://github.com/bluetape4k/bluetape4k-projects/issues/1561)
- **승인된 사양**: `docs/superpowers/specs/2026-08-30-issue-1561-netcdf-operation-boundary-design.md`
- **브랜치**: `feat/issue-1561-netcdf-operation-boundary`
- **기준**: `origin/develop@9831a513f9b81e53f505fadd2e4546b8ea8cf6a8`
- **작업 방식**: 이 세션에서 inline execution, Testcontainers 명령은 한 번에 하나씩 순차 실행
- **외부 side effect**: 승인된 branch push와 PR 생성만 허용, merge·release·publish·cleanup 금지

## 1. 파일 구조와 책임

| 파일 | 작업 | 책임 |
|---|---|---|
| `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt` | 수정 | progress 조회·metric, 기존 public constructor 보존, checkpoint 호출 |
| `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/ImportCheckpoint.kt` | 생성 | spatial cancellation용 internal fun interface와 no-op |
| `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt` | 수정 | progress, timeout, post-lease cancel/resume, replacement, unreadable-file 통합 검증 |
| `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfFileGuardTest.kt` | 수정 | URI, control, directory, symlink path 회귀 검증 |
| `utils/science/src/test/kotlin/consumer/fixture/NetCdfPublicApiSourceCompatibilityTest.kt` | 생성 | 외부 package public API compile, sealed `else`, caller timeout 정책 |
| `utils/science/README.md` | 수정 | English timeout lifecycle, trust, metric, migration, quarantine |
| `utils/science/README.ko.md` | 수정 | 한국어 locale parity |
| `docs/lessons/2026-08-30-issue-1561-netcdf-operation-boundary.md` | 생성 | 구현·검증·review에서 얻은 재사용 가능한 교훈 |
| `docs/review/issue-1561-netcdf-operation-boundary-review.md` | 생성 | 최종 inline·독립 review의 P0-P3 통합 결과 |

`NetCdfImportProgressRepository`, schema, model, `NetCdfException`, Gradle 설정은 변경하지 않는다.

## 2. 수용 기준 추적

| 수용 기준 | 구현 task | 검증 |
|---|---|---|
| timeout 후 progress 식별자 보존 | Task 2, Task 4 | pre/post-admission tests, README example |
| progress service API | Task 1 | null/status/blank/metric tests |
| path/auth/tenant/fingerprint 책임 | Task 3, Task 4 | path tests, unreadable test, KDoc/README parity |
| 2.0 sealed subtype migration | Task 4 | external-package `else` fixture, docs |
| timeout/resume | Task 2 | cancel/rollback/active lease/expiry/resume |
| file replacement | Task 3 | atomic inode replacement → `FileChanged`, no progress |
| permission boundary | Task 3, Task 4 | POSIX unreadable `FileOpen`, consumer auth negative-test 책임 |
| 진단 개선 | Task 1, Task 4 | low-cardinality lookup metric and caller alert contract |

## 3. 위험 예측과 대응

| 위험 | 신호 | 완화 | rollback/rerun |
|---|---|---|---|
| constructor ABI 파손 | reflection/`javap`에 3-인자 descriptor 없음 | primary constructor 수정 금지, internal 4-인자 secondary만 추가 | Task 2 revert, compile/ABI 재실행 |
| checkpoint가 production 의미 변경 | 두 번 이상 호출, rank-1 영향, write 뒤 block | 첫 spatial preflight `touchLease` 뒤·`readTile` 전 1회 | checkpoint diff revert, cancellation tests 재실행 |
| cancellation test hang | latch 또는 executor 5초 초과 | 모든 대기 bounded, `finally`에서 release·`shutdownNow` | 타깃 단일 테스트로 원인 확인 후 전체 재실행 |
| unreadable test 환경 의존 | root 실행, POSIX 미지원 | Ubuntu/macOS non-root 계약, permission `finally` 복구 | 환경 증거를 PENDING으로 보고; skip을 PASS로 사용 금지 |
| metric cardinality 증가 | tag에 ID/path/tenant 포함 | status allowlist 5개만 사용 | metric test와 source review 재실행 |
| 자동 retry storm | `FAILED`/local clock만 보고 반복 | 기본 자동 retry 0회, `ImportAlreadyRunning` authoritative, `RECOVERY_REQUIRED` | consumer fixture와 README review 재실행 |
| 파일 교체 테스트 flake | inode/size/mtime 동일 | 새 inode + atomic move + 크기 차이 | 타깃 파일 테스트 재실행 |
| raw progress 외부 노출 | response DTO에 lease/error/timestamp 존재 | caller-owned DTO allowlist와 민감 필드 부재를 fixture로 고정 | consumer fixture와 README review 재실행 |
| `fileId` 권한 토큰 오용 | authorization 전에 service 호출 | register/import/progress/retry별 authorize-first negative test | consumer fixture 단일 테스트 재실행 |

## Task 0: 승인된 사양과 계획 checkpoint

**Complexity:** 낮음
**Dependency:** 구현 전 필수
**Files:**
- Add: `docs/superpowers/specs/2026-08-30-issue-1561-netcdf-operation-boundary-design.md`
- Add: `docs/superpowers/plans/2026-08-30-issue-1561-netcdf-operation-boundary-plan.md`

- [ ] **Step 1: 문서 무결성을 검사한다**

Run:

```bash
node ~/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
  docs/superpowers/specs/2026-08-30-issue-1561-netcdf-operation-boundary-design.md \
  docs/superpowers/plans/2026-08-30-issue-1561-netcdf-operation-boundary-plan.md
git diff --check
```

Expected: terminology findings 0, whitespace error 0.

- [ ] **Step 2: 사용자 계획 승인 뒤 사양과 계획만 Lore commit한다**

```bash
git add \
  docs/superpowers/specs/2026-08-30-issue-1561-netcdf-operation-boundary-design.md \
  docs/superpowers/plans/2026-08-30-issue-1561-netcdf-operation-boundary-plan.md
git commit -m "NetCDF 작업 경계를 구현 전에 고정한다

Constraint: 기존 public constructor와 blocking API를 유지한다
Rejected: 작업 handle 추가 | 기존 fileId와 variableName으로 충분하다
Confidence: high
Scope-risk: moderate
Directive: 등록은 deadline 밖에서 완료하고 import만 취소 대상으로 둔다
Tested: 문서 terminology audit와 git diff --check
Not-tested: 구현과 Gradle 검증은 후속 task에서 수행한다"
```

Expected: spec/plan 두 파일만 포함한 commit.

## Task 1: Progress 조회 API와 low-cardinality metric

**Complexity:** 중간
**Dependency:** Task 0
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`
**Files:**
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`
- Modify: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt`

- [ ] **Step 1: 존재하지 않는 progress API를 호출하는 RED tests를 추가한다**

추가할 핵심 형태:

```kotlin
@Test
fun `findImportProgress returns null before import`(@TempDir dir: Path) {
    val path = NetCdfSampleWriter.writeSample(dir.resolve("progress-missing.nc"), rank = 2)
    val fileId = service.registerFile(path.absolutePathString())

    service.findImportProgress(fileId, "temperature").shouldBeNull()
    meterRegistry.find("netcdf.import.progress.lookup")
        .tag("status", "missing")
        .counter()
        .shouldNotBeNull()
}

@Test
fun `findImportProgress returns completed row without mutation`(@TempDir dir: Path) {
    val path = NetCdfSampleWriter.writeSample(dir.resolve("progress-completed.nc"), rank = 2)
    val fileId = service.registerFile(path.absolutePathString())
    service.importGridValues(fileId, "temperature")
    val before = transaction(db) { progressRepo.findByFileAndVariable(fileId, "temperature") }

    val found = service.findImportProgress(fileId, "temperature")

    found shouldBeEqualTo before
    meterRegistry.find("netcdf.import.progress.lookup")
        .tag("status", "completed")
        .counter()
        .shouldNotBeNull()
}

@Test
fun `findImportProgress rejects blank variable name`() {
    assertFailsWith<IllegalArgumentException> {
        service.findImportProgress(1L, " ")
    }
}
```

- [ ] **Step 2: compile RED를 확인한다**

Run:

```bash
./gradlew :bluetape4k-science:compileTestKotlin
```

Expected: `Unresolved reference 'findImportProgress'`로 FAIL.

- [ ] **Step 3: 최소 API와 metric을 구현한다**

`NetCdfCatalogService`에 추가:

```kotlin
/**
 * `(fileId, variableName)` 임포트 진행 상태를 조회합니다.
 *
 * 조회는 lease나 상태를 변경하지 않습니다. [fileId]는 권한 토큰이 아니므로 caller는
 * 호출 전에 파일 소유권과 tenant/job binding을 다시 검증해야 합니다.
 */
fun findImportProgress(fileId: Long, variableName: String): NetCdfImportProgress? {
    require(variableName.isNotBlank()) { "variableName must not be blank" }
    val progress = transaction { progressRepo.findByFileAndVariable(fileId, variableName) }
    meterRegistry?.counter(
        "netcdf.import.progress.lookup",
        "status",
        when (progress?.status) {
            null -> "missing"
            NetCdfImportStatus.PENDING -> "pending"
            NetCdfImportStatus.IN_PROGRESS -> "in-progress"
            NetCdfImportStatus.COMPLETED -> "completed"
            NetCdfImportStatus.FAILED -> "failed"
        },
    )?.increment()
    return progress
}
```

- [ ] **Step 4: 타깃 tests GREEN을 확인한다**

Run sequentially:

```bash
repo-test-summary -- ./gradlew :bluetape4k-science:test \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest'
```

Expected: all selected tests PASS, skipped=0.

- [ ] **Step 5: Task 1 diff를 review한다**

```bash
git diff -- \
  utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt \
  utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt
```

Expected: repository/schema/public model 변경 없음, metric tag allowlist만 존재.

## Task 2: 실제 lease 취득 뒤 cancellation과 resume

**Complexity:** 높음
**Dependency:** Task 1
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`
**Files:**
- Create: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/ImportCheckpoint.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`
- Modify: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt`

- [ ] **Step 1: public constructor ABI와 checkpoint lifecycle RED tests를 추가한다**

핵심 ABI assertion:

```kotlin
@Test
fun `public three argument constructor remains available`() {
    NetCdfCatalogService::class.java.getConstructor(
        NetCdfFileRepository::class.java,
        NetCdfImportProgressRepository::class.java,
        MeterRegistry::class.java,
    ).shouldNotBeNull()
}
```

핵심 post-admission test 흐름:

```kotlin
val leaseTouched = CountDownLatch(1)
val releaseCheckpoint = CountDownLatch(1)
val workerFailure = AtomicReference<Throwable?>()
val checkpointThread = AtomicReference<Thread?>()
val cancellableService = NetCdfCatalogService(
    fileRepo,
    progressRepo,
    meterRegistry,
    ImportCheckpoint {
        checkpointThread.set(Thread.currentThread())
        leaseTouched.countDown()
        check(releaseCheckpoint.await(5, TimeUnit.SECONDS)) {
            "netcdf post-lease checkpoint timed out"
        }
    },
)
val executor = Executors.newSingleThreadExecutor { task ->
    Thread(task, "netcdf-post-lease-cancel")
}
val task = executor.submit {
    try {
        cancellableService.importGridValues(fileId, "temperature")
    } catch (failure: Throwable) {
        workerFailure.set(failure)
        throw failure
    }
}
try {
    leaseTouched.await(5, TimeUnit.SECONDS).shouldBeTrue()
    val committedBeforeCancel = transaction(db) {
        progressRepo.findByFileAndVariable(fileId, "temperature")
    }.shouldNotBeNull()
    task.cancel(true).shouldBeTrue()
    executor.shutdownNow()
    executor.awaitTermination(5, TimeUnit.SECONDS).shouldBeTrue()
    (workerFailure.get() is InterruptedException).shouldBeTrue()

    val afterCancel = service.findImportProgress(fileId, "temperature").shouldNotBeNull()
    afterCancel.status shouldBeEqualTo NetCdfImportStatus.IN_PROGRESS
    afterCancel.leaseExpiresAt shouldBeEqualTo committedBeforeCancel.leaseExpiresAt
    afterCancel.updatedAt shouldBeEqualTo committedBeforeCancel.updatedAt
    afterCancel.lastSliceIdx shouldBeEqualTo committedBeforeCancel.lastSliceIdx
    countGridRows(fileId) shouldBeEqualTo 0L
    assertFailsWith<NetCdfException.ImportAlreadyRunning> {
        service.importGridValues(fileId, "temperature")
    }
    forceExpireLease(fileId, "temperature")
    service.importGridValues(fileId, "temperature")
    service.findImportProgress(fileId, "temperature")?.status
        .shouldBeEqualTo(NetCdfImportStatus.COMPLETED)
} finally {
    releaseCheckpoint.countDown()
    task.cancel(true)
    executor.shutdownNow()
    executor.awaitTermination(5, TimeUnit.SECONDS).shouldBeTrue()
    checkpointThread.get()?.isAlive.shouldBeFalse()
}
```

pre-admission test는 import 호출 전 latch에서 task를 막아 `Future.get(... )`의
`TimeoutException`, cancel, 종료, progress `null`, 동일 `fileId` 정상 import를 검증한다.
모든 latch 대기는 5초 bounded이며 `finally`에서 release, cancel, `shutdownNow`, 두 번째
bounded `awaitTermination`, captured thread `isAlive=false`를 공통으로 확인한다.

- [ ] **Step 2: compile RED를 확인한다**

```bash
./gradlew :bluetape4k-science:compileTestKotlin
```

Expected: `ImportCheckpoint`와 4-인자 internal constructor가 없어 FAIL.

- [ ] **Step 3: internal checkpoint를 최소 구현한다**

`ImportCheckpoint.kt`:

```kotlin
package io.bluetape4k.science.exposed.service

internal fun interface ImportCheckpoint {
    fun afterLeaseTouched()

    companion object {
        val NONE: ImportCheckpoint = ImportCheckpoint {}
    }
}
```

`NetCdfCatalogService`의 public primary constructor는 그대로 두고 다음 property와 secondary
constructor만 추가한다.

```kotlin
private var importCheckpoint: ImportCheckpoint = ImportCheckpoint.NONE

internal constructor(
    fileRepo: NetCdfFileRepository,
    progressRepo: NetCdfImportProgressRepository,
    meterRegistry: MeterRegistry?,
    importCheckpoint: ImportCheckpoint,
): this(fileRepo, progressRepo, meterRegistry) {
    this.importCheckpoint = importCheckpoint
}
```

spatial preflight loop를 `forEachIndexed`로 바꾸고 첫 tile에서만 호출한다.

```kotlin
tiles.forEachIndexed { tileIndex, tile ->
    checkNotInterrupted()
    transaction {
        checkNotInterrupted()
        context.lease.expiresAt = progressRepo.touchLease(
            context.progressId,
            context.lease.expiresAt,
            leaseTtl = LEASE_TTL,
        )
        if (tileIndex == 0) {
            importCheckpoint.afterLeaseTouched()
        }
        checkNotInterrupted()
        val data = readTile(prepared.variable, layout, tile, timeIdx, levelIdx)
        // 기존 scan 유지
    }
}
```

- [ ] **Step 4: cancellation tests GREEN을 확인한다**

```bash
repo-test-summary -- ./gradlew :bluetape4k-science:test \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest'
```

Expected: timeout/cancel/resume와 기존 service tests PASS, skipped=0, executor leak 없음.

- [ ] **Step 5: JVM constructor descriptor를 확인한다**

```bash
./gradlew :bluetape4k-science:compileKotlin
javap -classpath utils/science/build/classes/kotlin/main \
  io.bluetape4k.science.exposed.service.NetCdfCatalogService
```

Expected: 기존 3-인자 public constructor가 출력되고 새 public primary replacement가 없음.

## Task 3: 파일 교체와 path/permission 경계 회귀

**Complexity:** 중간
**Dependency:** Task 2
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`
**Files:**
- Modify: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt`
- Modify: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfFileGuardTest.kt`

- [ ] **Step 1: 기존 동작을 잠그는 regression tests를 먼저 추가한다**

파일 교체 test:

```kotlin
@Test
fun `registered file replacement is rejected before progress creation`(@TempDir dir: Path) {
    val original = NetCdfSampleWriter.writeSample(dir.resolve("replace.nc"), rank = 2)
    val fileId = service.registerFile(original.absolutePathString())
    val replacement = NetCdfSampleWriter.writeSample(dir.resolve("replacement.nc"), rank = 3)
    Files.move(
        replacement,
        original,
        StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING,
    )

    assertFailsWith<NetCdfException.FileChanged> {
        service.importGridValues(fileId, "temperature")
    }
    service.findImportProgress(fileId, "temperature").shouldBeNull()
}
```

POSIX unreadable test:

```kotlin
@Test
fun `registerFile rejects unreadable POSIX file`(@TempDir dir: Path) {
    val path = NetCdfSampleWriter.writeSample(dir.resolve("unreadable.nc"), rank = 2)
    val original = Files.getPosixFilePermissions(path)
    try {
        Files.setPosixFilePermissions(path, setOf(PosixFilePermission.OWNER_WRITE))
        assertFailsWith<NetCdfException.FileOpen> {
            service.registerFile(path.absolutePathString())
        }
    } finally {
        Files.setPosixFilePermissions(path, original)
    }
}
```

guard tests에는 URI, control 문자, directory 거부를 추가하고 기존 final/parent symlink
검증을 유지한다.

- [ ] **Step 2: regression tests를 실행한다**

```bash
repo-test-summary -- ./gradlew :bluetape4k-science:test \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest' \
  --tests 'io.bluetape4k.science.exposed.service.internal.NetCdfFileGuardTest'
```

Expected: 새 tests와 기존 tests PASS, skipped=0. 기존 production guard가 이미 계약을
충족하면 test-only diff를 유지하고 불필요한 source 수정을 추가하지 않는다.

- [ ] **Step 3: 환경과 cleanup을 확인한다**

```bash
id -u
git status --short
```

Expected: uid 0이 아님, permission이 복구돼 temp cleanup 성공, 의도한 파일만 변경.

## Task 4: 외부 caller compile fixture와 KDoc/README EN/KO

**Complexity:** 중간
**Dependency:** Task 1~3 GREEN
**Pattern skills:** `bluetape-kotlin-patterns`, `bluetape-writer`
**Files:**
- Create: `utils/science/src/test/kotlin/consumer/fixture/NetCdfPublicApiSourceCompatibilityTest.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`
- Modify: `utils/science/README.md`
- Modify: `utils/science/README.ko.md`

- [ ] **Step 1: external-package public-only fixture를 추가한다**

핵심 compile contract:

```kotlin
package consumer.fixture

internal fun readProgress(
    catalog: NetCdfCatalogService,
    fileId: Long,
    variableName: String,
): NetCdfImportProgress? = catalog.findImportProgress(fileId, variableName)

internal fun classify(exception: NetCdfException): String = when (exception) {
    is NetCdfException.FileChanged -> "file-changed"
    is NetCdfException.CorruptProgress -> "corrupt-progress"
    is NetCdfException.ImportAlreadyRunning -> "running"
    else -> "unhandled-netcdf"
}
```

같은 fixture에 library public API로 추가하지 않는 caller-owned DTO를 둔다.

```kotlin
private data class NetCdfImportStatusResponse(
    val status: String,
    val lastCommittedSlice: Long?,
    val outcome: String,
)
```

변환 함수는 상태 allowlist, `lastSliceIdx`, outcome만 복사한다. reflection으로 response의
property 이름이 정확히 세 개인지 확인하고 `errorMessage`, `leaseExpiresAt`, `startedAt`,
`completedAt`, `updatedAt`이 없음을 검증한다. raw `NetCdfImportProgress`를 HTTP/RPC serializer로
전달하는 example은 두 README와 fixture 어디에도 두지 않는다.

test-only caller adapter는 `authorize(operation, actor, tenant, job, fileIdOrPath)`가 성공한
뒤에만 service lambda를 호출한다. register/import/progress/retry 네 operation 각각에서
cross-tenant, unauthorized, allowed-root 밖 path를 거부했을 때 service lambda invocation
count가 0인지 negative test로 고정한다. 이 adapter는 library public API가 아니라 consumer
integration pattern의 compile/runtime fixture이며, 실제 application identity store와 정책은
이 저장소 범위 밖이다.

caller lifecycle policy는 다음 표를 exact parameterized test로 고정한다.

| worker/progress/authoritative signal | outcome | caller action |
|---|---|---|
| `terminated=false` | `RECOVERY_REQUIRED` | worker 격리, stuck alert, retry 금지 |
| progress `COMPLETED` | `COMPLETED` | 완료 처리 |
| 첫 `ImportAlreadyRunning` | `RUNNING` | DB clock 결과를 신뢰하고 retry 금지 |
| `PENDING`, `FAILED`, row 없음, 판정 불가능한 `IN_PROGRESS` | `RETRY_REVIEW` | 자동 retry 0회, 운영 검토 |
| 반복 `ImportAlreadyRunning` 또는 attempt 상한 소진 | `RECOVERY_REQUIRED` | retry 중지와 alert |
| non-transient typed failure | `RECOVERY_REQUIRED` | 입력/운영 조건 수정 전 retry 금지 |

각 분기는 retry invocation count, `netcdf.import.timeout`,
`netcdf.import.worker.stuck`, `netcdf.import.retry.exhausted` alert 이름과 correlation ID 존재,
raw path/tenant/error의 metric tag 부재를 함께 검증한다. `FAILED`만으로 자동 retry하지 않으며,
local clock의 lease 비교는 어느 fixture에도 넣지 않는다.

- [ ] **Step 2: public KDoc을 사양과 맞춘다**

class KDoc과 세 public 메서드는 다음을 설명한다.

- 모두 blocking이며 등록은 deadline 밖에서 완료
- timeout은 cooperative cancellation이고 worker 종료 확인 전 retry 금지
- progress는 진단용이며 local clock만으로 lease 만료 판정 금지
- `fileId`는 권한 토큰이 아니며 매 작업/retry authorization 필요
- service guard와 caller-owned allowed-root/authn/authz/tenant/immutable-file 경계
- progress model 직접 외부 반환 금지, caller-owned DTO allowlist와 redaction

- [ ] **Step 3: README EN/KO를 같은 순서로 갱신한다**

두 locale에 다음 구조를 동일하게 둔다.

1. 등록을 task 밖에서 수행하는 code example
2. import-only future, failure `AtomicReference`, timeout cancel, 30초 bounded termination
3. `awaitTermination=false => RECOVERY_REQUIRED`, 자동 retry 0회
4. progress/worker exception 상태표
5. trust boundary와 매 작업 authorization
6. fingerprint/hostile-writer/quarantine
7. metric allowlist와 caller alerts
8. sealed subtype `else` migration
9. API 표의 `findImportProgress()`

timeout code example은 두 README에서
`<!-- netcdf-timeout-example:start -->`/`<!-- netcdf-timeout-example:end -->` marker로 감싼다.
marker 내부는 `registerFile()`을 `submit` 전에 실행해 `fileId`를 보존하고, import-only future,
`AtomicReference` failure capture, timeout `cancel(true)`, 30초 bounded termination,
termination 실패의 `RECOVERY_REQUIRED`, worker 종료 후 progress 조회를 모두 포함한다.
두 locale의 marker 내부 code는 byte-for-byte 같고 설명과 상태표만 각 언어로 작성한다.
기존 “stable sealed-exception contract” 표현은 source-compatible `else` migration 안내로 교체한다.

두 README의 operator checklist는 stop retry → worker 격리 → progress/partial rows 보존 →
correlation ID 기반 alert → 입력·권한·tenant 확인 → 승인된 수동 cleanup 순서를 포함한다.

- [ ] **Step 4: compile과 locale token parity를 검증한다**

```bash
./gradlew :bluetape4k-science:compileTestKotlin
for readme in utils/science/README.md utils/science/README.ko.md; do
  rg -n 'findImportProgress|RECOVERY_REQUIRED|ImportAlreadyRunning|FileChanged|CorruptProgress|netcdf.import.progress.lookup|allowed[- ]root|tenant|else' "$readme"
done
awk '/<!-- netcdf-timeout-example:start -->/{capture=1;next}/<!-- netcdf-timeout-example:end -->/{capture=0}capture' \
  utils/science/README.md > /private/tmp/issue-1561-timeout-en.txt
awk '/<!-- netcdf-timeout-example:start -->/{capture=1;next}/<!-- netcdf-timeout-example:end -->/{capture=0}capture' \
  utils/science/README.ko.md > /private/tmp/issue-1561-timeout-ko.txt
test -s /private/tmp/issue-1561-timeout-en.txt
diff -u /private/tmp/issue-1561-timeout-en.txt /private/tmp/issue-1561-timeout-ko.txt
! rg -ni 'stable sealed[- ]exception contract' utils/science/README.md utils/science/README.ko.md
node ~/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
  utils/science/README.ko.md \
  docs/superpowers/specs/2026-08-30-issue-1561-netcdf-operation-boundary-design.md \
  docs/superpowers/plans/2026-08-30-issue-1561-netcdf-operation-boundary-plan.md
git diff --check
```

Expected: compile PASS, marker code parity PASS, deprecated sealed 표현 0건, 두 locale 필수 token
존재, terminology findings 0, whitespace error 0.
consumer fixture는 네 operation의 authorize-first negative test와 response 민감 필드 부재를 PASS해야 한다.

## Task 5: 비례 검증과 spec/plan verifier

**Complexity:** 중간
**Dependency:** Task 1~4 GREEN
**Files:** all changed files

- [ ] **Step 1: 변경 test subset을 순차 실행한다**

```bash
repo-test-summary -- ./gradlew :bluetape4k-science:test \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest' \
  --tests 'io.bluetape4k.science.exposed.service.internal.NetCdfFileGuardTest' \
  --tests 'consumer.fixture.NetCdfPublicApiSourceCompatibilityTest'
```

Expected: selected tests PASS, skipped=0.

- [ ] **Step 2: 전체 science module test를 한 번 실행한다**

```bash
repo-test-summary -- ./gradlew :bluetape4k-science:test
```

Expected: all tests PASS, failed=0, skipped=0. Testcontainers invocation은 다른 module/worktree와
동시에 실행하지 않는다.

- [ ] **Step 3: static validation을 실행한다**

```bash
./gradlew :bluetape4k-science:detekt :bluetape4k-science:compileKotlin \
  :bluetape4k-science:compileTestKotlin
git diff --check
```

Expected: all tasks PASS, diagnostics 0, whitespace error 0.

- [ ] **Step 4: repository hazard N/A를 증명한다**

```bash
{
  git diff --name-only origin/develop...HEAD
  git diff --name-only
  git ls-files --others --exclude-standard
} | sort -u
git status --short
```

Expected: `utils/science` source/test/docs와 승인된 spec/plan/review/lesson만 변경. module move,
settings/BOM/catalog/schema/workflow/dependency가 없어 registration·catalog·Nightly hazard N/A.

- [ ] **Step 5: verifier traceability를 완료한다**

사양 §11의 여섯 수용 기준과 이 계획 §2의 task/test를 대조한다. 누락, stale command,
unexpected public ABI, skipped test가 있으면 구현 task로 돌아가고 Step 5를 처음부터 재실행한다.

## Task 6: inline review, 독립 review, lesson, commit과 PR

**Complexity:** 높음
**Dependency:** Task 5 PASS
**Skills:** `verification-before-completion`, `requesting-code-review`, `bluetape-writer`
**Files:**
- Create: `docs/review/issue-1561-netcdf-operation-boundary-review.md`
- Create: `docs/lessons/2026-08-30-issue-1561-netcdf-operation-boundary.md`
- Modify: PR body only after local evidence PASS

- [ ] **Step 1: inline review를 먼저 수행한다**

다음 관점으로 exact diff를 직접 읽는다.

- cancellation/transaction rollback과 dataset/executor lifecycle
- path/auth/tenant/redaction과 metric cardinality
- public constructor/method ABI와 sealed fallback
- README EN/KO example correctness와 상태표 parity
- flaky timing, POSIX cleanup, atomic replacement

P0/P1을 발견하면 코드와 test를 수정하고 Task 5를 처음부터 재실행한다.

- [ ] **Step 2: 6개 독립 code-review perspective와 통합 review를 수행한다**

performance, stability, security, Ops, developer/API, user/caller lane을 read-only로 실행한다.
최신 P0=0/P1=0까지 affected lane만 재실행하고, 통합 결과를
`docs/review/issue-1561-netcdf-operation-boundary-review.md`에 기록한다.

- [ ] **Step 3: lesson을 작성하고 writer gate를 통과한다**

lesson은 context, decision, outcome, verification, review miss, future guard를 포함한다.

```bash
node ~/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
  docs/review/issue-1561-netcdf-operation-boundary-review.md \
  docs/lessons/2026-08-30-issue-1561-netcdf-operation-boundary.md
git diff --check
```

Expected: findings 0, writer SPW-01~05 PASS.

- [ ] **Step 4: 구현 commit을 만든다**

```bash
git add \
  utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt \
  utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/ImportCheckpoint.kt \
  utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt \
  utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfFileGuardTest.kt \
  utils/science/src/test/kotlin/consumer/fixture/NetCdfPublicApiSourceCompatibilityTest.kt \
  utils/science/README.md \
  utils/science/README.ko.md \
  docs/review/issue-1561-netcdf-operation-boundary-review.md \
  docs/lessons/2026-08-30-issue-1561-netcdf-operation-boundary.md
git commit -m "NetCDF timeout 뒤 진단 가능한 경계를 제공한다

Constraint: 기존 blocking API와 3-인자 constructor ABI를 유지한다
Rejected: public 작업 handle 추가 | 기존 fileId와 variableName으로 충분하다
Confidence: high
Scope-risk: moderate
Directive: worker 종료와 authorization을 확인하기 전에는 같은 import를 재시도하지 않는다
Tested: science 전체 test, detekt, compile, ABI, locale parity, inline 및 독립 review
Not-tested: 실제 consumer별 authN/authZ와 tenant 정책은 caller 책임이다"
```

- [ ] **Step 5: exact head를 push하고 승인된 PR을 생성한다**

```bash
pr_body=/private/tmp/issue-1561-pr-body.md
# 바로 아래 계약을 충족하는 한국어 본문을 이 파일에 작성하고 readback한 뒤 사용한다.
git push -u origin feat/issue-1561-netcdf-operation-boundary
gh pr create \
  --repo bluetape4k/bluetape4k-projects \
  --base develop \
  --head feat/issue-1561-netcdf-operation-boundary \
  --title "NetCDF timeout 이후 진행 상태 진단 경계를 추가한다" \
  --body-file "$pr_body"

head_sha=$(git rev-parse HEAD)
remote_sha=$(git ls-remote origin refs/heads/feat/issue-1561-netcdf-operation-boundary | awk '{print $1}')
test "$head_sha" = "$remote_sha"
gh pr checks --repo bluetape4k/bluetape4k-projects --watch
gh pr view --repo bluetape4k/bluetape4k-projects \
  --json url,headRefOid,baseRefName,mergeStateStatus,statusCheckRollup
```

PR body는 문제, 변경, timeout/trust/migration 계약, 검증, risk, `Refs #1561`, 마지막
`## DoD Status`를 포함한다. 생성 직후 live metadata와 remote exact head를 확인하고 regular CI의
모든 required job이 exact head에서 terminal PASS인지 읽는다. local JUnit summary의 failed=0,
skipped=0과 hosted check URL을 review 문서/PR DoD에 연결한다. `slow-netcdf` Nightly와 release
matrix receipt는 이 PR merge 증거로 대체하지 않고 2.0.0 release gate의 별도 handoff로 명시한다.
merge와 branch/worktree cleanup은 별도 승인 전까지 수행하지 않는다.

## 10. Step 3-R 통합 검토

| 관점 | 최초 결과 | 계획 반영 | 최종 결과 |
|---|---|---|---|
| Performance | P0=0, P1=0 | 단일 indexed 조회, 5값 metric allowlist, 자동 retry 0회 확인 | CLEAR |
| Stability | P1=2 | 모든 latch/termination bounded, 취소 전후 lease 필드와 grid row rollback 비교 | P0=0, P1=0 CLEAR |
| Security | P1=2 | 3-field redacted DTO, 민감 필드 부재, 네 operation authorize-first negative test | P0=0, P1=0 CLEAR |
| Operator/Ops | P0=0, P1=0, P2=2, P3=1 | operator checklist, exact-head CI handoff, stop-retry부터 재검증까지 rollback 순서 | P0=0, P1=0 CLEAR; P2/P3 integrated |
| Developer/API | P0=0, P1=0 | 3-인자 JVM descriptor, checkpoint 위치, fixture/Gradle 명령 실행 가능성 확인 | CLEAR |
| User/caller | P1=2 | outcome decision table과 marker 기반 EN/KO 실행 예제·byte parity | P0=0, P1=0 CLEAR |
| Main integration | 중복·충돌·범위 검토 | 승인된 API A 유지, library/caller/release gate 책임을 분리 | PASS |

Security의 실제 application identity store/policy와 Ops의 `slow-netcdf` Nightly receipt는 이
library PR이 대신 증명하지 않는다. 전자는 caller integration 책임으로 명시하고 fixture로 안전한
호출 순서를 고정하며, 후자는 2.0.0 release gate의 별도 exact-head handoff로 남긴다. 두 항목은
현재 implementation/PR 범위의 P0/P1 blocker가 아니다.

### Writer DoD

| 항목 | 결과 | 근거 |
|---|---|---|
| SPW-01 audience/goal | PASS | 구현자와 reviewer가 Issue #1561을 순차 실행하는 목표가 첫 문단에 명확함 |
| SPW-02 structure | PASS | 파일 책임, 추적표, 위험, RED/GREEN, 검증, review, delivery 순서 |
| SPW-03 executable detail | PASS | exact path, signature, command, expected result, rollback/rerun 포함 |
| SPW-04 terminology/locale | PASS | 한국어 본문과 보존할 API·command token을 분리하고 EN/KO parity gate 포함 |
| SPW-05 completion/risk | PASS | P0/P1 0, caller/release 책임과 별도 승인 gate를 명시 |

## 11. Plan self-review

- **Spec coverage:** 사양 §11 여섯 기준과 §12 DoD가 Task 1~6에 모두 매핑된다.
- **Placeholder scan:** `TBD`, `TODO`, “적절한 처리”, 후속 구현 placeholder 없음.
- **Type consistency:** `findImportProgress`, `ImportCheckpoint.afterLeaseTouched`,
  `NetCdfImportStatus`, metric status allowlist가 전 task에서 동일하다.
- **Ordering:** spec/plan commit → RED/GREEN API → cancellation → regression → docs/fixture →
  full verification → review/lesson → commit/push/PR 순서이며 later artifact 의존이 없다.
- **Rollback:** schema/dependency가 없어 commit 단위 revert가 가능하다. cancellation seam이나
  metric이 회귀하면 stop retry → worker 격리 → progress/partial rows 보존 → 해당 commit revert →
  타깃/full science 검증 → authorization·quarantine 재확인 순서로 복구한다. public 조회 API와
  docs를 유지한 채 internal 변경만 되돌리는 경우에도 사양/계획을 재검토한다.

## 12. Plan DoD

- 모든 public behavior는 RED 또는 기존 동작 regression 증거를 가진다.
- Testcontainers tests는 순차 실행하고 `skipped=0`을 요구한다.
- 기존 3-인자 constructor와 두 public 메서드는 유지된다.
- locale, KDoc, metric, trust, migration, quarantine가 exact task에 배정된다.
- library boundary는 실제 consumer 정책을 소유하지 않으며, authorize-first와 redacted response를
  외부-package fixture로 증명하고 실제 application integration은 caller 책임으로 명시한다.
- module/schema/dependency/workflow hazards는 changed-path 증거로만 N/A 처리한다.
- PR 생성까지만 승인 범위이며 merge·release·cleanup은 계획에 포함하지 않는다.
