# Module bluetape4k-idgenerators

## 개요

분산 환경에서 Unique한 ID 값을 다양한 방식으로 생성하는 라이브러리입니다. Twitter Snowflake, UUID, KSUID, Flake, Hashids 등 다양한 ID 생성 알고리즘을 제공합니다.

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-idgenerators:${version}")
}
```

## 주요 기능

| 알고리즘                | 타입        | 길이     | 정렬 가능 | 특징                 |
|---------------------|-----------|--------|-------|--------------------|
| **Snowflake**       | Long      | 19자리   | O     | Twitter 스타일, 분산 환경 |
| **GlobalSnowflake** | Long      | 19자리   | O     | 중앙집중식, 높은 처리량      |
| **Timebased UUID**  | UUID      | 36자리   | O     | 시간 기반 정렬           |
| **Random UUID**     | UUID      | 36자리   | X     | 완전 랜덤              |
| **KSUID**           | String    | 27자리   | O     | K-Sortable, Base62 |
| **Flake**           | ByteArray | 128bit | O     | Boundary 스타일       |
| **Hashids**         | String    | 가변     | X     | Long/UUID → 문자열    |

## 사용 예시

### Snowflake (Twitter 스타일)

분산 환경에서 기계별로 고유한 ID 생성

```kotlin
import io.bluetape4k.idgenerators.snowflake.*

// 기계 ID 1로 생성
val snowflake = DefaultSnowflake(machineId = 1)

// 단일 ID 생성
val id: Long = snowflake.nextId()  // 예: 1234567890123456789

// 문자열로 생성 (36진수)
val idString: String = snowflake.nextIdAsString()

// 여러 ID 생성
val ids: Sequence<Long> = snowflake.nextIds(10)

// ID 파싱 (생성 시간, 기계 ID, 시퀀스 추출)
val parsed = snowflake.parse(id)
println("Timestamp: ${parsed.timestamp}")  // 생성 시간
println("MachineId: ${parsed.machineId}")  // 기계 ID
println("Sequence: ${parsed.sequence}")    // 시퀀스 번호
```

### GlobalSnowflake (중앙집중식)

기계 ID 구분 없이 높은 처리량으로 ID 생성

```kotlin
import io.bluetape4k.idgenerators.snowflake.*

val snowflake = GlobalSnowflake()

// 1 millisecond 당 최대 4096 * 1024 개의 ID 생성 가능
val id: Long = snowflake.nextId()
val ids: Sequence<Long> = snowflake.nextIds(100)
```

### Timebased UUID (UUID v7)

시간 기반으로 정렬 가능한 UUID 생성

```kotlin
import io.bluetape4k.idgenerators.uuid.TimebasedUuidGenerator

val generator = TimebasedUuidGenerator()

// UUID 생성
val uuid1 = generator.nextUUID()
val uuid2 = generator.nextUUID()
val uuid3 = generator.nextUUID()

// 시간 순서대로 정렬됨
assert(uuid2 > uuid1)
assert(uuid3 > uuid2)

// Base62 문자열로 생성 (22자리)
val base62 = generator.nextBase62String()  // 예: "QLfDyyhZrm9uVtDzQcs4R"

// 여러 UUID 생성
val uuids = generator.nextUUIDs(10)
val base62Strings = generator.nextBase62Strings(10)
```

### Random UUID (UUID v4)

완전 랜덤한 UUID 생성

```kotlin
import io.bluetape4k.idgenerators.uuid.RandomUuidGenerator
import java.util.Random

val generator = RandomUuidGenerator()
// 또는 시드 지정
val generatorWithSeed = RandomUuidGenerator(Random(12345L))

val uuid = generator.nextId()
val uuidString = generator.nextIdAsString()  // Base62 인코딩
```

### KSUID (K-Sortable Unique ID)

시간 기반 정렬 가능, URL Safe, Base62 인코딩

```kotlin
import io.bluetape4k.idgenerators.ksuid.Ksuid

// KSUID 생성 (27자리)
val ksuid: String = Ksuid.generate()  // 예: "0ujtsYcgvSTl8PAuAdqWYSMnLOv"

// 특정 시간으로 생성
val ksuidAtTime = Ksuid.generate(Instant.now())
val ksuidAtDate = Ksuid.generate(Date())
val ksuidAtDateTime = Ksuid.generate(LocalDateTime.now())

// 여러 ID 생성
val ids = Ksuid.nextIds(10)

// KSUID 파싱
val pretty = Ksuid.prettyString(ksuid)
// Time = 2024-01-15T10:30:45Z
// Timestamp = 1705315845
// Payload = a1b2c3d4e5f6...
```

### Flake (Boundary 스타일)

128bit ID, MAC 주소 기반 노드 식별

```kotlin
import io.bluetape4k.idgenerators.flake.Flake

val flake = Flake()

// ByteArray로 생성 (16 bytes = 128 bits)
val id: ByteArray = flake.nextId()

// Base62 문자열로 생성
val idString: String = flake.nextIdAsString()  // 예: "AmknwjEj6DWnSOpkRM"

