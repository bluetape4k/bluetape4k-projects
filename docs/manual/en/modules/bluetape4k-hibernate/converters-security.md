---
title: Converters and security boundaries
description: Define storage and failure contracts for JSON, compression, encryption, and serialization converters.
manualId: bluetape4k-hibernate
chapterId: converters-security
---

# Converters and security boundaries

## Treat the format as schema

An AttributeConverter makes a Kotlin type fit a column, but its JSON model, serializer, compression algorithm, or encryption key becomes a durable storage contract. Plan how old rows remain readable before changing it.

`DurationAsTimestampConverter` stores duration milliseconds as a `Timestamp`; document that the column contains a duration, not an instant.

## JSON failures become null

`AbstractObjectAsJsonConverter<T>` logs Jackson serialization and parsing failures and returns null. Required data therefore needs validation before storage and an invariant check after loading. Write a domain converter that propagates failure when silent null is unacceptable.

## Own encryption keysets outside the process

`AESStringConverter` uses non-deterministic AES-GCM. `DeterministicAESStringConverter` uses AES-SIV for equality lookup but reveals equality patterns.

```kotlin
EncryptedStringConverterKeysets.configureAesKeyset(
    secretManager.load("hibernate/aes-keyset")
)
```

- Load keysets from a protected external store before conversion.
- Missing key material fails fast.
- A different keyset cannot decrypt existing ciphertext; design rotation before deployment.
- Do not put cleartext keyset JSON in source, logs, or plain configuration.

Generic object converters are deprecated and trusted-storage-only. Prefer the typed ByteArray or Base64 converter base with a secure serializer. Benchmark compression against representative payloads before fixing the stored format.

## Executable tests

```bash
./gradlew :bluetape4k-hibernate:test --tests '*EncryptedStringConverterTest'
./gradlew :bluetape4k-hibernate:test --tests '*JsonStringConverterTest'
```

## Sources and tests

- [`AbstractObjectAsJsonConverter.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/AbstractObjectAsJsonConverter.kt)
- [`EncryptedStringConverters.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/EncryptedStringConverters.kt)
- [`ObjectAsByteArrayConverter.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/ObjectAsByteArrayConverter.kt)
- [`EncryptedStringConverterTest.kt`](../../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/converter/EncryptedStringConverterTest.kt)
