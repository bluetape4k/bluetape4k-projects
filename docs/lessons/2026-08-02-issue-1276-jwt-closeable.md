# JWT 백그라운드 타이머의 명시적 수명주기

## 배경

JWT `KeyChainRepository`와 `DefaultJwtProvider`가 인스턴스마다 daemon `Timer`를 생성하지만, 호출자가 이를 종료할 공통 계약이 없었다. 반복적인 tenant/provider 재생성이나 테스트 fixture 폐기 뒤에도 timer task가 소유 객체를 붙잡을 수 있는 상태였으며, 이 문제를 #1276에서 추적했다.

## 원인

`AbstractKeyChainRepository`와 `DefaultJwtProvider`가 `init`에서 `kotlin.concurrent.timer`를 시작하고도 `KeyChainRepository`와 `JwtProvider`에 `close()` 계약을 제공하지 않았다. 따라서 timer의 `cancel()`을 호출할 안정적인 public lifecycle 경계가 없었다.

## 결정

- 두 public contract는 `AutoCloseable`을 상속하고, 백그라운드 작업이 없는 구현체를 위해 idempotent no-op 기본 `close()`를 제공한다.
- `AbstractKeyChainRepository.close()`는 repository가 소유한 refresh timer만 취소한다. timer callback과 close 사이의 경합은 lifecycle lock으로 직렬화한다.
- `DefaultJwtProvider.close()`는 provider가 소유한 rotation timer만 취소한다. 주입받은 repository는 borrowed resource이므로 닫지 않으며, 호출자가 provider와 repository를 각각 닫는다.
- cache provider는 delegate를 borrowed resource로 취급한다. delegate의 timer 수명은 원래 delegate 호출자가 관리한다. 외부 Redisson client도 repository가 닫지 않는다.

## 검증

`JwtLifecycleTest`는 짧은 refresh/rotation 주기로 callback이 실제 실행된 뒤 `close()`를 호출하고, 이후 callback count가 증가하지 않는지 검증한다. repository timer가 idempotent하게 종료되고 provider를 닫아도 borrowed repository timer가 살아 있는지도 확인한다. 전체 `:bluetape4k-jwt:test`는 157 passing, 10 pending으로 완료됐다.

## 향후 변경 지침

새 JWT repository/provider가 timer, executor, scheduler 같은 백그라운드 자원을 소유하면 public lifecycle 계약과 deterministic cancellation 테스트를 같은 변경에 포함한다. 주입받은 자원의 소유권은 자동으로 전이된다고 가정하지 말고 KDoc과 README에 borrow/own 경계를 명시한다.
