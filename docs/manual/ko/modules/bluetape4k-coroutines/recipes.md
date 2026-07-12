---
title: 실전 레시피와 Workshop
description: 요청 조합, replica race, Flow 변환, callback bridge를 조립합니다.
manualId: bluetape4k-coroutines
chapterId: recipes
---

# 실전 레시피와 Workshop

## 해결할 문제

API 하나가 아니라 ownership, failure policy, ordering, capacity를 함께 결정하는 실행 가능한 조합이 필요합니다.

## Mental model

각 recipe는 입력 경계, child policy, 결과 ordering, resource cleanup을 한 단위로 닫습니다.

## 최소 API surface

`coroutineScope`, `async`, `awaitAny`, `firstSuccessTaskScope`, `mapParallel`, Subject를 필요한 만큼만 사용합니다.

## 완전한 예제

HTTP 요청의 두 suspend call 조합, fastest/first-success replica, ordered/throughput-first transform, callback-to-Subject bridge를 독립 recipe로 구성합니다.

## 선택 기준

결과의 완전성, ordering, 허용 가능한 병렬도, caller 취소 시 cleanup을 recipe 선택 전에 기록합니다.

## 실패·취소·수명주기 계약

모든 recipe는 parent cancellation을 child에 전파하고 owned resource를 `finally` 또는 `close()`에서 정리합니다.

## 운영과 문제 진단

Recipe별 latency와 in-flight 상한을 먼저 정하고, timeout이나 retry가 곱해져 폭증하지 않는지 확인합니다.

## Source와 representative test

각 recipe는 library representative test와 `/Users/debop/work/bluetape4k/bluetape4k-workshop/kotlin`의 대응 module을 함께 근거로 합니다.

## 이어 읽기와 runnable workshop

Flow extensions, Ktor REST coroutines, Spring WebFlux coroutines, observability workshop을 목적에 맞게 실행합니다.
