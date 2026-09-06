# Issue #1645 원자적 파일 쓰기 사양 검토

## 검토 범위

- 대상: `docs/superpowers/specs/2026-09-06-issue-1645-atomic-file-replacement-design.md`
- provider 기준: `bluetape4k-projects@31ee966cf339f7b895284329c8fbd53638ab837c`
- downstream 기준: `bluetape4k-image@f4a656bea134f1dc890bf6306e000404754ba763`
- 공개 API: `Path.writeAtomically((OutputStream) -> Unit): Long`
- 비범위: 구현, Image migration, merge, `2.1.0-SNAPSHOT` 발행

## 독립 6관점 1차 결과

| 관점 | 결과 | P0 | P1 | P2 | P3 | 핵심 발견 |
|---|---:|---:|---:|---:|---:|---|
| 성능 | PASS | 0 | 0 | 2 | 0 | `Files.size` 추가 조회와 concurrency 검증 누락 |
| 안정성 | CHANGES REQUIRED | 0 | 4 | 3 | 0 | provider move 계약, exception topology, failure seam, S3 close/cancellation |
| 보안 | CHANGES REQUIRED | 0 | 1 | 2 | 0 | trusted parent와 lexical normalization/TOCTOU 경계 |
| 운영 | CHANGES REQUIRED | 0 | 2 | 4 | 1 | metadata, parent rollback, orphan temp, artifact provenance, 관측 책임 |
| 개발자/API | CHANGES REQUIRED | 0 | 2 | 2 | 0 | 컴파일 불가 coroutine 예, JVM facade·Java checked exception 계약 |
| 사용자/호출자 | CHANGES REQUIRED | 0 | 3 | 1 | 0 | coroutine 예, provider replace, commit 뒤 cleanup 모호성, Java 사용법 |

중복을 포함한 초기 합계는 `P0=0, P1=12, P2=14, P3=1`이다. 같은 JDK
provider 계약과 coroutine 예제 문제가 여러 관점에서 독립적으로 발견됐으므로, 최종
판정에서는 finding 수를 단순 합산하지 않고 각 원인과 수정 결과를 대조한다.

## 공식 JDK 계약 확인

독립 `researcher` lane과 main-session read-back이 Oracle Java SE 25 문서를 같은 의미로
확인했다.

