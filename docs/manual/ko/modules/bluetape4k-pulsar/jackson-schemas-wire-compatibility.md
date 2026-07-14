---
title: Jackson 2·3 Schema와 wire 호환성
description: JSON Schema metadata, mapper별 payload, compileOnly 의존성과 호환성 검증 방법을 설명합니다.
manualId: bluetape4k-pulsar
chapterId: jackson-schemas-wire-compatibility
---

# Jackson 2·3 Schema와 wire 호환성

## 두 mapper 계열

`jacksonSchema<T>()`는 Jackson 2의 `com.fasterxml.jackson.databind.ObjectMapper`, `jackson3Schema<T>()`는 Jackson 3의 `tools.jackson.databind.ObjectMapper`를 사용합니다.

```kotlin
val schema2: Schema<Order> = jacksonSchema()
val schema3: Schema<Order> = jackson3Schema()
```

API 모양은 같지만 mapper type과 관련 module은 서로 다릅니다. 한 애플리케이션에서 어느 계열이 payload 계약을 소유하는지 명확히 정합니다.

## SchemaInfo와 payload

두 구현은 `Schema.JSON(type).schemaInfo`에서 schema bytes와 properties를 가져와 `SchemaType.JSON` metadata를 만듭니다. schema name은 `type.simpleName`, 이름이 비어 있으면 전체 class name입니다.

실제 encode와 decode는 전달된 mapper가 수행합니다. metadata가 비슷하더라도 naming strategy, Kotlin module, visibility, 날짜·숫자 설정이 다르면 payload가 달라질 수 있습니다. “둘 다 JSON”이라는 이유만으로 Jackson 2 producer와 Jackson 3 consumer의 호환성을 단정하지 않습니다.

## runtime 의존성

모듈 build에서 두 Jackson 연동은 `compileOnly`입니다. 사용하는 계열을 애플리케이션 dependency에 추가하지 않으면 schema API 호출 시 `NoClassDefFoundError`가 날 수 있습니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-pulsar")
    implementation("io.github.bluetape4k:bluetape4k-jackson2")
}
```

## mapper를 명시해야 할 때

기본 mapper는 각 bluetape4k Jackson 모듈의 default mapper입니다. 애플리케이션이 custom serializer, property naming 또는 시간 module을 사용한다면 같은 설정의 mapper를 schema 생성에 명시합니다.

```kotlin
val schema = jacksonSchema<Order>(applicationMapper)
```

producer와 consumer가 별도 서비스라면 설정을 코드 관례에만 맡기지 말고 golden JSON과 양방향 decode test로 고정합니다.

## clone과 진화

`clone()`은 mapper와 이미 계산한 `SchemaInfo`를 재사용하는 독립 schema 객체를 만듭니다. release test는 metadata, encode/decode round trip, clone과 실제 broker round trip을 확인합니다.

이 검증은 field 추가·삭제의 Pulsar schema compatibility 정책을 대신하지 않습니다. schema evolution은 broker namespace와 topic 정책, nullable/default field, 이전 payload fixture를 포함해 별도로 검사합니다.

## Source와 tests

- [`JacksonSchema.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/codec/JacksonSchema.kt)
- [`Jackson3Schema.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/codec/Jackson3Schema.kt)
- [`JacksonSchemaTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/codec/JacksonSchemaTest.kt)
- [`Jackson3SchemaTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/codec/Jackson3SchemaTest.kt)
