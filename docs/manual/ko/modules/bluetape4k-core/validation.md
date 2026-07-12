---
title: 검증과 불변식
description: Public 입력과 internal invariant에 맞는 검증 함수를 선택합니다.
manualId: bluetape4k-core
chapterId: validation
---

# 검증과 불변식

## 해결할 문제

잘못된 값을 경계에서 거부하면서 exception 의미와 원래 값을 보존해야 합니다.

## Mental model

Public argument의 잘못은 `IllegalArgumentException`, 이미 생성된 객체의 잘못된 상태는 `IllegalStateException`으로 구분합니다.

## 최소 API surface

표준 Kotlin `require`/`check`를 기본으로 사용하고, 반복되는 null·blank·collection 조건에는 bluetape `require*` helper를 선택합니다.

## 완전한 예제

입력 문자열을 `requireNotBlank`로 검증하고 반환된 receiver를 다음 변환에 연결해 별도 non-null assertion을 만들지 않습니다.

## 선택 기준

검증 대상이 호출자 입력인지 내부 상태인지 먼저 구분합니다. Domain error가 필요하면 generic precondition exception으로 대체하지 않습니다.

## 실패·취소·수명주기 계약

검증은 side effect 전에 실행하며 실패 시 부분 상태를 남기지 않습니다. Error message에는 secret 원문을 포함하지 않습니다.

## 운영과 문제 진단

Validation failure는 caller 오류와 server invariant 위반을 다른 metric으로 집계합니다.

## Source와 representative test

[`RequireSupport.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/support/RequireSupport.kt)와 대응 test가 반환 receiver와 exception 계약의 근거입니다.

## 이어 읽기와 runnable workshop

Validated value의 bounded 보관은 [Bounded collections](./bounded-collections.md)에서 이어집니다.
