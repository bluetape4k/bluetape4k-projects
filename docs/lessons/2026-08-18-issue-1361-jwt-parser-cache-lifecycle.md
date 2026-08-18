# Issue #1361: JWT parser cache 수명주기

## 문제

`JwtParserSupport`는 `ConcurrentHashMap<JwtProvider, JwtParser>`에 parser를
보관합니다. parser가 만든 `Locator`는 provider의 `findKeyChain`을 캡처하므로,
provider를 닫아도 캐시 엔트리가 남아 있으면 provider와 repository가 전역 캐시에서
해제되지 않습니다. 기존 #1276 수정은 `DefaultJwtProvider`의 rotation timer 종료만
다뤘고 parser 캐시 수명은 다루지 않았습니다.

## 원인과 결정

`DefaultJwtProvider.close()`가 timer만 취소하고 `jwtParserCache`에서 자신의 entry를
제거하지 않는 것이 직접 원인입니다. provider별 parser를 계속 재사용하는 동시성 계약은
유지해야 하므로 캐시 구조나 의존성을 넓히지 않고, 공통 `clearJwtParserCache()`를
추가했습니다. 기본 `JwtProvider.close()`는 모든 구현체의 parser entry를 제거하고,
timer를 소유한 `DefaultJwtProvider.close()`도 lifecycle lock 안에서 같은 정리를 수행합니다.
주입받은 `KeyChainRepository`의 소유권과 close 순서는 변경하지 않았습니다.

## 결과

- provider가 살아 있는 동안 동시 parser 요청은 하나의 parser를 재사용합니다.
- `close()`는 idempotent timer 종료와 함께 해당 provider의 parser 캐시 엔트리를 제거합니다.
- 반복 생성·파싱·종료한 provider는 `jwtParserCache`에 강한 참조 키를 남기지 않습니다.

## 검증

- RED: 수정 전 `JwtParserSupportTest`에서 close 후 entry가 남아 `Expected <true> to be <false>`가 발생했고, 32개 provider 종료 후 32개 entry가 남았습니다.
- GREEN: `./gradlew :bluetape4k-jwt:test --tests 'io.bluetape4k.jwt.provider.JwtParserSupportTest' --no-configuration-cache` — 2 passing.
- 모듈: `./gradlew :bluetape4k-jwt:test --no-configuration-cache` — 160 passing, 10 pending.
- 첫 전체 실행의 Redis watchdog 단일 실패는 해당 테스트 단독 재실행에서 성공했으며, 캐시 변경과 무관한 retry-only 인프라 변동으로 분류했습니다.

## 다음 guard

provider가 parser, locator, callback처럼 provider를 캡처하는 전역 캐시를 추가할 때는
생성·사용·`close()` 후 entry 제거를 하나의 lifecycle 테스트로 고정합니다. map entry
제거가 실제 강한 참조 경계를 닫는지 확인하고, 별도 bounded/weak 정책을 도입할 때는
동시 재사용과 종료 경합을 다시 측정합니다.
