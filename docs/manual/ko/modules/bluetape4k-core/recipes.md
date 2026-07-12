---
title: Core 실전 레시피
description: 검증, bounded history, aggregation, deterministic shutdown을 조립합니다.
manualId: bluetape4k-core
chapterId: recipes
---

# Core 실전 레시피

## 해결할 문제

Utility를 나열하는 대신 경계 검증부터 bounded state와 cleanup까지 하나의 작업 흐름으로 연결해야 합니다.

## Mental model

Recipe는 입력 불변식, 데이터 표현, capacity, failure surface, 종료 순서를 함께 고정합니다.

## 최소 API surface

`require*`, `RingBuffer` 또는 `BoundedStack`, `ConcurrentReducer`, `ShutdownQueue` 중 문제에 필요한 것만 조합합니다.

## 완전한 예제

Validated event를 bounded recent history에 넣고 reducer로 집계한 뒤 shutdown queue가 역순으로 resource를 정리하는 흐름을 구성합니다.

## 선택 기준

표준 Kotlin/JDK API로 충분하면 그것을 우선합니다. Bluetape helper가 반복 계약을 더 명확하게 만들 때만 추가합니다.

## 실패·취소·수명주기 계약

Validation 이후에만 side effect를 시작하고, rejected aggregation과 shutdown failure를 호출자가 관찰할 수 있게 합니다.

## 운영과 문제 진단

Capacity, eviction, rejection, cleanup latency를 recipe의 필수 signal로 둡니다.

## Source와 representative test

각 recipe는 Core source와 `bluetape4k/core/src/test/kotlin`의 representative test를 근거로 합니다.

## 이어 읽기와 runnable workshop

세부 계약은 각 선행 chapter로 돌아가고, 실제 consumer module에서 작은 integration test로 조립을 검증합니다.
