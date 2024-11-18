# Module bluetape4k-idgenerators

Unique 한 ID 값을 다양한 방식으로 제공하는 라이브러리입니다.

## Snowflake

[Snowflake*](https://developer.twitter.com/en/docs/basics/twitter-ids)는 Twitter에서 개밣한 Long 수형의 Id Generator입니다.
1024개의 machine 당 1 millisec 당 4096개의 Id를 생성할 수 있습니다.

```kotlin
val snowflake = DefaultSnowflake(1)     // DefaultSnowflake 생성
val id1: Long = snowflake.nextId()
val id2: Long = snowflake.nextId()
```

`GlobalSnowflake` 는 기본 Snowflake에 비해, machineId 구분 없이 Id 를 생성합니다. 이 방식은 1 milliseconds 당 4096 * 1024 개의 Id를 생성할 수 있으므로,
Twitter의
snowflake 알고리즘보다 더 많은 ID를 생성할 수 있습니다.

```kotlin
val snowflake = UnifiedSnowflake()      // GlobalSnowflake 생성
val id1: Long = snowflake.nextId()
val id2: Long = snowflake.nextId()
```

### 참고

* [Twitter IDs (snowflake)](https://developer.twitter.com/en/docs/basics/twitter-ids)
* [Generating unique IDs in a distributed environment at high scale.](https://www.callicoder.com/distributed-unique-id-sequence-number-generator/)

## Timebased UUID

시간 기준으로 순 증가하는 UUID를 생성합니다.
이 UUID 생성은 향후 정렬을 수행할 때 유용합니다.
단 UUID 수형은 Long 수형의 두 배의 저장공간이 필요하므로, 이를 고려해야합니다.

```
val uuidGenerator = TimebasedUuidGenerator()

val u1 = uuidGenerator.nextUUID()
val u2 = uuidGenerator.nextUUID()
val u3 = uuidGenerator.nextUUID()

assertTrue { u2 > u1 }
assertTrue { u3 > u2 }
```

### 참고

[Java UUID Generator](https://github.com/cowtowncoder/java-uuid-generator)를 사용하여 다양한 방식으로 UUID를 생성할 수 있습니다.

## HashIds

[Hashids](https://hashids.org)는 `LongArray` 로부터 Unique한 문자열을 생성합니다.YouTube의 Short Url이 이러한 hashids 알고리즘을 사용한 예입니다.
기존 Hashids 알고리즘은 1..9007199254740992L 범위에서만 지원합니다.

Bluetape4k 에서 제공하는 Hashids는 위의 숫자 범위보다 큰 수나 음수에 대해서도 변환이 가능합니다.
즉 UUID나 Snowflake Id 등도 변환이 가능하고, 다양한 Array에 대해서도 지원 가능합니다. (예: [1,2,-3])

```kotlin
val hashids = Hashids(salt = "great korea")

hashids.encode(9876543210123L) shouldBeEqualTo "5rdJmxRkk"

hashids.encode(1L, -1L)     // Support negative number
```

```kotlin
val hashids = Hashids("this is my salt", alphabet = "01223456789abcdef")
hashids(1234567L) // returns "b332db5"
```

```kotlin
val hashids = Hashids()

val uuid = UUID.randomUUID()

val encoded = hashids.encodeUUID(uuid)
val decoded = hashids.decodeUUID(encoded)
decoded shouldBeEqualTo uuid
```

## Base62 Encoding/Decoding

UUID 값을 Base62로 Encoding/Decoding 하는 기능입니다.
UUID 값을 저장소에 저장하거나, 외부로 전달 시 데이터 사이즈를 줄일 수 있고, URL path 에 안전할 수 있습니다.

```kotlin
// base62 encode for uuid
val uuid = generator.nextUUID()
val encoded = uuid.toBase62String()
// uuid    = 0df6bfc8-5edd-11eb-a770-22071017d57b,
// encoded = QLfDyyhZrm9uVtDzQcs4R

// base62 decode for uuid
val uuid2 = encoded.toBase62Uuid()
```
