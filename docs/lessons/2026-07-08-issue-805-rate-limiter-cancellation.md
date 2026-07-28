# 이슈 805: Distributed rate limiter cancellation

## 배경

`DistributedSuspendRateLimiter`는 `withTimeout`을 사용한 뒤
`CancellationException`보다 먼저 `TimeoutCancellationException`을 잡았다. Limiter가
소유한 timeout 경로는 명확했지만, async bucket boundary에서 온 timeout-shaped
cancellation도 `RateLimitResult.ERROR`로 변환될 수 있었다.

## 결정

Limiter-owned timeout은 `withTimeoutOrNull`로 명시적으로 모델링하고, 그 `null` 결과만
`RateLimitResult.ERROR`로 변환한다. General error handling 전에 모든
`CancellationException`은 다시 던진다.

## 결과

Distributed suspend test는 이제 세 cancellation boundary를 다룬다. Cancelled await,
async timeout cancellation, `defaultTimeout` 유무에 따른 caller `withTimeout`이다.
기존 limiter-owned timeout test는 계속 `RateLimitStatus.ERROR`를 반환한다.

## 향후 지침

Suspend boundary 주변에서 `TimeoutCancellationException`을 ordinary error로 잡지
않는다. Component가 timeout을 소유할 때는 명시적인 timeout result boundary를 사용하고,
coroutine cancellation은 일반 `CancellationException` 경로로 전파한다.
