# 이슈 #851 Mutiny Coroutine `asUni` cold contract

issue #851은 `CoroutineScope.asUni { ... }`가 `Deferred`를 즉시 만들고 이를 `Uni`로
변환한다는 점을 찾았다. 그 결과 suspend block이 hot해져 subscriber가 없어도 `Uni`를
생성하는 것만으로 side effect가 시작됐다.

## 결정

Mutiny가 subscription 시점에 emitter consumer를 호출하므로 coroutine은
`Uni.createFrom().emitter { ... }` 안에서 만든다. bridge는 subscriber마다 fresh
`Deferred`를 만들고, `invokeOnCompletion`에서 completion/failure를 forward하며, coroutine
완료 전에 Uni subscription이 terminate되면 `Deferred`를 cancel한다.

## 교훈

- reactive bridge helper는 reactive demand semantic과 맞아야 한다. subscription 전에 work를
  시작하는 `Uni` factory는 side-effect leak이다.
- `Deferred.asUni()`는 result와 cancellation forwarding에는 유용하지만, `Deferred` 자체가
  subscription time에 생성될 때만 cold하다.
- cancellation test는 양방향을 모두 증명해야 한다. subscription cancel은 coroutine을
  cancel하고, coroutine cancellation/failure는 Uni에 도달해야 한다.

## 검증

- RED: `./gradlew :bluetape4k-mutiny:test --tests "io.bluetape4k.mutiny.CoroutineSupportTest.asUni does not start suspend block before subscription" --no-build-cache`가 `Expected <1> to equal to <0>`로 실패했다.
- GREEN targeted: `./gradlew :bluetape4k-mutiny:test --tests "io.bluetape4k.mutiny.CoroutineSupportTest.asUni does not start suspend block before subscription" --tests "io.bluetape4k.mutiny.CoroutineSupportTest.asUni cancellation cancels running coroutine" --tests "io.bluetape4k.mutiny.CoroutineSupportTest.asUni propagates failure and cancellation exceptions" --no-build-cache`
- module: `./gradlew :bluetape4k-mutiny:compileKotlin :bluetape4k-mutiny:compileTestKotlin :bluetape4k-mutiny:test --no-build-cache`가 29 tests로 통과했다.
- build: `./gradlew :bluetape4k-mutiny:build --no-build-cache`가 통과했다.