// Hex 문자열로 변환
val hexString = Flake.asHexString(id)  // 예: "0000019265902e57beab72881e400000"

// 컴포넌트 분해
val components = Flake.asComponentString(id)  // "timestamp-nodeId-sequence"
```

### Hashids (YouTube 스타일 Short URL)

숫자/UUID를 짧은 문자열로 인코딩

```kotlin
import io.bluetape4k.idgenerators.hashids.Hashids
import io.bluetape4k.idgenerators.hashids.encodeUUID
import io.bluetape4k.idgenerators.hashids.decodeUUID

// 기본 설정
val hashids = Hashids(salt = "my secret salt")

// Long 인코딩
val encoded = hashids.encode(123456789L)  // "abc123XYZ"

// 여러 Long 인코딩
val encoded2 = hashids.encode(1L, 2L, 3L)  // "xyz789"

// 음수 지원
val encoded3 = hashids.encode(1L, -1L)

// 큰 수 지원 (Long.MAX_VALUE)
val encoded4 = hashids.encode(Long.MAX_VALUE)

// 디코딩
val decoded = hashids.decode(encoded)  // longArrayOf(123456789)

// UUID 인코딩/디코딩
val uuid = UUID.randomUUID()
val encodedUuid = hashids.encodeUUID(uuid)
val decodedUuid = hashids.decodeUUID(encodedUuid)
// decodedUuid == uuid
```

### Hashids 커스텀 설정

```kotlin
import io.bluetape4k.idgenerators.hashids.Hashids

// 최소 길이 지정
val hashids = Hashids(
    salt = "my salt",
    minHashLength = 10,  // 최소 10자리
    customAlphabet = "0123456789abcdef"  // 16진수 알파벳
)

val encoded = hashids.encode(1234567L)  // "b332db5" (최소 10자리가 되도록 패딩)
```

### Base62 UUID 인코딩

```kotlin
import java.util.UUID

// UUID를 Base62로 인코딩 (36자리 → 22자리)
val uuid = UUID.randomUUID()
val encoded = uuid.toBase62String()  // "QLfDyyhZrm9uVtDzQcs4R"

// Base62를 UUID로 디코딩
val decoded = encoded.toBase62Uuid()  // 원본 UUID
```

## ID 생성기 선택 가이드

| 요구사항           | 추천 알고리즘         |
|----------------|-----------------|
| 분산 환경, 기계별 구분  | Snowflake       |
| 중앙집중식 ID 서비스   | GlobalSnowflake |
| DB 기본키, 정렬 필요  | Timebased UUID  |
| 완전 랜덤, 보안      | Random UUID     |
| URL Safe, 가독성  | KSUID           |
| 128bit, 높은 유일성 | Flake           |
| Short URL, 난독화 | Hashids         |

## Snowflake 구조

```
| 1 bit |     41 bits      |   10 bits   |   12 bits   |
| sign  |    timestamp     |  machineId  |  sequence   |
|  0    | milliseconds since epoch |  0-1023   |   0-4095   |
```

- **timestamp**: 41 bits, 약 69년간 유일성 보장
- **machineId**: 10 bits, 최대 1024개 기계
- **sequence**: 12 bits, millisecond당 최대 4096개 ID

## 주요 기능 상세

| 파일                               | 설명                    |
|----------------------------------|-----------------------|
| `IdGenerator.kt`                 | ID 생성기 인터페이스          |
| `LongIdGenerator.kt`             | Long 타입 ID 생성기        |
| `snowflake/Snowflake.kt`         | Snowflake 인터페이스       |
| `snowflake/DefaultSnowflake.kt`  | Twitter 스타일 Snowflake |
| `snowflake/GlobalSnowflake.kt`   | 중앙집중식 Snowflake       |
| `snowflake/SnowflakeSupport.kt`  | Snowflake 유틸리티        |
| `snowflake/SnowflakeId.kt`       | Snowflake ID 파싱 결과    |
| `uuid/TimebasedUuidGenerator.kt` | 시간 기반 UUID 생성기        |
| `uuid/RandomUuidGenerator.kt`    | 랜덤 UUID 생성기           |
| `uuid/NamebasedUuidGenerator.kt` | 이름 기반 UUID 생성기        |
| `ksuid/Ksuid.kt`                 | KSUID 생성기             |
| `flake/Flake.kt`                 | Flake ID 생성기          |
| `hashids/Hashids.kt`             | Hashids 알고리즘          |
| `hashids/HashidsSupport.kt`      | Hashids 확장 함수         |
| `utils/node/NodeIdentifier.kt`   | 노드 식별자 인터페이스          |
| `MachineIdSupport.kt`            | 기계 ID 생성 유틸리티         |

## 참고 자료

- [Twitter Snowflake](https://developer.twitter.com/en/docs/basics/twitter-ids)
- [A brief history of the UUID](https://segment.com/blog/a-brief-history-of-the-uuid/)
- [KSUID](https://github.com/ksuid/ksuid)
- [Boundary Flake](https://github.com/boundary/flake)
- [Hashids](https://hashids.org)
- [Java UUID Generator](https://github.com/cowtowncoder/java-uuid-generator)
