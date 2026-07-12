---
title: 시간과 범위
description: Inclusive boundary, overlap, timezone 변환을 명시적으로 다룹니다.
manualId: bluetape4k-core
chapterId: time-ranges
---

# 시간과 범위

## 해결할 문제

시간 구간의 시작·끝 포함 여부와 timezone을 암묵적으로 두면 경계 값에서 중복이나 누락이 생깁니다.

## Mental model

Instant는 timeline의 점이고 local date/time은 timezone 없이는 동일한 순간을 뜻하지 않습니다. Range는 boundary contract가 type만큼 중요합니다.

## 최소 API surface

현재 Core가 제공하는 range와 time extension을 사용하되 표준 `java.time` type을 기본 표현으로 유지합니다.

## 완전한 예제

UTC `Instant`로 저장하고 business timezone에서 조회 구간을 계산한 뒤 inclusive/exclusive end를 test로 고정합니다.

## 선택 기준

Storage와 transport는 Instant, 사용자 일정은 zone이 포함된 변환 경계를 사용합니다. Date-only business rule을 Instant로 조기에 바꾸지 않습니다.

## 실패·취소·수명주기 계약

Empty range와 reversed boundary를 생성 시점에 처리합니다. DST gap/overlap을 시스템 default timezone에 맡기지 않습니다.

## 운영과 문제 진단

Log와 metric에는 timezone 또는 UTC 여부를 포함해 서로 다른 clock 표현을 비교할 수 있게 합니다.

## Source와 representative test

[`javatimes`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/javatimes)와 [`ranges`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/ranges) source/test를 기준으로 실제 public API를 확인합니다.

## 이어 읽기와 runnable workshop

Range validation은 [검증과 불변식](./validation.md), 실제 조립은 [Recipes](./recipes.md)로 이어집니다.
