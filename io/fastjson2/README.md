# Module bluetape4k-fastjson2

English | [한국어](./README.ko.md)

## Overview

`bluetape4k-fastjson2` is a module that wraps the [Fastjson2](https://github.com/alibaba/fastjson2) library as Kotlin extension functions.

It provides high-performance serialization using JSONB (binary JSON) format, as well as type-safe deserialization extension functions for various data sources including JSON strings,
`InputStream`, `JSONObject`, and `JSONArray`.

## Architecture Diagrams

### Fastjson2 Class Structure

![Fastjson2 Class Structure diagram](../../docs/images/readme-diagrams/io-fastjson2-diagram-01.png)

### JSON vs JSONB Serialization Flow

![JSON vs JSONB Serialization Flow diagram](../../docs/images/readme-diagrams/io-fastjson2-diagram-02.png)

## Key Features

### 1. FastjsonSerializer

Implements the `JsonSerializer` interface, using JSONB for byte arrays and standard JSON for strings.

```kotlin
import io.bluetape4k.fastjson2.FastjsonSerializer

val serializer = FastjsonSerializer()

// JSONB binary serialization/deserialization (high-performance)
val bytes = serializer.serialize(user)
val restored = serializer.deserialize<User>(bytes)

// JSON string serialization/deserialization
val jsonText = serializer.serializeAsString(user)
val restored2 = serializer.deserializeFromString<User>(jsonText)

// Throws JsonSerializationException on failure
try {
    serializer.deserialize<User>(byteArrayOf(1, 2, 3))
} catch (e: JsonSerializationException) {
    // handle
}
```

`FastjsonSerializer` failure policy:

- `serialize(null)` returns an empty `ByteArray`; `serializeAsString(null)` returns an empty string.
- `deserialize(null)` / `deserializeFromString(null)` returns `null`.
- Ordinary backend serialization/deserialization failures throw `JsonSerializationException`.

#### ByteBuffer contract

For writable array-backed heap buffers and slices, `deserializeFrom` is optimized to pass the backing array,
offset, and remaining length directly to JSONB. Direct and read-only input use a bounded-copy compatibility
fallback. All input paths preserve position, limit, mark, and byte order. Feature-free readers are used and
AutoType is not enabled.
Set a bounded limit before passing untrusted input; the serializer cannot read outside the remaining range.

`serializeTo` is an allocating compatibility fallback: Fastjson2 2.0.62 exposes output through
`JSONB.toBytes`, so the result is copied into the caller target. Output position is committed only on success;
read-only and overflow failures remain raw buffer exceptions. The output allocation remains, and this API makes
no lower-copy output claim.
Fatal `Error` instances retain their identity instead of being wrapped.

```kotlin
import io.bluetape4k.fastjson2.FastjsonSerializer
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.json.deserialize
import java.nio.ByteBuffer

run {
    val buffer = ByteBuffer.allocate(4096)
    serializer.serializeTo(users, buffer)
    buffer.flip()
    val restored = serializer.deserialize<List<User>>(buffer)
}
run {
    val buffer = ByteBuffer.wrap(serializer.serialize(usersByName))
    val restored = serializer.deserialize<Map<String, User>>(buffer)
}

val contract: JsonSerializer = serializer
val buffer = ByteBuffer.wrap(serializer.serialize(users))
val rawUsers: List<*>? = contract.deserialize<List<User>>(buffer)
```

The concrete overload retains generic `Type` information. A receiver statically typed as `JsonSerializer`
keeps the compatibility class-token behavior, so collection elements remain raw maps.

```java
ByteBuffer buffer = ByteBuffer.wrap(bytes);
User restored = serializer.deserializeFrom(buffer, User.class);
```

### 2. JSON String Extension Functions

Extension functions to convert JSON strings to various types.

```kotlin
import io.bluetape4k.fastjson2.extensions.*

// Object → JSON string
val json = user.toJsonString()

// JSON string → object
val user = json.readValueOrNull<User>()

// JSON array string → List
val users = jsonArrayString.readValueAsList<User>()

// JSON string → JSONObject
val jsonObject = json.readAsJSONObject()
```

### 3. JSONB Binary Extension Functions

Serializes/deserializes using Fastjson2's JSONB (binary JSON) format, which offers better performance and compression than text JSON.

```kotlin
import io.bluetape4k.fastjson2.extensions.*

// Object → JSONB byte array
val bytes = user.toJsonBytes()

// JSONB byte array → object
val restored = bytes.readBytesOrNull<User>()

// InputStream → object
val user = inputStream.readBytesOrNull<User>()
```

### 4. JSONArray Extension Functions

Type-safe data extraction from `JSONArray`.

```kotlin
import io.bluetape4k.fastjson2.extensions.*

val jsonArray: JSONArray = JSONArray()

// Convert entire array to a specific type
val data = jsonArray.readValueOrNull<MyData>()

// Convert element at a specific index
val user = jsonArray.readValueOrNull<User>(0)

// Convert to List or Array
val users = jsonArray.readList<User>()
val userArray = jsonArray.readArray<User>()
```

### 5. JSONObject Extension Functions

Type-safe data extraction from `JSONObject`.

```kotlin
import io.bluetape4k.fastjson2.extensions.*

val jsonObject: JSONObject = JSONObject()

// Convert entire object to a specific type
val user = jsonObject.readValueOrNull<User>()

// Convert value at a specific key
val user = jsonObject.readValueOrNull<User>("key")
```

## JSONB vs JSON Comparison

| Format         | Speed    | Size   | Readability | Use Case                           |
|----------------|----------|--------|-------------|------------------------------------|
| JSONB (binary) | Fast     | Small  | No          | Internal serialization, cache, RPC |
| JSON (text)    | Moderate | Medium | Yes         | API responses, logging, debugging  |

## Dependencies

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-fastjson2:${bluetape4kVersion}")

    // Included automatically
    // api("com.alibaba.fastjson2:fastjson2")
    // api("com.alibaba.fastjson2:fastjson2-kotlin")
}
```

## Module Structure

```
io.bluetape4k.fastjson2
├── FastjsonSerializer.kt              # JsonSerializer implementation
└── extensions/
    ├── JSONExtensions.kt              # String, InputStream extension functions
    ├── JSONBExtensions.kt             # JSONB binary extension functions
    ├── JSONArrayExtensions.kt         # JSONArray extension functions
    └── JSONObjectExtensions.kt        # JSONObject extension functions
```

## Testing

```bash
./gradlew :bluetape4k-fastjson2:test
```

## References

- [Fastjson2](https://github.com/alibaba/fastjson2)
- [JSONB Specification](https://github.com/alibaba/fastjson2/wiki/jsonb_format_cn)
