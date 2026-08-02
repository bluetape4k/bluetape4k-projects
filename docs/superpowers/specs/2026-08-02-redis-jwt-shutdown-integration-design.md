# Redis-backed JWT shutdown integration 설계

## 목표

Issue #1295의 목적은 실제 Redis와 Redisson을 통과하는 JWT key-chain 및
cache provider의 종료 경로를 검증하는 것이다. 검증은 애플리케이션이
주입한 `RedissonClient`의 소유권을 JWT provider/repository가 가져가지 않는
계약과, 각 객체가 소유한 timer만 취소하는 계약을 동시에 고정한다.

## 현재 근거

- `RedisKeyChainRepository`는 `AbstractKeyChainRepository`의 refresh timer와
  Redis deque/rotation lock을 사용하지만 주입된 `RedissonClient`를 직접 닫지
  않는다.
- `DefaultJwtProvider`는 `forTesting` 내부 생성 경로로 짧은 rotation interval을
  사용할 수 있고, 주입된 repository를 빌려 쓰며 자체 timer만 닫는다.
- `RedissonJwtProvider`는 delegate를 위임하고 Redis map cache를 빌려 쓰므로
  delegate timer나 client 종료 책임을 갖지 않는다. 실제 rotation timer는
  `DefaultJwtProvider`가 소유하므로 호출자가 delegate를 별도로 닫아야 한다.
- `RedisServer`와 `ToxiproxyServer`는 이미 Testcontainers와 shared Network
  패턴을 제공한다. `testcontainers-toxiproxy` catalog alias는 존재하지만
  `bluetape4k-testcontainers`의 `compileOnly` dependency는 JWT 테스트에
  transitive하게 노출되지 않는다.

## 대안 비교

### A. JWT 테스트에 ToxiProxy client를 직접 연결한다 (채택)

`utils/jwt/build.gradle.kts`의 test scope에 `libs.testcontainers.toxiproxy`를
추가하고, 새 테스트가 `Network.newNetwork()` 안에서 Redis와 ToxiProxy를
직접 시작한다. proxy를 Redisson address로 사용하여 정상 요청, route
disable 중의 bounded failure, enable 후 recovery를 순서대로 검증한다.

장점은 acceptance criteria를 가장 작은 변경으로 충족하고 공용 테스트
인프라 API를 변경하지 않는다는 점이다. 단점은 Docker가 필요한 테스트가
하나 추가된다는 점이며, 이 테스트는 다른 Testcontainers 모듈과 병렬 실행하지
않는다.

### B. 공용 `ToxiproxyServer`에 Redis proxy factory를 추가한다 (제외)

테스트 인프라 모듈에 proxy 생성/정리 helper를 추가하면 호출부는 짧아지지만,
공용 API와 문서/테스트를 함께 변경하고 JWT의 단일 integration 요구를
테스트 인프라 설계로 확장하게 된다. 현재 helper의 동작이 이미 충분하므로
YAGNI에 맞지 않는다.

### C. Redis만 사용하는 shutdown 통합 테스트를 추가한다 (제외)

실제 Redisson ownership과 close 순서는 확인할 수 있지만 route interruption과
recovery를 재현하지 못해 issue의 핵심 수용 기준을 충족하지 못한다.

## 선택한 구성과 수명주기

1. `Network.newNetwork()`를 만들고 `RedisServer`를 `redis` alias로, 이어서
   `ToxiproxyServer`를 같은 network에 시작한다.
2. `ToxiproxyClient`로 `0.0.0.0:8666 -> redis:6379` proxy를 만들고, host의
   mapped proxy port를 짧은 timeout/retry Redisson config의 address로 사용한다.
3. 실제 `RedisKeyChainRepository`와 짧은 interval의
   `DefaultJwtProvider.forTesting`을 만들고, 그 delegate와 Redis map cache를
   `RedissonJwtProvider`에 주입한다.
4. JWT 생성/파싱과 forced rotation으로 key rotation 및 cache parsing을
   확인한다. `proxy.disable()` 중 provider의 forced rotation이 정해진 짧은
   시간 안에 false로 끝나는지 확인하고, `proxy.enable()` 후 같은 경로가
   성공하는지 확인한다.
5. wrapper provider를 두 번 닫고, delegate를 두 번 닫고, repository를 두 번
   닫는다. wrapper/repository/delegate close가 `RedissonClient`를 종료하지
   않는지 확인한 뒤 application ownership으로 client를 닫고 `isShutdown`
   terminal state를 확인한다.
6. delegate를 닫은 뒤 만료된 key-chain을 Redis에 직접 넣고 refresh interval
   이상 기다려도 timer가 새 회전을 만들지 않는지 deque의 key id로 확인한다.
   이는 process-wide thread-name 검사 없이 delegate-owned background work의
   종료를 입증한다.

## 오류와 경계

- ToxiProxy disable 중의 Redis 명령은 Redisson config의 짧은 timeout과
  `retryAttempts = 0`으로 bounded failure가 된다.
- proxy가 복구되면 새 요청은 반드시 같은 client를 통해 성공해야 한다.
- provider/repository close는 borrowed delegate, client나 Redis container를
  닫지 않는다. delegate의 rotation timer는 delegate close가 소유하며,
  container와 client는 테스트의 외부 owner가 명시된 순서로 정리한다.
- proxy/client/container 정리는 `finally`에서 각각 best-effort로 수행하며,
  테스트 본문의 primary assertion failure를 덮지 않는다.
- Redis cluster/failover, production API 변경, process-wide thread enumeration은
  이 설계의 범위가 아니다.

## 수용 기준 대응

| 기준 | 검증 방법 |
| --- | --- |
| 실제 Redis + Redisson + JWT | shared Network의 RedisServer와 proxy 주소를 쓰는 integration test |
| proxy interruption/recovery | `disable` 중 bounded rotation failure, `enable` 후 성공 |
| ownership 분리 | provider/delegate/repository close 뒤 client가 살아 있고, 명시적 client shutdown 후만 `isShutdown` |
| idempotent close 및 timer 종료 | 반복 close와 delegate close 후 만료 key-chain 재회전 억제 확인 |
| 문서화 | README 두 locale에 Redis-backed ownership/close 순서 추가 |
| 품질 | targeted integration test, JWT detekt, diff-check 및 proportional module test |

## Definition of Done

- test-only dependency만 추가되고 production ownership/API는 변경하지 않는다.
- 테스트가 Docker가 있는 로컬 환경에서 통과하고, hosted CI에서 Docker 결과가
  없으면 PR에 그 사실을 명시한다.
- README/KDoc 설명은 실제 코드의 close 순서와 일치한다.
- 변경 파일의 `git diff --check`, targeted test, detekt, module test 결과와
  exact PR head를 DoD에 남긴다.
