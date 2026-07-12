---
title: 수명주기와 취소
description: Scope 소유권, 취소 전파, dispatcher 종료를 명시적으로 설계합니다.
manualId: bluetape4k-coroutines
chapterId: lifecycle
---

# 수명주기와 취소

## 해결할 문제

Coroutine을 시작하는 것보다 누가 종료할지를 먼저 결정해야 합니다. 요청이 끝날 때 함께 취소할 작업과 component가 닫힐 때까지 유지할 작업을 같은 scope에 섞지 않습니다.

## Mental model

Caller-owned scope는 호출자의 수명주기를 따릅니다. `CloseableCoroutineScope`, `DefaultCoroutineScope`, `ThreadPoolCoroutineScope`는 component가 소유하며 `close()`가 종료 경계입니다.

## 최소 API surface

일반 요청은 전달받은 `CoroutineScope`와 `coroutineScope`를 사용합니다. 독립 dispatcher가 필요한 component만 closeable scope를 생성합니다.

## 완전한 예제

Component는 scope를 property로 소유하고 `close()`에서 한 번만 정리합니다. 호출자는 `try/finally` 또는 애플리케이션 lifecycle hook으로 close를 보장해야 합니다.

## 선택 기준

호출 결과에 종속된 작업은 caller-owned, 여러 호출 사이에 유지되는 worker는 component-owned를 선택합니다. 단순 병렬 조합에는 새 scope를 만들지 않습니다.

## 실패·취소·수명주기 계약

`CancellationException`은 정상적인 종료 신호이므로 잡아서 일반 실패로 바꾸지 않습니다. Timeout은 기다림만 끝낼 수도 있으므로 underlying I/O의 취소 지원을 별도로 확인합니다.

## 운영과 문제 진단

Active job, dispatcher thread, queue 길이를 함께 관찰합니다. 종료 후 thread가 남으면 scope 소유자와 close 호출 경로부터 확인합니다.

## Source와 representative test

구현 근거는 [`CloseableCoroutineScope`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/CloseableCoroutineScope.kt) 계열과 [coroutines test](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines)입니다.

## 이어 읽기와 runnable workshop

다음은 [Deferred 조정](./deferred.md)입니다. HTTP 요청 수명주기는 `spring-boot/webflux-coroutines` workshop과 함께 확인합니다.
