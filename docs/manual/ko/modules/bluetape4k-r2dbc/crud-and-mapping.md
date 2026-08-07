---
title: CRUD와 row mapping
description: R2dbcClient의 raw table·entity CRUD와 identifier 검증, typed row access, PostgreSQL JSON 변환을 설명합니다.
manualId: bluetape4k-r2dbc
chapterId: crud-and-mapping
---

# CRUD와 row mapping

## raw table과 entity 경로

`insert`, `update`, `delete`는 table 이름을 직접 받는 얇은 SQL DSL과 Spring Data entity operation을 함께 제공합니다. SQL 구조를 직접 소유하면 raw table 경로를, mapping metadata와 entity type을 중심으로 작업하면 reified entity 경로를 사용합니다.

```kotlin
val id = client
    .insert()
    .into("users", "user_id")
    .value("username", "nick")
    .value("password", "pass")
    .value("name", "Nick")
    .nullValue("description", String::class.java)
    .awaitOne()
```

generated key가 `Int` 범위를 넘는 column이면 `awaitOneLong()`을 사용합니다. README의 `sqlInsert`와 `awaitGeneratedKey`는 1.12.1 source에 없으므로 사용하지 않습니다.

## update와 delete 범위를 명시한다

```kotlin
val changed = client
    .update()
    .table("users")
    .set("active", false)
    .matching("username = :username", mapOf("username" to "nick"))
    .fetch()
    .awaitRowsUpdated()
```

`matching()`을 생략하면 모든 행이 대상이 될 수 있습니다. 의도적인 전체 update/delete가 아니라면 service에서 조건 유무를 검증합니다.

## identifier 검증의 범위

raw CRUD는 table과 column identifier에 영문자 시작, 영숫자·underscore·dot만 허용합니다. identifier 위치에 SQL 값을 넣는 injection을 일찍 막습니다. 그러나 `matching("...")`의 where 문자열, order 표현식과 일반 raw SQL은 parser로 검증하지 않습니다. 값은 parameter로 바인딩하고 SQL 조각은 신뢰할 수 있는 코드에서만 만듭니다.

## row를 타입으로 읽는다

`R2dbcClient.execute<T>`는 `MappingR2dbcConverter`로 row를 읽습니다. 간단한 projection은 property 이름과 column alias를 맞추고, 변환 규칙이 복잡하면 Spring custom converter를 등록합니다.

`ReadableSupport`는 `int`, `long`, `string`, `instant`, `localDateTime`, `uuid`와 nullable variant를 index와 name 양쪽에 제공합니다. non-null accessor를 null column에 사용하면 변환 실패를 숨기지 않습니다.

## PostgreSQL JSON

`JsonToMapConverter`와 `MapToJsonConverter`는 PostgreSQL driver의 `Json`과 `Map<String, Any?>` 사이를 Jackson3로 변환합니다. 두 dependency 모두 module build에서 optional이므로 해당 기능을 사용할 때 runtime에 PostgreSQL R2DBC driver와 Jackson3를 추가해야 합니다.

malformed JSON이나 serialization 실패는 `ConversionFailedException`으로 전파되고 원래 Jackson cause를 보존합니다. 잘못된 값을 빈 map으로 바꾸지 않으므로 application boundary에서 오류 정책을 정합니다.

## Source와 tests

- [`Insert.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Insert.kt)
- [`Update.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Update.kt)
- [`Delete.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Delete.kt)
- [`ReadableSupport.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/ReadableSupport.kt)
- [`PostgresJsonConvertersTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/convert/postgresql/PostgresJsonConvertersTest.kt)
- [`InsertTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/InsertTest.kt)
- [`UpdateTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/UpdateTest.kt)

## 다음 읽을 장

조건 조합이 늘어나면 [동적 query](./dynamic-queries.md)에서 `QueryBuilder`의 쓰임과 제한을 확인합니다.
