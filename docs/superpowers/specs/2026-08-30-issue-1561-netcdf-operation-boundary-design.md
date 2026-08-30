# NetCDF 작업 경계와 진행 상태 조회 설계

- **일자**: 2026-08-30
- **이슈**: [#1561](https://github.com/bluetape4k/bluetape4k-projects/issues/1561)
- **대상 모듈**: `bluetape4k-science` (`utils/science`)
- **기준**: `origin/develop@9831a513f9b81e53f505fadd2e4546b8ea8cf6a8`
- **브랜치**: `feat/issue-1561-netcdf-operation-boundary`
- **상태**: 사용자 설계안 A 승인, Step 2-R 통합 PASS

## 1. 문제와 목표

현재 `NetCdfCatalogService`의 README timeout 예시는 `registerFile()`과
`importGridValues()`를 같은 작업에 넣는다. timeout이 발생하면 호출자가 작업 내부에서
생성된 `fileId`를 받지 못하므로 `(fileId, variableName)` 진행 상태를 조회하거나 같은
작업을 안전하게 재개할 수 없다. Repository에는 이미 진행 상태 조회 기능이 있지만,
서비스 사용자가 직접 Exposed transaction을 열어야 한다.

이번 변경의 목표는 다음과 같다.

1. 등록과 장시간 import의 수명주기를 분리해 timeout 이후에도 `fileId`를 보존한다.
2. `NetCdfCatalogService`에서 `(fileId, variableName)` 진행 상태를 직접 조회한다.
3. local path 검증과 호출 계층의 허용 경로·인증·인가·tenant 책임을 구분한다.
4. 2.0에서 sealed exception 하위 타입이 추가될 수 있음을 설명하고 안전한 호출자
   분기 패턴을 source-compile fixture로 고정한다.
5. timeout, 재시도, 파일 교체, 경로 거부 경계를 회귀 테스트로 고정한다.

## 2. 현재 근거

- `NetCdfCatalogService.registerFile(filePath)`는 등록이 끝난 뒤에만 `Long fileId`를
  반환한다.
- `NetCdfCatalogService.importGridValues(fileId, variableName)`는 blocking API이며,
  `CancellationException`과 `InterruptedException`을 실패 상태로 바꾸지 않고 다시
  던진다.
- 취소된 import가 이미 lease를 얻었다면 진행 상태는 lease 만료 전까지
  `IN_PROGRESS`일 수 있다.
- `NetCdfImportProgressRepository.findByFileAndVariable()`는 필요한 조회를 제공하지만
  호출자가 transaction을 열어야 한다.
- `NetCdfFileGuard`는 control 문자, URI, non-regular file, symlink를 거부하고
  `fileKey|size|lastModifiedTime`으로 identity를 확인한다. 허용 root, 인증·인가,
  tenant 소유권은 검사하지 않는다.
- 현재 `NetCdfExceptionApiCompatibilityTest`의 `else` 분기는 안전한 기본 처리 패턴을
  보여 주지만, 외부 package의 source-compile 계약으로 명시돼 있지 않다.

## 3. 범위와 비범위

### 3.1 포함 범위

- `NetCdfCatalogService.findImportProgress(fileId, variableName)` 추가
- 공개 서비스 KDoc에 blocking, timeout, cooperative cancellation, progress, lease 계약 추가
- README EN/KO timeout 예제와 신뢰 경계 설명 교정
- `utils/science/src/test/kotlin/consumer/fixture/NetCdfPublicApiSourceCompatibilityTest.kt`
  외부 package source-compile fixture 추가
- timeout 후 동일 `fileId` 재사용, 파일 교체, path 거부 회귀 테스트
- public constructor는 유지하면서 transaction 내 cancellation을 결정적으로 검증하는
  internal test checkpoint 추가
- POSIX unreadable local file 거부 테스트와 기존 path 거부 테스트 보강

### 3.2 제외 범위

- 새 작업 handle 타입이나 async/coroutine API
- Repository, table, schema, lease TTL 정책 변경
- allowed-root 설정, 인증·인가, tenant 정책 구현
- 콘텐츠 hash 계산 또는 fingerprint 형식 변경
- 취소 시 진행 상태를 즉시 `FAILED`로 변경하는 동작
- 새 `NetCdfException` 하위 타입 추가
- dependency, Gradle catalog, module registration, workflow 변경

## 4. 대안과 결정

### 대안 A — 기존 `fileId`와 `variableName`을 작업 식별자로 유지하고 조회 API만 추가

`registerFile()`을 deadline 밖에서 먼저 호출하고, `importGridValues()`만 deadline이 있는
worker에 제출한다. timeout 후 호출자는 보존한 `fileId`와 `variableName`으로
`findImportProgress()`를 호출한다.

- 장점: 기존 API와 저장 모델을 유지하며 public ABI 추가가 메서드 하나로 제한된다.
- 비용: 두 값을 함께 전달해야 하며, caller가 executor 종료와 재시도 정책을 소유한다.
- **결정**: 채택. 사용자가 2026-08-30 승인했다.

### 대안 B — `NetCdfImportOperation(fileId, variableName)` public handle 추가

- 장점: 작업 식별자를 하나의 값으로 전달할 수 있다.
- 거부 이유: 기존 tuple을 감싸는 public Serializable 타입과 overload가 늘지만, 이번
  수용 기준에는 별도 상태나 동작이 필요하지 않다.

### 대안 C — `registerFile()` 반환형 변경 또는 등록과 import를 하나의 task로 결합

- 장점: 겉보기 호출 단계가 줄어든다.
- 거부 이유: 반환형 변경은 source/binary compatibility를 깨뜨리고, 결합 task는 timeout
  시 `fileId` 유실 문제를 그대로 남긴다.

## 5. 공개 API 계약

다음 메서드를 `NetCdfCatalogService`에 추가한다.

```kotlin
fun findImportProgress(fileId: Long, variableName: String): NetCdfImportProgress?
```

계약은 다음과 같다.

- `(fileId, variableName)` 진행 row가 없으면 `null`을 반환한다.
- `variableName`이 blank면 `IllegalArgumentException`을 발생시킨다.
- 조회는 서비스가 Exposed transaction 안에서 수행한다.
- 파일 레코드의 존재 여부는 별도로 검증하지 않는다. Repository의 현재 조회 의미를
  그대로 노출해 timeout 직후에도 부수 효과 없이 상태만 읽는다.
- 반환 모델은 현재의 `NetCdfImportProgress`를 재사용한다.
- 조회는 lease를 획득·연장·만료시키거나 progress 상태를 변경하지 않는다.
- 조회 결과마다 `netcdf.import.progress.lookup{status}` counter를 1회 증가시킨다.
  `status` allowlist는 `missing`, `pending`, `in-progress`, `completed`, `failed`이며
  `fileId`, `variableName`, path, tenant는 metric tag에 넣지 않는다.
- `fileId`는 어떤 경우에도 외부 권한 토큰이 아니다. caller는 `registerFile()`,
  `importGridValues()`, `findImportProgress()`, 모든 retry 직전에 canonical path의
  allowed-root 포함 여부, 인증·인가, 파일 소유권, tenant/job binding을 다시 검증한다.
- `NetCdfImportProgress`는 내부 운영 모델이다. HTTP/RPC 응답으로 직접 직렬화하지 않고,
  caller가 허용한 상태·cursor만 담는 redacted DTO로 변환한다. `errorMessage`, lease
  token 역할을 하는 시각, 내부 timestamp는 외부 응답에서 기본적으로 제외한다.

서비스 KDoc은 다음 작업 경계를 명시한다.

1. `registerFile()`과 `importGridValues()`는 blocking이다.
2. timeout이 필요한 호출자는 먼저 `registerFile()`로 `fileId`를 확보한다.
3. deadline은 `importGridValues()` 작업에만 적용한다.
4. timeout은 cooperative cancellation 요청이다. `Future.cancel(true)`가 반환됐다고
   worker 종료나 transaction rollback이 끝났다고 간주하지 않는다.
5. caller는 executor를 종료하고 bounded `awaitTermination`으로 worker 종료를 확인한 뒤
   진행 상태를 조회한다.
6. `awaitTermination`이 `false`면 worker가 살아 있다고 보고 재시도하지 않는다. executor를
   격리하고 운영 결과를 `RECOVERY_REQUIRED`로 전환해 alert하며 progress는 진단에만
   사용한다.
7. worker가 종료되면 task wrapper가 기록한 최종 예외와 progress를 함께 확인한다.
   진행 row가 `COMPLETED`면 성공으로 처리하고, 활성 lease의 `IN_PROGRESS`면 재시도하지
   않는다. `PENDING`, `FAILED`, row 없음, 만료된 `IN_PROGRESS`는 조사 대상이지만,
   상태만으로 자동 재시도를 결정하지 않는다. `FileChanged`, `CorruptProgress`,
   `FileOpen`, variable/coordinate/CRS/shape/resource/duplicate 관련 typed failure는 입력이나
   운영 조건을 수정하기 전까지 자동 재시도하지 않는다. 정책상 transient로 분류한
   failure만 별도 승인된 bounded backoff와 retry budget 안에서 재시도한다. 이 모듈의
   README 예제와 기본 권장 정책은 자동 재시도 0회다. application이 자동 재시도를
   도입하면 job별 attempt 상한을 설정하고, `ImportAlreadyRunning` 또는 상한 소진 시
   `RECOVERY_REQUIRED`로 중지·alert해야 한다.
8. 재시도를 허용한 경우에도
   `leaseExpiresAt`을 caller clock으로 비교해 단독 판정하지 않는다. worker 종료 후
   import를 다시 요청하고 `ImportAlreadyRunning`을 DB clock 기반 authoritative 결과로
   처리한다. malformed `IN_PROGRESS`의 null lease는 Repository가 복구하며, 일관되지 않은
   cursor/status는 `CorruptProgress`로 격리될 수 있다. 실제 backoff와 재시도 상한은
   caller가 소유한다.

## 6. 신뢰 경계와 파일 identity

`NetCdfCatalogService`는 외부 요청을 직접 받는 sandbox나 authorization 계층이 아니라
신뢰된 운영 호출자를 위한 local-file API다.

- 서비스가 검사하는 경계: blank/control 문자, URI scheme, symlink 구성요소,
  non-regular file, 파일 크기 상한, 등록 시점과 재개 시점의 identity 변화
- 호출 계층이 검사할 경계: allowed root, canonical path가 허용 root 아래인지,
  인증·인가, tenant 또는 job 소유권, 파일 생성·배포 주체, 보존·삭제 정책
- `fileKey|size|lastModifiedTime` fingerprint는 같은 경로의 파일 교체와 일반적인 변경을
  탐지하기 위한 identity 휴리스틱이다. 콘텐츠 hash나 악의적 동일 metadata 변경에 대한
  증명이 아니다.
- guard의 path stat과 path-based open은 sandbox나 hostile-writer에 대한 완전한 TOCTOU
  방어가 아니다. caller는 서비스 계정만 쓸 수 있는 private/quarantined directory를
  사용하고, import 수명 동안 untrusted writer의 rename·replacement·metadata 변경을
  차단해야 한다. hostile writer를 지원해야 한다면 file descriptor/handle 기반 open과
  별도 threat-model 설계가 필요하며 이번 범위에는 포함하지 않는다.
- caller는 등록된 파일을 import 완료까지 immutable하게 유지해야 한다. 교체가 감지되면
  기존 `fileId` import는 `NetCdfException.FileChanged`로 실패한다.
- caller는 최초 등록 결과를 신뢰해 권한 검사를 생략하지 않는다. 등록·import·progress
  조회·retry마다 authorization 결과와 tenant/job binding을 다시 확정하며, stale
  `fileId`만으로 권한을 부여해서는 안 된다.

외부 응답은 서비스 모델을 직접 사용하지 않고 다음 최소 의미만 가진 caller-owned DTO로
변환한다. 이름은 예시이며 library public API로 추가하지 않는다.

```kotlin
data class NetCdfImportStatusResponse(
    val status: String,
    val lastCommittedSlice: Long?,
    val outcome: String,
)
```

`status`는 allowlist 상태만, `outcome`은 `COMPLETED`, `RUNNING`, `RETRY_REVIEW`,
`RECOVERY_REQUIRED` 중 하나만 사용한다. raw `errorMessage`, path, lease 시각, 내부
timestamp는 운영 저장소와 접근 통제된 log에만 남기며 외부 DTO에는 포함하지 않는다.

## 7. Sealed exception 2.0 호환성

이번 변경은 `NetCdfException` 하위 타입을 추가하지 않는다. 다만 2.0 계열에서 typed
failure가 확장될 수 있으므로 다음 migration 계약을 문서화한다.

- 기존 public 메서드의 descriptor는 변경하지 않는다. 조회 메서드 추가는 기존 binary와
  source caller에 additive compatibility를 제공한다.
- Kotlin caller가 다른 module에서 `NetCdfException`을 exhaustive `when`으로 분기하면,
  새 subtype이 추가된 버전으로 source recompilation할 때 새 분기를 요구할 수 있다.
- 모든 subtype을 업무 의미로 구분할 필요가 없는 caller는 `else` fallback을 둔다.
- 특정 subtype만 복구 가능한 caller는 해당 subtype을 먼저 처리하고 나머지는 base
  handler로 전달하는 `else` 분기를 둔다.
- 외부 package fixture는
  `utils/science/src/test/kotlin/consumer/fixture/NetCdfPublicApiSourceCompatibilityTest.kt`에
  두고 public API만 사용한다. `else` fallback이 있는 분기와 새 progress 조회 호출이
  함께 source-compile되는지 확인한다. 이는 미래 subtype 자체를 합성하는 proof가 아니라,
  현재 권장 fallback 패턴의 compile proof다. sealed direct subtype은 같은 package/module
  제약이 있으므로 외부 consumer fixture에서 가짜 future subtype을 추가하지 않는다.

## 8. 실패 모드와 동작

| 실패 모드 | 예상 동작 | 호출자 조치 |
|---|---|---|
| 등록 전에 timeout을 적용해 `fileId`를 잃음 | 수정된 예제에서는 발생하지 않음 | 등록을 deadline 밖에서 완료 |
| import timeout 후 worker가 아직 실행 중 | `awaitTermination=false`, 활성 작업 가능 | executor 격리, 운영 알림, 재시도 금지; progress는 진단에만 사용 |
| 취소가 slice commit 뒤 관찰됨 | progress가 `COMPLETED`일 수 있음 | 상태를 최종 결과로 사용 |
| 진행 row가 생성되기 전 취소됨 | 조회 결과 `null` | worker 종료 확인 후 같은 `fileId`로 재시도 가능 |
| `PENDING` 또는 null lease의 `IN_PROGRESS` | Repository가 재획득 또는 malformed lease 복구 | worker 종료 확인 후 bounded 재호출 |
| status/cursor 조합이 일관되지 않음 | `CorruptProgress`로 격리될 수 있음 | 자동 반복을 중지하고 운영 진단 |
| `FAILED`이지만 원인이 영구 오류임 | 같은 입력으로 반복 실패 | typed failure와 worker 최종 예외를 분류하고 입력 수정 전 자동 재시도 금지 |
| `ImportAlreadyRunning` 반복 또는 attempt 상한 소진 | 중복 작업이나 무한 retry 위험 | 자동 반복 중지, `RECOVERY_REQUIRED`, low-cardinality alert |
| 등록된 경로의 파일이 교체됨 | `FileChanged`, progress row 미생성 또는 기존 row 불변 | 새 파일을 새 `fileId`로 등록 |
| URI·symlink·directory 경로 전달 | `FileOpen` 또는 등록 거부 | caller 입력 검증과 allowed-root 정책 점검 |
| caller가 다른 tenant의 `fileId` 사용 | 서비스 자체는 식별하지 못함 | 호출 계층에서 tenant 소유권 검증 |
| sealed subtype 추가 뒤 exhaustive caller 재컴파일 | 새 branch가 없으면 compile 실패 가능 | base fallback 또는 새 subtype branch 추가 |

## 9. 테스트 전략

### 9.1 Progress API

- row가 없으면 `null`
- 저장된 `IN_PROGRESS`/`COMPLETED` row의 필드를 그대로 반환
- blank `variableName` 거부
- 조회가 상태와 lease를 변경하지 않음

### 9.2 Timeout과 재시도

- pre-admission 테스트는 임시 NetCDF를 먼저 등록해 `fileId`를 확보하고, worker를
  import 호출 전 latch에서 대기시켜 timeout과 interrupt를 결정적으로 만든다.
- pre-admission worker 종료 후 `findImportProgress()`가 `null`임을 확인하고, 같은
  `fileId`로 import를 다시 실행해 `COMPLETED`를 확인한다.
- post-admission 테스트를 위해 기존 public primary constructor
  `(NetCdfFileRepository, NetCdfImportProgressRepository, MeterRegistry?)`를 그대로 두고,
  네 번째 `internal ImportCheckpoint`를 받는 internal secondary constructor를 추가한다.
  primary constructor에 default parameter를 추가하지 않는다. reflection과 `javap`로 기존
  3-인자 JVM descriptor가 남는지 검증한다.
- `ImportCheckpoint`는 internal fun interface이고 production 기본 구현은 no-op이다.
  spatial import의 첫 preflight tile transaction에서 최초 `touchLease()` 성공 직후,
  `readTile()` 전에 정확히 한 번 호출한다. rank-1과 두 번째 write pass에는 호출하지 않는다.
- post-admission worker는 checkpoint에서 lease 획득을 알리고 latch를 기다린다. 테스트는
  `Future.cancel(true)`와 `shutdownNow()`를 호출하고 bounded `awaitTermination` 성공을
  확인한다.
- checkpoint의 interrupt는 현재 Exposed transaction을 rollback한다. 테스트는 grid row가
  없고, worker 종료 뒤 heartbeat가 더 갱신되지 않으며, progress가 초기 lease의
  `IN_PROGRESS`로 남는지 확인한다. dataset은 기존 `use` 경계에서 닫힌다.
- 활성 lease 중 즉시 재호출은 `ImportAlreadyRunning`이어야 한다. DB에서 lease를
  결정적으로 만료한 뒤 같은 `fileId`로 재호출해 `COMPLETED`를 확인한다.
- 별도 lifecycle fixture는 `awaitTermination=false` 분기에서 retry가 호출되지 않는지,
  task wrapper가 기록한 최종 예외와 progress를 함께 분류하는지 검증한다.
- 기존 partial progress/expired lease resume 테스트와 함께 timeout 이후 재개 의미를
  검증한다.
- checkpoint는 internal 테스트 seam이며 외부 caller, public constructor, 정상 import
  의미에는 노출되지 않는다.
- 테스트의 latch 대기와 worker 종료 상한은 각각 5초로 고정한다. 상한을 넘기면
  `shutdownNow()`를 다시 요청하고 thread 이름을 assertion에 포함한 뒤 테스트를 실패시킨다.
  기다림을 무제한으로 두지 않는다.

### 9.3 파일 교체

- 임시 복사본을 등록한 뒤 같은 path의 파일 내용을 교체한다.
- 교체는 새 inode를 같은 path로 atomic move하고 크기 또는 mtime 차이를 강제해
  fingerprint 변화가 파일시스템 timestamp 정밀도에 의존하지 않게 한다.
- 기존 `fileId` import가 `NetCdfException.FileChanged`를 발생시키는지 확인한다.
- 교체 감지 시 새 progress row가 생기지 않는지 확인한다.

### 9.4 경로와 권한 경계

- URI, control 문자, final/parent symlink, directory를 거부하는 기존 guard 테스트를
  유지·보강한다.
- allowed-root, 인증·인가, tenant 정책은 서비스 테스트 대상이 아님을 README/KDoc
  parity 검사로 확인한다.
- 이 모듈은 tenant authorization을 보장하지 않는다. 서비스 테스트는 local path의
  구조·identity·현재 process의 read permission 거부를 증명하며, consumer는
  등록·import·조회·retry마다 authorization을 검증하는 negative test를 자체 경계에 둔다.
- 저장소의 `test-science` CI는 `ubuntu-latest`이고 로컬 기준 환경은 macOS이므로 POSIX
  unreadable-file 검증을 지원 범위로 고정한다. 테스트는 정상 NetCDF를 생성한 뒤 read
  permission을 제거하고 `registerFile()`이 `FileOpen`을 발생시키는지 확인하며 `finally`에서
  permission을 복구한다. CI/로컬 실행 사용자는 root가 아니어야 하며, root 실행은 이
  acceptance의 유효한 증거로 인정하지 않는다.

### 9.5 Source compatibility

- `utils/science/src/test/kotlin/consumer/fixture/NetCdfPublicApiSourceCompatibilityTest.kt`의
  `consumer.fixture` package에서 public API만으로 기존 두 메서드와 새 조회 메서드를
  참조한다.
- `NetCdfException` 분기는 `else` fallback을 포함해 새 subtype 추가에도 source-compile
  가능한 현재 권장 패턴을 고정한다. 미래 subtype 추가 자체를 시뮬레이션한다고 주장하지
  않는다.

### 9.6 운영 진단과 보존

- 서비스는 progress 조회에만 `netcdf.import.progress.lookup{status}`를 기록한다.
  timeout, worker stuck, retry exhausted는 서비스 밖 executor lifecycle이므로 caller가
  `netcdf.import.timeout`, `netcdf.import.worker.stuck`, `netcdf.import.retry.exhausted`에
  해당하는 low-cardinality metric/log를 기록한다.
- log correlation은 application job/correlation ID를 사용한다. raw path, tenant,
  `errorMessage`를 metric tag에 넣지 않는다. 접근 통제된 운영 log의 보존 기간은
  application 정책을 따른다.
- `FileChanged` 전에 이미 commit된 grid row와 progress는 자동 삭제하지 않는다. 기존
  `fileId`를 quarantine 상태로 다루고 새 파일은 새 `fileId`로 등록한다. 수동 정리는
  보존 기간, 참조 여부, tenant 소유권, 삭제 권한을 확인한 운영자만 수행한다.
- 이번 변경은 destructive cleanup API나 schema를 추가하지 않는다. 정리 자동화가
  필요하면 별도 이슈와 migration/runbook 검토가 필요하다.

## 10. 문서 계약

README EN/KO는 같은 순서와 의미를 유지한다.

- 등록 예제에서 `fileId`를 deadline task 밖에서 생성
- 등록 → import-only task → timeout cancel → bounded worker 종료 결과 → task wrapper의
  최종 예외 → progress 조회 → 상태와 오류 종류별 처리 순서
- `awaitTermination=false`이면 재시도 금지, `FAILED`만으로 자동 재시도 금지,
  영구 typed failure와 정책상 transient failure 구분
- trusted local path와 caller-owned allowed-root/authn/authz/tenant 경계
- 등록·import·조회·모든 retry마다 authorization과 tenant/job binding 재검증
- fingerprint 한계와 immutable file 책임
- progress 모델 직접 외부 노출 금지, 상태 중심 redacted DTO와 오류 정보 제거
- private/quarantined directory와 untrusted writer 차단 전제
- sealed exception subtype 추가 시 source migration 안내
- API 표에 `findImportProgress()` 추가
- progress lookup metric allowlist, caller timeout/stuck/retry alert, partial row quarantine

두 README의 parity 검사는 다음 항목을 같은 순서로 읽어 확인한다.

1. `NetCdfCatalogService` API 표의 세 메서드
2. timeout lifecycle 7단계와 `awaitTermination=false` 처리
3. progress 상태·worker 예외·영구/일시 오류 분류표
4. allowed-root/authn/authz/tenant 재검증과 redacted DTO
5. fingerprint/hostile-writer 한계
6. sealed subtype `else` migration 안내
7. progress lookup metric과 caller alert, partial row quarantine

검증은 각 locale에서 위 identifier와 상태 토큰을 `rg`로 확인하고, 최종 inline review에서
문단 순서와 의미 parity를 함께 판정한다.

KDoc은 한국어 reader-facing 정책을 따르며, API 이름과 상태 enum은 그대로 보존한다.

## 11. 수용 기준 추적

| 이슈 수용 기준 | 설계 충족 방식 | 검증 |
|---|---|---|
| timeout 경로에서 progress 조회용 식별자 보존 | 등록을 deadline 밖으로 이동, `fileId` 유지 | timeout/retry 통합 테스트, README 예제 |
| path/auth/tenant/fingerprint 책임 명시 | 서비스 KDoc과 EN/KO trust boundary | 문서 parity와 source review |
| 2.0 sealed subtype migration 안내 | base fallback 권장, additive API 명시 | 외부 package source-compile fixture |
| timeout/resume 검증 | timeout 전 row 없음과 동일 `fileId` 재시도, 기존 partial resume 유지 | 타깃 Testcontainers 테스트 |
| file replacement 검증 | import 전 fingerprint 재검증 유지 | `FileChanged` 통합 테스트 |
| permission boundary 검증 | POSIX unreadable local file 거부, tenant authorization은 consumer가 매 작업·retry에서 검증 | unreadable/guard 테스트, KDoc/README review, consumer negative-test 책임 명시 |

## 12. 완료 정의

- 기존 `registerFile()`과 `importGridValues()` descriptor가 유지된다.
- reflection/`javap`가 기존 3-인자 public constructor descriptor를 확인한다.
- `findImportProgress()`가 transaction 경계를 숨기고 읽기 전용 조회를 제공한다.
- timeout 후 동일 `fileId` 재시도와 파일 교체 거부 테스트가 통과한다.
- 외부 package source-compile fixture가 통과한다.
- README EN/KO와 KDoc이 timeout·trust·migration 계약을 같은 의미로 설명한다.
- `:bluetape4k-science:test`, 관련 정적 검사, `git diff --check`가 통과한다.
- inline review와 독립 review의 최신 결과가 P0=0/P1=0이다.
- PR은 승인된 `develop <- feat/issue-1561-netcdf-operation-boundary`로 생성하고 merge는
  별도 승인 전까지 수행하지 않는다.

## 13. Step 2-R 통합 검토

검토 범위는 이 사양과 현재 `utils/science`의 service, repository, guard, model, test,
README EN/KO다. 각 lane은 읽기 전용으로 한 관점만 검토했고, 수정은 main session이
통합했다.

| 관점 | 최초 결과 | 반영한 수정 | 최종 결과 |
|---|---|---|---|
| 성능 | P0=0, P1=0 | indexed single-row 조회라 별도 benchmark N/A | CLEAR |
| 안정성 | P1=1, P2=2 | post-lease cancellation checkpoint, DB-clock retry, atomic replacement | CLEAR |
| 보안 | P2=2 | redacted DTO, 매 작업 authorization, hostile-writer 경계 | CLEAR |
| Operator/Ops | P1=1, P2=3 | `RECOVERY_REQUIRED`, 자동 retry 0회, metric allowlist, quarantine | CLEAR |
| Developer/API | P1=2, P2=3 | POSIX unreadable test, 3-인자 constructor ABI와 checkpoint 위치 | CLEAR |
| User/caller | P1=2, P2=3 | 규범적 timeout 순서, 영구/일시 오류 분류, locale parity | CLEAR |
| Main integration | 중복·충돌·근거 검토 | public API 최소화, 책임 경계와 수용 기준 재추적 | PASS |

최신 통합 결과는 **P0=0, P1=0, P2=0, P3=0**이다. public 설계안 A는 유지됐고,
internal test seam과 운영 계약만 구체화됐으므로 사용자 재승인이 필요한 material design
변경은 없다.

### Writer DoD

| 항목 | 결과 | 근거 |
|---|---|---|
| SPW-01 | PASS | 독자는 library 개발자·운영 caller, 언어는 한국어, source/issue/ref 고정 |
| SPW-02 | PASS | 문제, 대안, API, 신뢰 경계, 실패 모드, 호환성, 테스트, 수용 기준, DoD 포함 |
| SPW-03 | PASS | Korean naturalness KO-01~KO-07 검토, identifier와 상태 토큰 보존 |
| SPW-04 | PASS | source-to-claim과 issue-to-acceptance 표, 6개 관점 재검토로 의미 추적 |
| SPW-05 | PASS | 최종 Markdown read-back, terminology audit findings=0, `git diff --check` PASS |
