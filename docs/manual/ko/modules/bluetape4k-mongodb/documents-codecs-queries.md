---
title: Document, codec과 query 경계
description: Document builder와 getAs의 실제 계약, typed collection codec과 공식 MongoDB Kotlin query extensions의 역할을 설명합니다.
manualId: bluetape4k-mongodb
chapterId: documents-codecs-queries
---

# Document, codec과 query 경계

## `documentOf`가 줄이는 코드

pair overload는 빈 `Document`에 key와 value를 순서대로 넣습니다. builder overload는 `Document.() -> Unit`을 적용합니다.

```kotlin
val first = documentOf(
    "sku" to "A-100",
    "stock" to 3,
    "tags" to listOf("sale", "summer"),
)

val second = documentOf {
    put("sku", "B-200")
    put("stock", 0)
}
```

duplicate key가 있으면 뒤의 `put`이 앞의 값을 덮어씁니다. builder는 schema, 필수 field나 key naming을 검사하지 않습니다. 외부 입력을 그대로 넣기 전에 application validation을 수행합니다.

## `getAs<T>`는 safe cast

`Document.getAs<T>(key)`의 구현은 `get(key) as? T`입니다.

```kotlin
val stock: Int? = document.getAs<Int>("stock")
```

key가 없거나 값이 `Long`인데 `Int`를 요청하는 것처럼 runtime type이 다르면 모두 `null`입니다. 숫자 변환, nested path, BSON type coercion이나 오류 설명을 제공하지 않습니다. missing과 wrong type을 구분해야 한다면 `containsKey`와 BSON-aware accessor를 함께 사용합니다.

## `Document`와 애플리케이션 타입

`Document`는 schema가 느슨하고 일부 projection이나 aggregation 결과를 빠르게 다루기 좋습니다. 반면 애플리케이션 타입은 compile-time field와 업무 validation을 표현하기 쉽습니다.

`getCollectionOf<Order>("orders")`를 호출해도 codec은 자동 등록되지 않습니다. client 또는 database codec registry에 `Order`를 처리할 codec을 넣어야 합니다. build의 BSON Kotlin dependency와 선택적 kotlinx.serialization BSON codec은 codec 구성 재료이지 애플리케이션 schema를 대신하지 않습니다.

## 선택적인 kotlinx.serialization codec

`mongo.bson.kotlinx`는 1.12.1 build에서 `compileOnly`입니다. 해당 API를 직접 사용한다면 consumer runtime에 dependency가 실제로 있는지 확인합니다. “모듈이 compile된다”는 사실만으로 배포 classpath의 codec 존재를 보장하지 않습니다.

codec 변경은 저장 형식과 호환성에 영향을 줍니다. 이미 저장된 document를 구·신 codec이 모두 읽을 수 있는지, field rename과 default가 어떻게 적용되는지 교차 테스트합니다.

## query DSL의 소유자

`Filters`, `Sorts`, `Updates`, `Projections`와 KProperty 기반 Kotlin extensions는 MongoDB driver가 제공합니다. `bluetape4k-mongodb`는 별도 문자열 query 언어나 mapping layer를 만들지 않습니다.

```kotlin
val filter = Filters.eq(Product::sku, "A-100")
val sort = Sorts.ascending(Product::sku)
```

사용 가능한 overload와 BSON 변환 규칙은 공식 driver 계약을 따릅니다. 매뉴얼 예제의 문자열 field와 KProperty field를 혼용할 때 실제 저장 field 이름이 같은지 codec mapping을 확인합니다.

## 민감한 데이터와 log

`Document.toString()` 전체를 log에 남기면 credential, 개인정보와 큰 payload가 그대로 노출될 수 있습니다. collection, operation, 제한된 business identifier와 exception type만 기록하고 filter·update document는 redaction 정책을 거칩니다.

## Source와 tests

- [`DocumentExtensions.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/bson/DocumentExtensions.kt)
- [`DocumentExtensionsTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/bson/DocumentExtensionsTest.kt)
- [`MongoDatabaseExtensions.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensions.kt)
- [`build.gradle.kts`](../../../../../data/mongodb/build.gradle.kts)
