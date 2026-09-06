# Issue #1645 원자적 파일 교체 구현 계획 검토

## 검토 범위

- 대상 계획: `docs/superpowers/plans/2026-09-06-issue-1645-atomic-file-replacement-plan.md`
- 승인 사양: `docs/superpowers/specs/2026-09-06-issue-1645-atomic-file-replacement-design.md`
- provider 기준: `bluetape4k-projects@871f73aa8d464a01b9e71350e8220619c548684c`
- upstream 기준: `origin/develop@31ee966cf339f7b895284329c8fbd53638ab837c`
- 공개 API: `Path.writeAtomically((OutputStream) -> Unit): Long`
- 비범위: production 구현, Image #631 migration, merge, `2.1.0-SNAPSHOT` 발행

## 독립 6관점 1차 결과

| 관점 | 결과 | P0 | P1 | P2 | P3 | 핵심 발견 |
|---|---:|---:|---:|---:|---:|---|
| 성능 | PASS WITH P2 | 0 | 0 | 2 | 0 | bounded contention과 allocation/throughput 수용 기준 부족 |
| 안정성 | PASS WITH P2 | 0 | 0 | 1 | 0 | 여러 commit을 포함한 rollback 절차 불완전 |
| 보안 | PASS WITH P2 | 0 | 0 | 4 | 0 | lexical 경계, payload 한도, redaction, 임시 파일 permission |
| 운영 | PASS WITH P2 | 0 | 0 | 4 | 0 | orphan runbook, 개발 버전 발행 근거, rollback, exact-head 확인 |
| 개발자/API | PASS WITH P2 | 0 | 0 | 4 | 0 | test freshness, normalized target, close 실패, cancellation 예제 |
| 사용자/호출자 | CHANGES REQUIRED | 0 | 1 | 4 | 1 | crash durability 과장과 호출자용 Kotlin/Java 예제 누락 |

중복을 포함한 초기 합계는 `P0=0, P1=1, P2=19, P3=1`이다. 같은 README 예제와
rollback 문제가 여러 관점에서 독립적으로 나타났으므로, main integration은 원인별로
중복을 제거하고 수정 결과를 대조했다.

## 수정과 disposition

| 영역 | 최초 등급 | disposition | 계획 반영 |
|---|---:|---|---|
| README crash durability 문구 | P1 | 수정 | `fsync`, process crash, power loss에서 파일 durability를 보장하지 않는다고 명시했다. |
| bounded contention과 partial visibility | P2 | 수정 | 4 writer, 각 12회, concurrent reader와 sibling temp 누수 검사를 추가했다. |
| per-call allocation과 throughput | P2 | 부분 수정·나머지 유예 | production writer를 singleton으로 재사용한다. 공개 성능 SLA가 없고 provider·filesystem 환경 차이가 지배하므로 절대 throughput 기준은 두지 않는다. |
| 전체 변경 rollback | P2 | 수정 | 발행 전 모든 구현·문서·lesson commit 역순 revert와 merge 뒤 corrective revert를 구분했다. |
| lexical normalization 보안 경계 | P2 | 수정 | sandbox, symlink, hard-link, mount 교체, TOCTOU를 방어하지 않는다고 KDoc와 README에 배정했다. |
| unbounded payload 예제 | P2 | 수정 | caller가 byte 한도를 집행하는 bounded copy 예제와 실패 test를 추가했다. |
| 경로와 예외의 민감한 값 | P2 | 수정 | full path, basename, 민감한 exception text redaction을 문서와 lesson에 배정했다. |
| 임시 파일 permission | P2 | 수정 | provider 기본 permission과 access-restricted private parent 요구를 문서화했다. |
| crash orphan 운영 | P2 | 수정 | pattern, 활성 writer 제외, retention, bounded 삭제, file count와 allocated byte 감시를 runbook에 배정했다. |
| 개발 버전 발행 근거 | P2 | 수정 | exact SHA/GAV, workflow run, 발행 시각, Maven metadata와 override 없는 downstream resolution을 요구했다. |
| release rollback | P2 | 수정 | 발행 artifact를 삭제·덮어쓰지 않고 새 수정 provenance가 생길 때까지 Image #631을 차단한다. |
| PR exact-head 운영 gate | P2 | 수정 | local/remote/PR SHA, required checks, review와 unresolved thread를 PR 전후에 다시 읽는다. |
| test cache freshness | P2 | 수정 | targeted test에 `--no-build-cache`, module 회귀에 `--rerun-tasks`를 적용했다. |
| normalized move target | P2 | 수정 | operations seam의 `lastMoveTarget`을 실제 normalized target과 대조한다. |
| close-primary와 cleanup suppression | P2 | 수정 | 기존 target을 미리 만들고 close를 primary, cleanup을 suppressed로 검증한다. |
| coroutine cancellation 경계 | P2 | 수정 | caller context를 callback 밖에서 캡처해 commit 전 검사하고 dispatcher는 caller가 선택한다. |
| Kotlin extension import와 예제 compile | P2 | 수정 | 외부 `readme` package test가 실제 import와 bounded/coroutine helper를 compile·실행한다. |
| Java facade와 checked exception | P2 | 수정 | `AtomicFileSupport` import와 `throws IOException` method를 README 및 외부 Java test에 같은 형태로 둔다. |
| 자동 생성 parent의 실패 뒤 잔존 | P2 | 수정 | 두 README와 filesystem test에 rollback하지 않는 계약을 추가했다. |
| unchecked failure identity | P2 | 수정 | stream open의 `AssertionError` identity와 staged cleanup을 operations seam에서 검증한다. |
| 한국어 번역투 | P3 | 수정 | durability, fallback, 모듈 구조 문장을 자연스러운 한국어로 고쳤다. |

