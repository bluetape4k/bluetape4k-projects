# 원자적 파일 교체에서 provider 계약과 실패 identity를 분리하는 법

## 문제와 원인

기존 target을 새 payload로 교체할 때 target에 직접 쓰면 callback이나 stream close가
실패한 시점에 기존 내용까지 손상될 수 있다. sibling 임시 파일을 사용해도 일반 move로
fallback하거나 commit 뒤 cleanup을 다시 조회하면, 원자성 거부를 성공처럼 처리하거나
이미 이동된 파일의 후속 조회 실패로 정상 결과를 뒤집을 수 있다.

## ATOMIC_MOVE가 의미하는 범위

- callback과 provider stream close가 모두 성공한 뒤에만 sibling 임시 파일을 target으로
  이동한다.
- `ATOMIC_MOVE`와 함께 전달한 다른 option의 의미, 특히 기존 target 교체 여부는
  filesystem provider 계약을 따른다.
- provider가 atomic move를 지원하지 않거나 기존 target 교체를 거부하면 그대로
  실패한다. library는 일반 move fallback을 제공하지 않는다.
- atomic move 성공은 process crash나 power loss에 대한 `fsync` durability를 보장하지
  않는다. 기존 target의 permission, owner, ACL, xattr 보존도 별도 계약이다.

## callback-close-cleanup의 예외 순서

callback 실패가 primary이고 close도 실패하면 close failure를 첫 suppressed로 연결한다.
그 뒤 임시 파일 cleanup까지 실패하면 cleanup failure를 다음 suppressed로 연결한다.
callback이 성공하고 close가 실패하면 close failure가 primary다. commit 실패도 동일하게
primary가 되며 cleanup 실패만 suppressed로 추가한다.

이 정책은 `IOException`에만 적용하면 안 된다. callback, provider와 cleanup이 던진
unchecked exception, `CancellationException`, `Error`도 원래 instance를 유지해야 한다.
suppressed를 추가하기 전에는 같은 instance, 이미 등록된 instance와 양방향
cause/suppressed 도달성을 검사해 순환 graph를 만들지 않는다.

## commit 이후 cleanup lookup을 피하는 이유

성공한 atomic move 뒤에는 sibling 임시 파일 경로가 이미 target으로 이동됐다. 이때
`deleteIfExists` 같은 cleanup 조회를 수행하면 provider의 후속 오류가 성공한 교체 결과를
예외로 바꿀 수 있다. cleanup은 commit 이전 실패 경로에서만 실행하고, move가 반환되면
곧바로 기록한 byte 수를 반환한다. byte 수는 provider-owned counting stream에서 계산해
commit 뒤 `Files.size` 같은 metadata 재조회를 하지 않는다.

## metadata, trusted parent, crash orphan과 관측 책임

경로 정규화는 lexical 처리일 뿐 path sandbox가 아니다. symlink, hard-link, mount 교체,
TOCTOU 공격을 방어하지 않으므로 접근이 제한된 신뢰 가능한 parent에서만 사용한다.
민감한 payload에는 opaque basename과 private parent를 사용하고, 공격자가 제어하는 공유
writable directory에는 secure directory handle 기반 API를 적용한다.

process crash 뒤에는 `.<basename>.*.tmp`가 남을 수 있다. 애플리케이션 운영자는 private
parent의 파일 수와 사용량을 감시하고, 활성 writer가 없음을 확인한 뒤 설정한 보존 시간이
지난 항목만 bounded batch로 정리한다. writer 실행 중 무제한 glob 삭제를 수행하지 않는다.
telemetry에는 target/provider와 primary/suppressed 종류를 남기되 전체 경로, basename과
예외 메시지의 민감한 값은 가린다.

## downstream coroutine/S3 adapter 적용 규칙

- `writeAtomically`는 blocking API이므로 coroutine caller가 dispatcher를 선택한다.
- cancellation은 callback 밖에서 캡처한 context를 callback 반환 전에 검사한다. commit 뒤
  cancellation은 완료된 교체를 되돌리지 않는다.
- S3 download adapter는 remote stream과 local output의 소유권을 분리하고, remote read나
  local close가 실패하면 provider API의 primary/suppressed 계약을 보존한다.
- 다운로드 크기와 시간을 caller가 제한한다. local atomic commit은 remote object의
  정합성, checksum 또는 retry 정책을 대신하지 않는다.
- 소비자 전환은 provider merge, 새 개발 버전 발행, exact SHA/GAV/시각과 remote resolution,
  소비자 smoke, 독립 PR 순서로 진행한다.

## 재사용 체크리스트

1. sibling 임시 파일을 target과 같은 parent에 만드는가?
2. callback과 close가 성공하기 전에는 move를 호출하지 않는가?
3. atomic move 거부를 일반 move로 fallback하지 않는가?
4. primary failure identity와 suppressed 순서·비순환성을 테스트하는가?
5. 성공 뒤 cleanup 또는 metadata 조회를 수행하지 않는가?
6. different-target global serialization 부재와 same-target 완전 payload를 검증하는가?
7. metadata·durability 제외, trusted parent와 orphan 관측 책임을 문서화하는가?
8. Kotlin/Java caller와 공개 JVM descriptor를 함께 검증하는가?

## 결과와 검토에서 놓친 점

provider API, failure seam, filesystem/caller test와 locale 문서를 한 계약으로 묶었다.
초기 Detekt 실행은 `ignoreFailures` 때문에 성공 종료했지만 새 파일 finding 7건을 report에
남겼다. exit code만 확인하지 않고 report를 직접 읽은 뒤 cleanup 흐름을 `finally` 밖으로
옮기고 의도적인 `Throwable` 포착 범위를 표시했다. 이 수정 뒤 신규 finding은 0건이다.

계획 당시 base도 PR 직전에 전진해 있었다. 초기 module 성공을 재사용하지 않고 current
`origin/develop` 위로 rebase한 뒤 선행 bounded-input 변경을 포함한 전체 suite를 다시
실행했다. native six-perspective reviewer는 usable verdict 없이 timeout돼 main-session
exact-diff fallback으로 provenance를 분리했다. GNO collection은 main checkout을 가리켜
worktree lesson을 아직 찾지 못하므로 merge 뒤 knowledge read-back이 필요하다.

## 검증

- 신규 filesystem·실패 topology·Kotlin/Java caller 테스트 24개 통과
- current `origin/develop` 통합 뒤 `bluetape4k-io` 전체 1,298개 테스트 통과
- Kotlin/Java compile과 Detekt 신규 finding 0건
- `javap`로 `Path`, `Function1`, `long`, `throws IOException` JVM 계약 확인
- README 영어/한국어 계약과 CHANGELOG 연결 확인
