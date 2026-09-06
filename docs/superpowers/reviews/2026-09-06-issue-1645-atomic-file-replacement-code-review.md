# Issue #1645 원자적 파일 교체 최종 코드 검토

## 검토 기준

- repository: `bluetape4k/bluetape4k-projects`
- base: `origin/develop@8e5ff93d71faa372efaf1012088acc9e480bf04e`
- reviewed code head: `d5d5cd3612677f28618640c70fa685237c4b6d99`
- diff: `origin/develop...d5d5cd3612677f28618640c70fa685237c4b6d99`
- 범위: `Path.writeAtomically`, failure seam과 filesystem/caller test, KDoc,
  README locale pair, CHANGELOG, 설계·계획·체크리스트

## Provenance와 실행 경계

성능·안정성·보안 reviewer 세 lane과 운영·개발자/API·사용자/호출자 reviewer 세 lane을
각각 native `code-reviewer`로 실행했다. live base 전진을 발견한 뒤 첫 세 lane을 회수하고
rebase·재검증 후 여섯 관점 모두 current base와 exact head를 전달했다. 각 lane은 90초
안에 verdict를 남기지 않아 repository liveness 규칙에 따라 회수됐다.

따라서 아래 관점 판정은 main-session exact-diff fallback이며 independent 또는 특정 model
검토 provenance를 주장하지 않는다. native timeout 자체는 코드 finding이 아니지만,
독립 검토가 실행되지 않은 검증 공백으로 남긴다.

## Severity 요약

| 상태 | P0 | P1 | P2 | P3 |
|---|---:|---:|---:|---:|
| 최초 main-session 검토 | 0 | 0 | 0 | 1 |
| 수정 후 | 0 | 0 | 0 | 0 |

수정한 P3는 `AtomicFileSupport.kt`의 Detekt suppression 근거가 영문 내부 주석으로 남아
한국어 reader-facing comment 규칙과 맞지 않은 점이다. 같은 의미의 한국어 주석으로 바꿨고
실행 동작이나 ABI는 변경하지 않았다.

## 여섯 관점 검토

### 1. 성능 — PASS

- `AtomicFileSupport.kt:55-91`의 singleton writer는 mutable shared state가 없고 호출마다
  sibling temp와 counting stream만 만든다. 서로 다른 target을 전역 lock으로 직렬화하지
  않는다.
- `AtomicFileSupport.kt:135-147`은 provider write 성공 뒤에만 count를 증가시키고 commit
  뒤 `Files.size`를 조회하지 않는다.
- `AtomicFileSupport.kt:150-179`의 Throwable graph 순회는 cleanup까지 실패한 예외 경로에만
  실행된다. 정상 경로 allocation과 traversal을 늘리지 않는다.
- 절대 throughput은 filesystem별로 달라 공개 계약으로 삼지 않았다. bounded contention
  test만 acceptance 증거로 사용했다.

Finding: P0 0, P1 0, P2 0, P3 0.

### 2. 안정성 — PASS

- `AtomicFileSupport.kt:68-91`은 parent 생성, sibling temp 생성, open, callback, close,
  atomic move 순서를 고정한다. close failure가 있으면 move에 진입하지 않는다.
- `AtomicFileSupport.kt:71-76,104-110`은 commit 전 실패에서 staged file만 정리하고 primary
  identity를 다시 던진다. move 성공 경로에는 cleanup 조회가 없다.
- `AtomicFileSupport.kt:94-110,150-179`은 callback, close, cleanup의 primary/suppressed
  순서와 양방향 graph cycle을 보존한다.
- failure seam test가 parent/temp/open/callback/close/move/delete 단계와
  `CancellationException`, `Error`, unsupported atomic move를 직접 검증한다.

Finding: P0 0, P1 0, P2 0, P3 0.

### 3. 보안 — PASS

- `AtomicFileSupport.kt:33-42`는 lexical normalization이 sandbox가 아니며 symlink,
  hard-link, mount 교체와 TOCTOU를 방어하지 않는다고 명시한다.
- 임시 파일명에 basename이 들어가고 permission이 provider default라는 점, private parent와
  opaque basename을 사용해야 한다는 점을 KDoc과 README 두 locale에 함께 기록했다.
- library가 provider 예외를 감추거나 path를 직접 logging하지 않는다. caller가 전체 경로,
  basename과 민감한 예외 메시지를 가려 기록하도록 문서화했다.
