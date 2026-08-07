---
title: Row와 Cassandra 값을 Kotlin 타입으로 옮기기
description: Row map 변환, 타입 안전 getter/setter, UDT·Tuple·Codec과 mapper helper의 경계를 설명합니다.
manualId: bluetape4k-cassandra
chapterId: rows-data-mapping
---

# Row와 Cassandra 값을 Kotlin 타입으로 옮기기

## 먼저 도메인 타입으로 옮긴다

쿼리 결과의 컬럼이 정해져 있다면 `Row`를 `Map<String, Any?>`로 오래 들고 다니지 말고 읽는 지점에서 도메인 타입으로 바꿉니다. 컬럼 이름, null 허용 여부, 컬렉션 복사 정책이 한 함수에 모이므로 스키마가 어긋났을 때 실패 지점도 분명해집니다.

```kotlin
import com.datastax.oss.driver.api.core.cql.Row
import io.bluetape4k.cassandra.data.getList

data class User(
    val id: Long,
    val name: String,
    val tags: List<String>,
)

fun Row.toUser(): User = User(
    id = getLong("id"),
    name = getString("name") ?: error("users.name must not be null"),
    tags = getList<String>("tags")?.toList().orEmpty(),
)
```

`name`은 스키마에서 필수라는 결정을 코드에 남깁니다. 반면 `tags`는 null을 빈 목록으로 받아들이고, 드라이버가 반환한 변경 가능한 목록을 `toList()`로 복사합니다. null과 빈 컬렉션을 구분해야 하는 도메인이라면 `orEmpty()`를 쓰지 말고 nullable 속성으로 유지합니다.

## Row를 동적으로 살펴볼 때

`RowSupport`는 컬럼을 인덱스, 문자열 이름, `CqlIdentifier` 기준의 map으로 바꾸는 도구를 제공합니다.

| API | 키 | 적합한 용도 |
| --- | --- | --- |
| `toMap()` | 컬럼 인덱스 | 결과 순서 자체를 점검하는 진단 코드 |
| `toNamedMap()` | 렌더링한 CQL 이름 | 동적 내보내기, 로그용 요약, 스키마 탐색 |
| `toCqlIdentifierMap()` | `CqlIdentifier` | 인용과 대소문자 규칙을 보존해야 하는 동적 처리 |
| `map`, `mapWithName`, `mapWithCqlIdentifier` | 위와 같음 | 각 값을 같은 방식으로 변환하는 동적 경계 |
| `columnCodecs()` | `CqlIdentifier` | 드라이버가 고른 `TypeCodec`을 진단 |

이 함수들은 드라이버 codec을 골라 각 컬럼 값을 bytes에서 decode합니다. `toNamedMap()`은 렌더링한 CQL 이름을 기준으로 정렬하므로 select 목록 순서를 보존하는 자료구조가 아닙니다. `toMap()`, `toNamedMap()`, `toCqlIdentifierMap()`처럼 변환 함수를 받지 않는 API의 값 타입은 `Any?`라서, 필드가 정해진 업무 모델의 기본 표현으로 쓰면 잘못된 이름과 타입을 컴파일 시점에 잡지 못합니다. 반면 `map*` 함수는 변환 결과 타입 `T`를 값으로 반환합니다.

그래서 map 변환은 컬럼 구성이 실행 시점에 결정되는 경계나 진단 코드에만 두고, 안정된 스키마는 `toUser()` 같은 명시적 매퍼로 옮기는 편이 낫습니다. 민감한 컬럼이 있는 `Row`를 통째로 map으로 바꿔 로그에 남기지 않습니다.

## null과 빈 문자열을 합칠지 결정한다

`getStringOrEmpty(index|name|id)`는 드라이버의 nullable `getString` 결과에 `orEmpty()`를 적용합니다. 편리하지만 Cassandra의 null과 빈 문자열을 모두 `""`로 바꿉니다.

```kotlin
import io.bluetape4k.cassandra.cql.getStringOrEmpty

val displayName = row.getStringOrEmpty("display_name")

// null과 빈 문자열의 뜻이 다르면 nullable getter를 유지합니다.
val middleName: String? = row.getString("middle_name")
```

화면 표시처럼 두 상태를 같게 처리해도 될 때만 `getStringOrEmpty`를 사용합니다. 값이 없다는 상태가 검증, 수정, 감사 로직에 영향을 준다면 nullable getter를 그대로 사용하고 도메인 경계에서 처리합니다.

## 이름, 인덱스, CqlIdentifier getter

