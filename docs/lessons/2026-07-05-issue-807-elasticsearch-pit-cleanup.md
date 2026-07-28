# 이슈 807: Elasticsearch PIT cleanup

## 배경

`searchAsFlow`는 Point-in-Time context를 열고 collector coroutine의 `finally` block에서
닫았다. Cancellation 중에는 suspend close call이 Elasticsearch에 도달하기 전에
취소될 수 있다.

## 결정

PIT close를 작은 `NonCancellable` best-effort cleanup helper로 옮긴다. Cleanup
failure는 원래 collector cancellation이나 upstream failure를 대체하지 않고 log한다.

## 결과

새 unit test는 parent coroutine이 취소된 뒤에도 suspend cleanup이 완료됨을 증명하며,
기존 Elasticsearch integration test도 계속 통과한다.

## 향후 지침

Coroutine Flow가 remote resource를 열고 suspend call로 닫는다면 close path를
`NonCancellable` cleanup boundary 뒤에 두고, heavy infrastructure test와 분리해 helper
자체를 테스트한다.
