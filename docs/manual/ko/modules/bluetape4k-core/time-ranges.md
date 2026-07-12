---
title: 시간과 범위
description: Inclusive boundary, overlap, timezone 변환을 명시적으로 다룹니다.
manualId: bluetape4k-core
chapterId: time-ranges
---

# 시간과 범위

## 해결할 문제

시간 조회에서 가장 비싼 오류는 타입 오류보다 경계 의미의 불일치입니다. 한 쪽이 종료 시각을 포함하고 다른 쪽이 제외하면 중복 집계나 누락이 생깁니다. timezone이 빠진 local time을 timeline의 점처럼 저장하면 DST 전환에서 더 큰 모호성이 생깁니다.

## 두 개의 독립된 결정

1. **시간 표현**: 저장·전송은 `Instant`, 사용자 일정 계산은 명시적인 `ZoneId`가 있는 `ZonedDateTime`/`LocalDate`를 우선합니다.
2. **범위 경계**: 시작과 끝을 포함하는지 type으로 표현하고, repository query와 test도 같은 계약을 사용합니다.

| 타입 | 표기 | 동일한 시작/끝 | 흔한 용도 |
| --- | --- | --- | --- |
| `ClosedClosedRange` | `[start, end]` | 한 점 | 양끝을 포함하는 값 구간 |
| `ClosedOpenRange` | `[start, end)` | empty | 시간 window, paging cursor |
| `OpenClosedRange` | `(start, end]` | empty | 직전 checkpoint 이후 집계 |
| `OpenOpenRange` | `(start, end)` | empty | 두 경계를 모두 제외하는 수학 구간 |

## 시간 window 예제

```kotlin
import io.bluetape4k.javatimes.toZonedDateTime
import io.bluetape4k.ranges.closedOpenRangeOf
import java.time.Instant
import java.time.ZoneId

val zone = ZoneId.of("Asia/Seoul")
val from = Instant.parse("2026-07-11T15:00:00Z")
val until = Instant.parse("2026-07-12T15:00:00Z")
val queryWindow = closedOpenRangeOf(from, until)

check(from in queryWindow)
check(until !in queryWindow)
check(from.toZonedDateTime(zone).toLocalDate().toString() == "2026-07-12")
```

연속된 `[dayStart, nextDayStart)` window는 경계가 겹치지 않으므로 일별 조회에 적합합니다. “하루 = 24시간”을 가정해 `plusSeconds(86400)`로 다음 날을 계산하지 말고 business zone의 다음 `LocalDate` 시작을 구한 뒤 `Instant`로 바꿉니다.

## Range 연산 계약

- `value in range`는 open/closed endpoint를 반영합니다.
- `range.contains(other)`는 다른 범위 전체가 포함되는지 검사합니다.
- `range.overlaps(other)`는 실제 공통 원소가 있는지 검사합니다. 맞닿은 값이 양쪽에서 포함될 때만 overlap입니다.
- reversed range는 empty입니다. 시작과 끝이 같은 범위는 `[x, x]`만 non-empty입니다.
- `1 until 3`은 Core의 `ClosedOpenRange`를 만듭니다.

```kotlin
val left = closedOpenRangeOf(0, 5)       // [0, 5)
val right = closedOpenRangeOf(5, 10)     // [5, 10)
check(!left.overlaps(right))

val includingFive = closedClosedRangeOf(0, 5)
check(includingFive.overlaps(right))      // 양쪽이 5를 포함
```

## 변환 시 주의할 점

`(0..10).toClosedOpenRange()`는 `ClosedRange.endInclusive` 값 `10`을 새 범위의 `endExclusive`로 그대로 사용합니다. 결과는 `[0, 10)`이며 원래 포함되던 `10`이 제외됩니다. 정수 범위의 모든 원소를 유지하려고 변환하는 API가 아닙니다.

경계 의미를 바꾸려는 의도가 아니라면 source range를 직접 변환하지 말고 원하는 exclusive endpoint를 계산해 factory로 생성합니다.

## Time helper 선택 기준

| 상황 | 표현/도구 |
| --- | --- |
| DB 저장, 이벤트 timestamp, 서비스 간 전달 | UTC `Instant` |
| 사용자의 “매일 오전 9시” 규칙 | `LocalTime` + 명시적 `ZoneId` |
| 달력 날짜 기반 정책 | `LocalDate`; 너무 일찍 `Instant`로 변환하지 않음 |
| offset이 protocol 일부 | `OffsetDateTime` |
| 실제 지역 timezone 규칙이 필요 | `ZonedDateTime` |

Core의 `instantOf`, `toLocalDateTime`, `toOffsetDateTime`, `toZonedDateTime`은 JDK type 사이 변환을 간결하게 합니다. zone parameter가 없는 helper는 UTC 기본값을 사용하므로, 사용자 local time으로 해석해야 할 때는 반드시 zone을 넘깁니다.

## 실패와 운영

- system default timezone을 business rule에 사용하지 않습니다.
- DST gap/overlap에서 어떤 offset을 선택할지 fixture test로 고정합니다.
- 로그와 metric의 timestamp는 UTC/zone/offset을 식별할 수 있어야 합니다.
- repository query의 SQL 비교 연산자와 `Range` endpoint 계약을 같은 test에서 확인합니다.
- empty/reversed range는 “결과 없음”인지 caller 오류인지 API 경계에서 결정합니다.

## Source와 representative test

- [`Range.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/ranges/Range.kt)
- [`ClosedOpenRange.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/ranges/ClosedOpenRange.kt)
- [`InstantSupport.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/javatimes/InstantSupport.kt)
- [`RangeBoundaryTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/ranges/RangeBoundaryTest.kt)
- [`RangeSupportTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/ranges/RangeSupportTest.kt)
- [`InstantSupportTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/javatimes/InstantSupportTest.kt)

입력 경계는 [검증과 불변식](./validation.md), 실제 조립은 [Core 실전 레시피](./recipes.md)로 이어집니다.
