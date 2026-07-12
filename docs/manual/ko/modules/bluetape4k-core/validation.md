---
title: 검증과 불변식
description: Caller argument, object state, domain rule의 실패를 올바른 경계와 exception으로 표현합니다.
manualId: bluetape4k-core
chapterId: validation
---

# 검증과 불변식

검증은 null check 모음이 아니라 잘못된 상태가 system 안으로 들어오는 것을 막는 경계입니다. 실패한 규칙의 소유자에 따라 exception 의미도 달라집니다.

![Caller argument, object state, domain rule 검증 경계](../../../assets/core/validation-boundary.svg)

## 세 가지 경계

| 실패한 규칙 | 기본 도구 | 표면 |
| --- | --- | --- |
| 호출자가 넘긴 argument | `require`, `requireNotBlank`, `requirePositiveNumber` 등 | `IllegalArgumentException` |
| 이미 생성된 객체의 state | `check` 또는 명시적 state guard | `IllegalStateException` |
| business/domain rule | domain validator/result/exception | domain-specific surface |

Generic precondition으로 domain failure를 대체하면 호출자가 retry, 사용자 메시지, 상태 전이를 구분할 수 없습니다.

## Receiver를 보존하는 helper

대부분의 `require*` extension은 검증한 receiver를 반환하므로 초기화와 변환을 연결할 수 있습니다.

```kotlin
class SearchRequest(rawQuery: String?, limit: Int) {
    val query: String = rawQuery.requireNotBlank("query").trim()
    val limit: Int = limit.requireInRange(1, 100, "limit")
}
```

지원 범주는 null/empty/blank, contains/prefix/suffix, equality/comparison, closed/open range, 숫자 부호, array/collection/map 조건입니다. 표준 `require` 한 줄이 더 명확하면 helper를 늘리지 않습니다.

## Side effect보다 먼저

```kotlin
fun createAccount(command: CreateAccount): AccountId {
    val email = command.email.requireNotBlank("email")
    command.initialCredit.requireZeroOrPositiveNumber("initialCredit")

    // Validation is complete before persistence or external calls.
    return repository.insert(email, command.initialCredit)
}
```

검증 뒤 side effect가 시작되면 실패 시 partial state가 남지 않습니다. Race가 가능한 invariant는 precondition만으로 보장되지 않으므로 transaction/unique constraint도 필요합니다.

## Message와 observability

- parameter name과 기대 조건을 포함합니다.
- password, token, 원문 payload를 message에 넣지 않습니다.
- argument failure와 server state invariant failure를 다른 metric으로 집계합니다.
- high-cardinality 원문은 metric label이 아니라 제한된 log/trace에 둡니다.

## 테스트 표

Happy path만이 아니라 경계 바로 아래/위, null, empty, blank, open/closed endpoint, 반환 receiver identity를 확인합니다. Exception type과 parameter name도 public contract라면 assertion에 포함합니다.

## Source와 representative tests

- [`RequireSupport.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/support/RequireSupport.kt)
- [`RequireSupportTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/support/RequireSupportTest.kt)

검증한 값을 고정된 memory budget에 보관하는 방법은 [Bounded collections](./bounded-collections.md)에서 이어집니다.
