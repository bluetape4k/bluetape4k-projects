# Issue #1348 — LettuceJCache EntryProcessor 원자성

## 배경

`LettuceJCache.invoke`는 `MutableEntry`를 Redis hash에서 읽은 뒤 프로세서가
종료되면 별도의 `HSET` 또는 `HDEL`로 커밋했습니다. 서로 다른 Lettuce 연결이
같은 키를 동시에 갱신하면 두 호출이 같은 값을 읽고 마지막 쓰기가 앞선 갱신을
덮어쓰는 lost update가 발생했습니다. 기존 README의 `invoke`/`invokeAll` 미지원
문구도 실제 구현과 맞지 않았습니다.

## 결정

이번 stacked train의 PR B는 PR A `#1432`의 exact head 위에 다음 계약을 고정합니다.

- `LettuceMap`에 token-safe `SET NX PX` 분산 락과 소유 토큰 검증 Lua 해제를 추가합니다.
- 같은 `mapKey`를 사용하는 `LettuceJCache`의 변경 작업은 하나의 락 구간에서
  실행합니다. `invoke`는 프로세서 실행과 `MutableEntry` 커밋을 같은 구간에 둡니다.
- 프로세서가 예외를 던지면 커밋하지 않습니다. `invokeAll`은 키별 원자성과
  `EntryProcessorResult`별 성공/실패를 유지하며 전체 키 집합 트랜잭션은 약속하지 않습니다.
- 커밋은 기존 TTL과 listener event 계약을 유지합니다. Redis 8 `HSETEX` 경로에서도
  JCache가 약속한 hash-level TTL을 다시 적용합니다.

락 lease는 기본 1분, 획득 대기는 기본 5분이며, lease는 프로세서가 중단된 경우의
복구 상한입니다. 프로세서는 lease 안에 완료되어야 합니다.

## 검증

- RED: Ryuk 비활성화 조건에서 독립 연결 2개가 80회 동시 증가를 수행했을 때
  기존 구현은 `11`로 종료되어 `80`을 충족하지 못했습니다.
- GREEN: 같은 테스트가 분산 락 적용 후 `80`을 기록했습니다.
- 회귀 테스트: 프로세서 예외 시 원래 값 보존, `invokeAll` 키별 결과/예외,
  TTL 갱신, `CacheEntryUpdatedListener` 이벤트를 추가했습니다.
- 일반 Testcontainers 실행은 Colima Ryuk 소켓 마운트 오류로 시작하지 못했고,
  `TESTCONTAINERS_RYUK_DISABLED=true` 조건에서 전체 모듈 테스트를 재실행했습니다.

## 후속 guard

분산 락의 lease 만료 전파, 프로세서 실행 시간 정책, Redis Cluster hash-tag
키 전략을 별도 이슈에서 검토합니다. `invokeAll` 전체 집합 원자성이 필요한 사용자는
키별 결과 계약과 다른 API 경계를 사용해야 합니다.

## Stacked PR Train

```text
develop
└─ PR A #1432 / Issue #1426
   └─ PR B #1348 (현재 문서)
```

PR B는 PR A가 병합되기 전까지 `fix/1426-nearcache-observation`을 base로 유지하며,
두 PR의 병합은 별도 exact-head CI와 승인 게이트를 통과한 뒤에만 수행합니다.