- payload와 시간 한도는 caller 책임이며 bounded README fixture가 한도 초과 시 target을
  commit하지 않음을 검증한다.

Finding: P0 0, P1 0, P2 0, P3 0.

### 4. 운영 — PASS

- README는 process crash로 `.<basename>.*.tmp`가 남을 수 있음을 밝히고 private parent의
  파일 수·사용량 감시, active writer 제외, retention 뒤 bounded batch cleanup을 요구한다.
- 기존 target metadata와 `fsync` durability를 보장하지 않는 경계를 KDoc, README와
  CHANGELOG에 일치시켰다.
- rollback은 새 API 호출 제거이며 module/dependency/catalog/workflow 등록을 바꾸지 않는다.
  Image 소비자 전환은 provider merge와 개발 버전 발행 provenance 뒤 별도 PR로 남겼다.

Finding: P0 0, P1 0, P2 0, P3 0.

### 5. 개발자/API — PASS

- 공개 표면은 additive `Path.writeAtomically((OutputStream) -> Unit): Long` 한 개다.
  `@file:JvmName("AtomicFileSupport")`와 `@Throws(IOException::class)`가 Java facade를 만든다.
- 외부 package Kotlin fixture와 Java fixture가 실제 호출을 compile·실행한다. `javap`의
  descriptor는 `(Ljava/nio/file/Path;Lkotlin/jvm/functions/Function1;)J`이고
  `throws IOException`을 선언한다.
- callback의 stream borrow 규칙, blocking 성격, coroutine dispatcher/cancellation 소유권,
  Java callback의 `UncheckedIOException` 변환을 두 README에서 설명한다.

Finding: P0 0, P1 0, P2 0, P3 0.

### 6. 사용자/호출자 — PASS

- README 영어/한국어에 bounded copy, coroutine, Java 예제가 같은 순서와 의미로 있다.
- empty/root path 거부, 자동 생성 parent 잔존, no-fallback, provider별 replacement,
  metadata/durability 제외와 orphan 운영 책임을 호출자가 판단할 수 있게 공개했다.
- 지원하지 않는 provider에서 실패가 정상 계약임을 명확히 해 silent non-atomic downgrade를
  피한다.
- `2.1.0-SNAPSHOT` 발행과 Image migration은 exact provider SHA/GAV/시각 및 remote
  resolution 증거 전까지 시작하지 않는 hold를 유지한다.

Finding: P0 0, P1 0, P2 0, P3 0.

## Main integration 수용 기준

| Acceptance | Source/Test/Doc evidence | Result |
|---|---|---|
| sibling temp와 close-before-move | `AtomicFileSupport.kt:68-90`, failure seam test | PASS |
| exact exception identity와 suppression | `AtomicFileSupport.kt:94-110,150-179`, failure policy test | PASS |
| no fallback와 provider replace 경계 | move option test, KDoc, README locale pair | PASS |
| counting과 no metadata reread | `CountingOutputStream`, operations surface, count test | PASS |
| path, concurrency, orphan/observability | filesystem concurrency test, KDoc, README locale pair | PASS |
| Kotlin/Java API와 ABI | 외부 caller test 4개, `javap -s` | PASS |
| 선행 develop 통합 | merge-base `8e5ff93d`, module 1,298/1,298 | PASS |

## 검증 증거

- current base rebase 뒤 신규 계약 test 24/24 PASS
- `:bluetape4k-io:test --rerun-tasks`: 1,298/1,298 PASS
- `compileKotlin`, `compileTestKotlin`, `compileTestJava`: PASS
- Detekt report의 `AtomicFile` finding 0
- Korean terminology audit 신규 finding 0, CHANGELOG baseline 대비 delta 0
- `git diff --check`: PASS

## 남은 검증 공백

- exact-head GitHub CI와 live review/thread는 PR 생성 뒤 확인한다.
- Full Nightly는 실행하지 않았다.
- Windows, network filesystem, unsupported provider와 process-crash fault injection은 로컬에서
  실행하지 않았다. API가 해당 환경을 성공으로 약속하지 않고 fail-closed provider 계약과
  orphan 운영 경계를 문서화했으므로 현재 P0/P1은 아니다.
- native six-perspective reviewer는 timeout으로 usable verdict를 만들지 못했다.

## 최종 판정

`PASS`. main-session exact-diff review와 integration 기준에서 P0=0, P1=0, P2=0이며,
P3 한 건은 수정했다. PR 전 final artifact, knowledge index와 exact-head 검증을 계속한다.
