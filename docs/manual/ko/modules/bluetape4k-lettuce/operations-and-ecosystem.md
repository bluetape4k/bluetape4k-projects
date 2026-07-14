---
title: 운영과 생태계 경로
description: Lettuce 운영 지표와 cache, Hibernate, Exposed, workshop으로 이어지는 선택 경로를 설명합니다.
manualId: bluetape4k-lettuce
chapterId: operations-and-ecosystem
---

# 운영과 생태계 경로

## 무엇을 관찰할까

Redis 연결·재연결, command latency와 timeout, pipeline batch, codec decode 오류를 기본 지표로 둡니다. loaded map은 hit/miss, loader latency, write-behind queue, retry, dead-letter와 shutdown drain을 추가합니다. queue 포화는 단순히 용량을 늘릴 신호가 아니라 writer가 Redis 쓰기 속도를 따라가지 못한다는 신호입니다.

## 테스트에서 운영 실패까지

`LettuceClientsTest`는 동일 client/codec connection 재사용과 multi-thread·virtual-thread·coroutine 접근을 검증합니다. `RedisFutureSupportTest`는 입력 순서와 실패 전파를, loaded-map 테스트는 writer 실패·dead-letter·caller scope 보존을 확인합니다. benchmark 수치는 특정 장비와 payload의 비교 자료이며 애플리케이션 SLA가 아닙니다.

## 다음 library 선택

- raw Redis command와 coroutine adapter가 목적이면 이 모듈에 머뭅니다.
- 함수 결과 memoization과 cache abstraction은 [`bluetape4k-cache-lettuce`](../bluetape4k-cache-lettuce.md)로 이어갑니다.
- Hibernate second-level cache는 [`bluetape4k-hibernate-cache-lettuce`](../bluetape4k-hibernate-cache-lettuce.md)를 사용합니다.
- DB read-through/write-through 경계를 직접 설계한다면 [`bluetape4k-jdbc`](../bluetape4k-jdbc.md), [`bluetape4k-hibernate`](../bluetape4k-hibernate.md), Exposed repository와 함께 transaction 경계를 먼저 정합니다.
- 실전 예제는 [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)과 [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)을 참고합니다.

cache-aside의 PUT 관리와 진짜 write-through/write-behind를 같은 용어로 부르지 않습니다. DB writer까지 연결한 `LettuceLoadedMap` 또는 repository 예제로 일관성 경계를 설명해야 합니다.

## Source와 tests

- [`Benchmark.ko.md`](../../../../../infra/lettuce/Benchmark.ko.md)
- [`LettuceSuspendedLoadedMapTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/map/LettuceSuspendedLoadedMapTest.kt)
- [`FastForyCompatibilityTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/FastForyCompatibilityTest.kt)
