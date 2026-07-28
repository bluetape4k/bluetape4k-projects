# withKryoAsync 취소 시 Kryo Pool 누수

**날짜**: 2026-05-16
**이슈**: #480
**브랜치**: `fix/kryo-async-leak`

## 근본 원인

`withKryoAsync`는 `supplyAsync` **밖에서** `Kryo` instance를 얻고 `whenCompleteAsync`로 반환했다:

```kotlin
// BEFORE (leak risk)
val kryo = KryoProvider.obtainKryo()                  // caller thread에서 획득
return CompletableFuture.supplyAsync { func(kryo) }
    .whenCompleteAsync { _, _ -> KryoProvider.releaseKryo(kryo) }  // cancellation 시 실행되지 않을 수 있음
```

누수 경로는 두 가지다:

1. **시작 전 취소**: caller가 자기 thread에서 `Kryo`를 획득했지만, 반환된 future가
   `supplyAsync` 실행 전에 취소되면 `whenCompleteAsync`가 trigger되지 않고 instance가 반환되지 않는다.
2. **`whenCompleteAsync` stage 취소**: `whenCompleteAsync`는 새 `CompletableFuture`를 반환한다.
   그 stage가 취소되거나 callback이 실행되지 않으면(예: parent stage가 exceptionally complete되고
   새 stage가 즉시 취소되는 경우), `releaseKryo`가 건너뛰어진다.

## 수정

`try/finally`를 사용해 `obtainKryo`와 `releaseKryo`를 `supplyAsync` lambda **안으로** 옮긴다:

```kotlin
// AFTER (fix)
return CompletableFuture.supplyAsync {
    val kryo = KryoProvider.obtainKryo()
    try {
        func(kryo)
    } finally {
        KryoProvider.releaseKryo(kryo)
    }
}
```

- Future가 supplier 실행 **전에** 취소되면 `obtainKryo`가 호출되지 않으므로 누수가 없다.
- Future가 실행 **중에** 취소되면 supplier는 worker thread에서 완료까지 계속 실행되고,
  worker가 종료되기 전에 `finally`가 무조건 실행되어 instance를 반환한다.
- `func`의 exception도 synchronous `withKryo` pattern과 동일하게 `finally`를 trigger한다.

## 테스트 범위

새 `KryoSupportTest.kt`는 helper function 5개를 모두 다룬다:

- `withKryo` — normal path와 exception path(두 경우 모두 pool 반환)
- `withKryoOutput` — normal path와 exception path
- `withKryoInput` — normal path와 exception path
- `withKryoAsync` — normal path, null return, exception path(20회 실패 후에도 pool 고갈 없음),
  cancellation path(latch 기반 synchronization, `Thread.sleep` 없음)
- `withKryoSuspending` — normal path와 exception path

Cancellation test는 user func의 `finally` block에서 signal되는 `funcCompleted` `CountDownLatch`를
사용한다. Framework의 `releaseKryo`는 user func가 반환된 직후(user `finally` 포함) 실행되므로,
`funcCompleted` 대기만으로 임의의 `Thread.sleep` 없이 충분한 synchronization이 된다.

## 핵심 교훈

**Resource는 task boundary 밖이 아니라 안에서 획득한다.**
Async task가 시작 전에 취소될 수 있다면 `supplyAsync` 전에 획득한 resource는 누수 위험이 있다.
항상 executor lambda 내부에서 `obtain`/`release` pair의 scope를 잡고, success, exception, interrupt
등 모든 exit path에서 release되도록 `try/finally`를 사용한다.

**`whenCompleteAsync`는 cancellation 시 callback 실행을 보장하지 않는다.**
이 API는 새 `CompletableFuture`를 반환하며, 그 새 stage가 취소되면 callback이 실행되지 않을 수 있다.
Resource release에는 completion callback 대신 supplier 내부 `try/finally`를 사용한다.

**Concurrency test에서 `Thread.sleep`은 flaky하다.**
`CountDownLatch`나 유사한 명시적 signalling을 사용한다. 여기의 `funcCompleted` latch pattern은
user `finally`에서 signal하고 framework release가 즉시 이어지는 구조라, 임의 delay 없이 deterministic
synchronization을 제공한다.

## 검증

```
:bluetape4k-io:test
  KryoSupportTest  12 passing (2s)
  Full module      925 passing (9.7s) — BUILD SUCCESSFUL
```