별도 follow-up issue로 넘긴 기능 결함은 없다. 유예한 절대 throughput benchmark는 공개
성능 계약이 없고 결과가 provider 환경에 종속되기 때문에 현재 API의 수용 기준으로 삼지
않는다. 대신 추가 metadata 조회가 없는 구현 구조와 bounded contention 회귀로 해당 위험을
검증한다.

## 사용자/호출자 영향 재검토

| 관점 | 결과 | P0 | P1 | 잔여 사항 |
|---|---:|---:|---:|---|
| 사용자/호출자 | PASS WITH P2 | 0 | 0 | Kotlin/Java import, exact snippet compile, parent 잔존, unchecked identity와 한국어 표현을 main integration에서 수정했다. |

초기 P1인 crash durability 과장은 독립 재검토에서 닫혔다. 재검토가 남긴 P2/P3도 위
수정으로 계획과 test fixture에 반영했다.

## Step 3-R 필수 검사

| # | 검사 | 상태 | 근거 |
|---:|---|---:|---|
| 1 | 사양과 DoD를 concrete task에 연결 | PASS | 마지막 spec-to-task 표가 모든 acceptance와 DoD를 Task 1~5에 연결한다. |
| 2 | 현재 codebase에서 실행 가능한 순서 | PASS | RED 두 단계, GREEN, 문서, broader 검증, review·PR 순서다. |
| 3 | 이후 task artifact에 대한 선행 의존 없음 | PASS | test fixture가 먼저 생기고 production, 문서, broader 검증이 순차적으로 따른다. |
| 4 | 관련 success/failure/edge/concurrency/coroutine/lifecycle/backend case | PASS | 실제 filesystem, operations seam, concurrent reader/writer, coroutine caller, provider 거부를 포함한다. |
| 5 | concrete targeted verification command | PASS | test class selector, module rerun, compile, Detekt, `javap`, diff 명령을 고정했다. |
| 6 | README locale pair | PASS | `io/io/README.md`와 `README.ko.md`에 같은 구조와 예제를 배정했다. |
| 7 | 한국어 KDoc·CHANGELOG·PR·lesson | PASS | Task 2, 3, 5에서 artifact와 검증을 지정했다. |
| 8 | 새 module 등록/BOM/CI/coverage | N/A | 기존 `bluetape4k-io`에 API만 추가하고 module·artifact를 만들지 않는다. |
| 9 | Spring Boot 조건·등록 순서 | N/A | Spring Boot auto-configuration을 변경하지 않는다. |
| 10 | Exposed import·receiver shadowing | N/A | Exposed 코드를 변경하지 않는다. |
| 11 | coroutine cancellation·dispatcher | PASS | dispatcher와 commit 전 cancellation 검사를 caller-owned 예제와 compile test로 고정했다. |
| 12 | allocation·blocking·cleanup·stability | PASS | singleton writer, blocking 표기, lifecycle cleanup, bounded contention과 fresh rerun을 포함한다. |
| 13 | cross-module duplication 결정 | PASS | Projects 공통 API를 먼저 제공하고 Image의 세 중복 consumer는 발행 provenance 뒤 별도 migration한다. |
| 14 | rollback·compatibility·migration 위험 | PASS | pre/post-merge rollback, provider compatibility와 downstream hold가 명시돼 있다. |

조건부 검사 중 domain-constrained field, streaming terminal protocol, JDK preview API는 현재
범위에 해당하지 않는다. resource lifecycle과 callback borrowing, coroutine cancellation은
각각 Task 2와 Task 3에서 검증한다.

## Machine evidence

- run: `20260906T063550Z-69429917`
- 완료 lane: `plan-performance`, `plan-stability`, `plan-security`, `plan-operations`,
  `plan-developer`, `plan-user`, `plan-rereview-user`
- receipt sequence: `94`
- unresolved failed lane: 없음

## 현재 판정

`PASS`. six-perspective plan review와 사용자/호출자 영향 재검토가 `P0=0, P1=0`으로
수렴했다. 모든 P2/P3는 계획 수정 또는 근거가 있는 수용 기준 유예로 disposition했다.
production 구현은 committed plan의 사용자 승인 뒤에 시작한다.
