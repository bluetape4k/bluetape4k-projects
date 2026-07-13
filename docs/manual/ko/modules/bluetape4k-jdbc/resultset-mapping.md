---
title: ResultSet 읽기와 mapping
description: SQL NULL, 단일 행과 collection mapping, cursor 이동과 지연 sequence의 수명주기를 설명합니다.
manualId: bluetape4k-jdbc
chapterId: resultset-mapping
---

# ResultSet 읽기와 mapping

## SQL NULL을 Kotlin null로 옮긴다

JDBC의 `getInt`, `getLong`, `getBoolean` 같은 primitive getter는 SQL NULL에서도 0이나 `false`를 반환할 수 있습니다. NULL 여부는 바로 뒤의 `wasNull()`로 확인해야 합니다. `getIntOrNull`, `getLongOrNull` 같은 확장은 이 두 호출을 묶어 Kotlin nullable 값으로 반환합니다.

```kotlin
data class AccountRow(
    val id: Long,
    val retryCount: Int?,
    val nickname: String?,
)

val accounts = dataSource.executeQuery(
    "SELECT id, retry_count, nickname FROM accounts",
) { rs ->
    rs.toList { row ->
        AccountRow(
            id = row.getLong("id"),
            retryCount = row.getIntOrNull("retry_count"),
            nickname = row.getString("nickname"),
        )
    }
}
```

column index는 JDBC 규칙대로 1부터 시작합니다. label 접근은 SQL alias를 존중하므로 projection 이름을 안정적으로 유지할 때 유용합니다.

## 결과 개수 계약을 코드에 드러낸다

| 예상 결과 | API | 결과가 없을 때 | 두 행 이상일 때 |
| --- | --- | --- | --- |
| 0~1행 | `mapFirst` | `null` | 첫 행만 반환 |
| 정확히 1행 | `mapSingle` | `NoSuchElementException` | `IllegalStateException` |
| 1행 이상 중 첫 행 | `mapFirstOrThrow` | `NoSuchElementException` | 첫 행만 반환 |
| 여러 행 | `toList`, `toSet`, `toMap`, `groupBy` | 빈 collection | 모두 소비 |

`mapFirst`는 query가 실제로 unique하다는 사실을 검증하지 않습니다. unique 결과가 업무 계약이면 SQL 제약과 함께 `mapSingle`로 예상 cardinality를 확인합니다.

## Cursor를 소비하는 함수

대부분의 mapping 함수는 `ResultSet.next()`를 호출하면서 현재 위치부터 끝까지 전진합니다. 같은 `ResultSet`에 `count`, `any`, `toList`를 차례로 호출하면 뒤 함수는 앞 함수가 남긴 위치부터 시작합니다. 한 cursor는 한 번의 변환에만 사용하고, 다른 형태가 필요하면 변환된 collection에서 처리합니다.

`isEmptyByMovingCursor`와 `isNotEmptyByMovingCursor`는 이름 그대로 cursor를 한 행 이동합니다. non-empty 결과라면 cursor가 첫 행에 놓이므로 바로 그 행을 읽어야 합니다. 이어서 `toList`를 호출하면 두 번째 행부터 담깁니다. 예전 `isEmpty`와 `isNotEmpty` 이름은 이 부작용이 드러나지 않아 deprecated 되었습니다.

```kotlin
dataSource.executeQuery("SELECT id FROM accounts ORDER BY id") { rs ->
    if (rs.isNotEmptyByMovingCursor()) {
        val firstId = rs.getLong("id") // 현재 첫 행을 먼저 소비
        val remainingIds = rs.toList { it.getLong("id") }
        listOf(firstId) + remainingIds
    } else {
        emptyList()
    }
}
```

forward-only driver에서는 `moveToPrevious()`가 실패할 수 있으며 이 함수는 그때 `false`를 반환합니다. cursor를 되돌리는 것을 보편적인 복구 방법으로 삼지 않습니다.

## Sequence의 수명주기

`sequence`와 `mapAsSequence`는 `ResultSet` cursor를 지연 소비합니다. sequence 자체가 데이터를 복사해 두는 것은 아닙니다. 아래처럼 mapper block 안에서 terminal operation까지 끝내야 합니다.

```kotlin
val ids: List<Long> = dataSource.executeQuery(
    "SELECT id FROM accounts ORDER BY id",
) { rs ->
    rs.sequence { row -> row.getLong("id") }
        .take(100)
        .toList()
}
```

mapper 밖으로 sequence를 반환하면 `executeQuery`가 statement와 `ResultSet`을 먼저 닫습니다. 나중에 sequence를 순회할 때 이미 닫힌 cursor를 읽게 됩니다.

## Token 기반 mapping

여러 타입을 반복해서 읽는 projection은 `extract`와 `ResultSetGetColumnTokens`를 사용할 수 있습니다.

```kotlin
val rows = dataSource.executeQuery(
    "SELECT id, display_name FROM accounts",
) { rs ->
    rs.extract {
        AccountRow(
            id = long["id"]!!,
            retryCount = null,
            nickname = string["display_name"],
        )
    }
}
```

`!!`는 SQL과 schema가 NULL을 금지한다는 계약이 있을 때만 사용합니다. nullable column이라면 도메인 타입도 nullable로 유지하거나 mapping 경계에서 명시적인 기본값·오류 정책을 둡니다.

## Source와 tests

- [`ResultSetExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetExtensions.kt)
- [`ResultSetMappingExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetMappingExtensions.kt)
- [`ResultSetGetColumnTokens.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetGetColumnTokens.kt)
- [`ResultSetExtensionsTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/ResultSetExtensionsTest.kt)
- [`ResultSetMappingExtensionsTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/ResultSetMappingExtensionsTest.kt)

## 다음 읽을 장

조회 결과를 값으로 옮기는 규칙을 정했다면 [트랜잭션과 상태 복원](./transactions.md)에서 여러 statement의 성공·실패 경계를 묶습니다.