`GettableSupport`는 Java Driver의 `GettableByName`, `GettableByIndex`, `GettableById`에 Kotlin reified helper를 더합니다. 세 접근 방식 모두 `getValue`, `getList`, `getSet`, `getMap`을 제공하며 실제 decode는 세션의 codec registry가 담당합니다.

```kotlin
import com.datastax.oss.driver.api.core.CqlIdentifier
import io.bluetape4k.cassandra.data.getList
import io.bluetape4k.cassandra.data.getValue

val name: String? = row.getValue("name")
val tags: MutableList<String>? = row.getList("tags")

val nameId = CqlIdentifier.fromInternal("name")
val sameName: String? = row.getValue(nameId)
```

이 helper는 요청한 Kotlin 타입이 Cassandra 컬럼 타입과 맞는지 컴파일러가 증명해 주는 장치가 아닙니다. 등록된 codec을 찾지 못하거나 실제 값과 요청 타입이 맞지 않으면 읽는 시점에 드라이버 예외가 발생합니다. 일반 업무 코드는 컬럼 이름을 쓰고, 메타데이터를 다루거나 인용 식별자를 보존해야 할 때 `CqlIdentifier`를 선택합니다. 인덱스 접근은 select 목록 순서와 강하게 결합되므로 좁은 변환 함수 안에만 두는 편이 안전합니다.

## 값을 쓸 때도 타입 경계를 유지한다

`SettableSupport`는 `BoundStatement`, `UdtValue`, `TupleValue`가 구현하는 settable 인터페이스에 `setValue`, `setList`, `setSet`, `setMap`을 제공합니다. 이름 기반 `setValue`는 nullable 값을 받을 수 있고, 컬렉션 helper는 원소 타입을 reified 타입으로 드라이버에 전달합니다.

```kotlin
import io.bluetape4k.cassandra.data.setList
import io.bluetape4k.cassandra.data.setValue

val bound = prepared.boundStatementBuilder()
    .setValue("name", user.name)
    .setList("tags", user.tags)
    .build()
```

helper는 CQL 문자열에 값을 끼워 넣지 않습니다. 먼저 prepared statement의 bind marker를 정하고, builder가 기대하는 이름과 Cassandra 타입에 맞춰 값을 설정합니다. null을 설정할지 아예 건너뛸지는 mapper나 호출부에서 별도로 결정해야 합니다.

## UDT와 Tuple

UDT는 keyspace metadata에서 `UserDefinedType`을 찾은 뒤 `newValue`로 만들고, Tuple은 `DataTypes.tupleOf(...)`로 정의한 타입에서 값을 만듭니다. 읽을 때는 Java Driver의 `getUdtValue`와 `getTupleValue`를 사용합니다.

```kotlin
val coordinatesType = session.metadata
    .getKeyspace("examples")
    .flatMap { keyspace -> keyspace.getUserDefinedType("coordinates") }
    .orElseThrow(::IllegalArgumentException)

val coordinates = coordinatesType.newValue(12, 34)
val bound = prepared.bind().setUdtValue("coordinates", coordinates)

val stored = row.getUdtValue("coordinates")
val x = stored?.getInt("x")
```

Tuple도 같은 방식으로 `TupleValue`를 만들고 `setTupleValue`/`getTupleValue`로 옮깁니다. bluetape4k helper는 이 드라이버 모델을 다른 직렬화 형식으로 바꾸지 않습니다. UDT 필드 순서와 이름, Tuple 원소 순서, null 허용 여부는 Cassandra schema와 호출 코드가 함께 지켜야 합니다.

## Codec으로 애플리케이션 타입 연결하기

기본 codec에 없는 타입은 `CqlSessionBuilder.addTypeCodecs(...)`로 등록합니다. 1.12.1 예제는 `MappingCodec`, enum, `Optional`, 배열 codec을 등록한 뒤 `getValue<T>`와 `setValue`를 사용합니다.

```kotlin
sessionBuilder.addTypeCodecs(CqlIntToStringCodec())

val bound = prepared.bind().setValue("pk", "1")
val pk: String? = row.getValue("pk")
```

`getValue<String>`이라는 호출만으로 Cassandra `int`가 문자열이 되지는 않습니다. 해당 CQL 타입과 Kotlin 타입을 잇는 `TypeCodec`이 그 세션의 registry에 등록돼 있어야 합니다. 여러 codec이 같은 조합을 처리한다면 선택 규칙도 세션 설정의 일부로 관리합니다.

## EntityHelper를 선택하는 경우

DataStax Mapper를 사용해 `EntityHelper<T>`가 생성돼 있다면 bluetape4k의 mapper helper로 준비와 바인딩을 반복하지 않아도 됩니다.

