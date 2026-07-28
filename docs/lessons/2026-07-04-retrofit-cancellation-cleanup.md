# Retrofit cancellation race는 전달된 response body를 닫아야 한다

## 배경

이슈 #948은 coroutine cancellation 이후 response가 도착할 수 있지만 명시적인
response-body cleanup evidence가 없는 Retrofit coroutine bridge race를 발견했다.

## 결정

`onResponse` 전에 cancellation이 이기면 continuation을 취소하기 전에 Retrofit
response body를 닫는다. Resume 이후 dispatch 전에 cancellation이 이기면 `resume`
cancellation handler에서 전달된 response도 닫는다.

## 검증

- `./gradlew :bluetape4k-retrofit2:test --tests 'io.bluetape4k.retrofit2.SuspendRetrofitCallSupportTest'`
- `git diff --check`

## 향후 지침

Coroutine HTTP bridge는 모든 cancellation race path에서 response resource를 닫아야
한다. 전달 전에 명시적인 `!cont.isActive` 확인과 `resume(value) { ... }` cleanup
handler를 함께 사용하는 방식을 우선한다.
