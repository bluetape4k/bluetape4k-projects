# Virtual Thread State Flake Lessons

## 배경

PR #427 merge 이후 develop push의 `Examples` workflow가 `Example4_VirtualThreadFactory`에서 실패했다.
실패한 assertion은 새로 시작한 virtual thread가 `RUNNABLE`일 것으로 기대했지만 runner는 `TIMED_WAITING`을 관찰했다.

## 결정 또는 발견

Virtual thread scheduler state는 관찰값이지 안정적인 contract가 아니다. Test는 `start()` 직후 하나의 transient
`Thread.State`만 assert하면 안 된다. Test 대상 behavior가 concurrent execution이면 `CountDownLatch` 같은
lifecycle handshake나 `bluetape4k-junit5` concurrency harness를 사용한다.

## 결과

Example은 안정적인 lifecycle contract를 증명한다.

- factory가 unstarted virtual thread를 만든다.
- thread body가 시작되고 entry를 signal한다.
- test가 release latch를 잡고 있는 동안 thread가 alive 상태를 유지한다.
- release와 `join` 이후 thread가 종료된다.

## 검증

이 failure mode에는 targeted verification을 사용한다.

- `repo-test-summary -- ./gradlew :bluetape4k-examples-virtualthreads-demo:test --tests "io.bluetape4k.examples.virtualthreads.part1.Example4_VirtualThreadFactory" --rerun-tasks`

## 향후 지침

Virtual-thread example에서는 transient scheduler state assertion보다 deterministic lifecycle signal을 선호한다.
Runner timing에 따라 `RUNNABLE`, `WAITING`, `TIMED_WAITING`은 모두 valid observation일 수 있다.