```kotlin
import io.bluetape4k.cassandra.mapper.bind
import io.bluetape4k.cassandra.mapper.prepareInsert

val prepared = userEntityHelper.prepareInsert(session)
val statement = userEntityHelper.bind(prepared, user)
val result = session.execute(statement)
```

`prepareInsert`와 `prepareInsertIfNotExists`는 `EntityHelper`가 만든 CQL을 `CqlSession.prepare`에 넘깁니다. `bind`는 `PreparedStatement`의 builder에 entity 값을 채우고 `BoundStatement`를 반환합니다. 어느 함수도 statement를 실행하지 않으므로 마지막 실행은 호출부의 책임입니다.

`bind`의 기본값은 `NullSavingStrategy.DO_NOT_SET`과 `lenient = true`입니다. `DO_NOT_SET`은 null 속성의 setter를 호출하지 않아 bind marker를 unset 상태로 남깁니다. 그래서 UPDATE라면 기존 컬럼 값을 덮어쓰지 않습니다. `SET_TO_NULL`은 null 속성도 CQL `NULL`로 바인딩합니다.

`lenient = true`이면 target에 대응 컬럼이 없는 entity 속성을 건너뛰므로 일부 속성만 채운 statement가 만들어질 수 있습니다. `lenient = false`이면 computed property를 제외한 모든 entity 속성에 대응하는 target 컬럼이 있어야 하며, 하나라도 없으면 `IllegalArgumentException`이 발생합니다. 저장 의도와 prepared statement의 bind marker 구성을 확인한 뒤 두 값을 정합니다.

`bluetape4k-cassandra` 1.12.1은 DataStax mapper runtime을 API dependency로 제공합니다. 하지만 애플리케이션의 `EntityHelper<T>` 코드는 mapper annotation processor가 생성해야 합니다. runtime이 classpath에 있다는 사실만으로 helper가 생기지는 않습니다. processor 설정 없이 사용한다면 직접 작성한 typed row mapper가 더 단순한 경계입니다.

## 무엇을 선택할까

| 상황 | 선택 |
| --- | --- |
| 컬럼이 고정된 조회 | `Row`를 도메인 타입으로 옮기는 명시적 함수 |
| 실행 시점에 컬럼이 달라지는 도구 | `toNamedMap()` 또는 `toCqlIdentifierMap()` |
| 컬렉션을 이름·인덱스·식별자로 읽고 쓰기 | reified getter/setter helper |
| Cassandra UDT·Tuple을 그대로 다루기 | 드라이버의 `UdtValue`·`TupleValue` API |
| 별도 Kotlin 타입을 CQL 타입에 연결 | 세션에 등록한 `TypeCodec` |
| DataStax Mapper가 entity helper를 생성 | `EntityHelper` prepare/bind helper |

스키마가 고정돼 있는데도 `Any?` map을 기본값으로 쓰거나, null과 빈 값을 무조건 합치거나, codec 등록 없이 임의 타입을 요청하는 방식은 피합니다. 변환 경계가 짧고 명시적일수록 잘못된 컬럼과 타입을 실제 쿼리에 가까운 곳에서 발견할 수 있습니다.

## 소스와 대표 테스트

- [`RowSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/RowSupport.kt): 문자열 기본값, 동적 map 변환, 컬럼 codec 조회
- [`GettableSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/data/GettableSupport.kt): 이름·인덱스·`CqlIdentifier` getter
- [`SettableSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/data/SettableSupport.kt): 이름·인덱스·`CqlIdentifier` setter
- [`EntitySupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/mapper/EntitySupport.kt): `EntityHelper` prepare와 bind
- [`CustomCodecExamples.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/examples/datatypes/CustomCodecExamples.kt): 사용자 codec 등록과 읽기·쓰기
- [`UserDefinedTypesSimpleExamples.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/examples/datatypes/UserDefinedTypesSimpleExamples.kt): UDT metadata와 값 처리
- [`TuplesSimpleExamples.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/examples/datatypes/TuplesSimpleExamples.kt): Tuple 생성과 읽기
- [`EntitySupportTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/mapper/EntitySupportTest.kt): prepare와 bind 위임 경계
- [`build.gradle.kts`](../../../../../data/cassandra/build.gradle.kts): mapper runtime과 annotation processor dependency

## 앞뒤로 읽기

- 이전: [코루틴 쿼리와 여러 페이지 읽기](./coroutine-queries.md)
- 다음: [Statement와 QueryBuilder 선택](./statements-query-builder.md)