- [`Files.move`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html#move(java.nio.file.Path,java.nio.file.Path,java.nio.file.CopyOption...)):
  `ATOMIC_MOVE`가 적용되면 다른 option은 무시된다. 기존 target을 교체할지
  `IOException`으로 거부할지는 provider별 동작이며, atomic move가 불가능하면
  `AtomicMoveNotSupportedException`이다.
- [`Files.createTempFile`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html#createTempFile(java.nio.file.Path,java.lang.String,java.lang.String,java.nio.file.attribute.FileAttribute...)):
  임시 파일 이름과 기본 permission은 provider 경계이며 기존 target attribute 상속을
  보장하지 않는다.
- [`Files.createDirectories`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html#createDirectories(java.nio.file.Path,java.nio.file.attribute.FileAttribute...)):
  실패 전에 일부 parent directory가 생성될 수 있다.

## 수정과 disposition

| 원인 | 최초 등급 | disposition | 사양 반영 |
|---|---:|---|---|
| `ATOMIC_MOVE + REPLACE_EXISTING`의 portable replace 과장 | P1 | 수정 | 성공은 provider 조건부이며 모든 거부 예외를 fallback 없이 전파한다. |
| non-suspend callback 안의 suspend 함수 호출 | P1 | 수정 | `CoroutineContext`를 먼저 캡처하고 callback에서 `ensureActive()`를 호출한다. |
| callback·close·cleanup 동시 실패 순서 미정 | P1 | 수정 | callback primary, close 첫 suppressed, cleanup 다음 suppressed로 고정한다. |
| parent/temp/open failure seam 누락 | P1 | 수정 | 단계별 호출·cleanup·기존 target 보존 검증을 추가한다. |
| S3 source close와 최종 cancellation 검사 | P1 | 수정 | callback 반환 전 close·검증·`ensureActive()` 순서와 회귀를 고정한다. |
| trusted parent와 TOCTOU 경계 | P1 | 수정 | 공유 writable 경로를 제외하고 secure directory handle 경로를 안내한다. |
| Java facade·checked exception·실제 호출 계약 | P1 | 수정 | `AtomicFileSupport`, `@Throws(IOException::class)`, Java fixture와 예제를 고정한다. |
| commit 성공 뒤 cleanup 예외의 모호한 결과 | P1 | 설계 변경 | 성공 move 뒤 cleanup lookup을 생략해 해당 상태를 제거한다. |
| byte 수 확인의 metadata round trip | P2 | 설계 변경 | provider-owned counting stream으로 반환값을 계산하고 `Files.size`를 쓰지 않는다. |
| global/same-target concurrency proof | P2 | 수정 | barrier/latch 기반 결정적 acceptance를 추가한다. |
| 기존 target metadata와 temp permission | P1/P2 | 문서화 | staged/provider attributes를 따르며 기존 권한·owner·ACL·xattr를 보존하지 않는다. |
| streaming resource limit | P2 | 문서화 | remote/untrusted 입력 한도는 callback 안에서 caller가 집행한다. |
| empty/root receiver 검사 순서 | P2 | 수정 | absolute 변환 전에 원래 receiver의 파일명을 검사한다. |
| parent rollback·orphan temp·observability | P2/P3 | 문서화 | parent 잔존, crash orphan 패턴·정리, caller-owned 로그·metric을 명시한다. |
| downstream artifact provenance | P2 | 수정 | exact SHA/GAV/발행 시각과 local override 없는 remote resolution을 요구한다. |
| stream 유효 기간과 KDoc 항목 | P2 | 수정 | callback 실행 중 사용, close·보관 금지와 public 계약 항목을 열거한다. |
| suppressed 중복·순환 graph | P2 | 수정 | identity 중복과 양방향 reachability를 거부한다. |

모든 P2/P3는 사양 수정 또는 명시적인 provider/caller-owned 범위로 처리했다. 별도
follow-up issue로 넘긴 항목은 없다.

## Lane 복구 기록

첫 operations/developer/user lane은 사용자 steering으로 turn 경계가 바뀔 때 결과를
남기지 못하고 native 목록에서 사라졌다. 세 lane을 실패로 기록한 뒤 동일한 read-only
범위의 replacement lane을 한 번씩 실행하고 원본/대체 evidence digest를 결합해
resolution을 완료했다. performance/stability/security와 공식 JDK researcher lane은
원본 실행에서 완료됐다.

## 재검토

| 관점 | 결과 | P0 | P1 | 잔여 사항 |
|---|---:|---:|---:|---|
| 보안 | PASS | 0 | 0 | trusted parent와 TOCTOU 경계가 닫혔다. |
| 안정성 | PASS | 0 | 0 | provider 의미, 예외 순서, failure seam, S3 lifecycle이 닫혔다. |
| 운영 | PASS WITH P2 | 0 | 0 | 관측 책임의 KDoc/README 수용 기준 추적성을 본 세션에서 보완했다. |
| 개발자/API | PASS | 0 | 0 | coroutine 예와 Java facade·호출 검증 계약이 닫혔다. |
| 사용자/호출자 | PASS | 0 | 0 | 교체 의미, commit 뒤 cleanup, Java 사용 계약이 닫혔다. |

성능 관점은 1차 검토에서 이미 `P0=0, P1=0`이었고 byte counting과 결정적 concurrency
검증을 사양에 반영했다. 영향받은 다섯 관점의 재검토도 모두 `P0=0, P1=0`으로
수렴했다. 운영 관점의 잔여 P2는 KDoc/README 검증 목록과 수용 기준에 caller-owned
관측 책임을 추가해 main integration에서 닫았다.

## 현재 판정

`PASS`. six-perspective review와 영향 관점 재검토가 `P0=0, P1=0`으로 수렴했고,
모든 P2/P3를 사양 수정 또는 명시적 provider/caller-owned 경계로 disposition했다.
구현 계획 진입은 별도의 written-spec 사용자 승인 뒤에 수행한다.
