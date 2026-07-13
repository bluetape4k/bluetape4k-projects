---
title: Converter와 보안 경계
description: JSON, 압축, 암호화와 직렬화 converter의 저장 형식과 실패 계약을 설명합니다.
manualId: bluetape4k-hibernate
chapterId: converters-security
---

# Converter와 보안 경계

## 저장 형식은 schema 계약이다

AttributeConverter는 Kotlin 타입을 column 값으로 바꾸지만, 한번 저장한 형식은 schema와 같은 호환성 계약이 됩니다. serializer, 압축 알고리즘, JSON 모델이나 암호화 key를 바꿀 때는 기존 row를 읽을 migration 경로가 필요합니다.

`LocaleAsStringConverter`는 language tag를 저장하고 읽을 때 `_`를 `-`로 정규화합니다. `DurationAsTimestampConverter`는 duration의 millisecond를 `Timestamp` 값으로 저장합니다. 이는 절대 시각이 아니므로 column 이름과 schema 문서에 기간 값임을 명시합니다.

## JSON 변환 실패는 null이 된다

`AbstractObjectAsJsonConverter<T>`는 Jackson으로 JSON 문자열을 저장합니다. 직렬화나 역직렬화가 실패하면 예외를 던지지 않고 error log를 남긴 뒤 `null`을 반환합니다.

```kotlin
class OptionAsJsonConverter:
    AbstractObjectAsJsonConverter<Option>(Option::class.java)
```

필수 데이터에 이 converter를 사용하면 잘못된 값이 null처럼 보일 수 있습니다. column nullability만 믿지 말고 저장 전 validation과 읽은 뒤 invariant 검사를 둡니다. 실패를 반드시 호출자에게 알려야 하는 도메인이라면 예외를 전파하는 전용 converter를 작성합니다.

## 암호화 keyset은 애플리케이션이 소유한다

`AESStringConverter`는 AES-GCM이라 같은 평문도 다른 암호문을 만듭니다. `DeterministicAESStringConverter`는 AES-SIV를 사용해 equality lookup이 가능하지만 같은 평문이 같은 암호문으로 나타나 패턴이 노출됩니다.

```kotlin
EncryptedStringConverterKeysets.configureAesKeyset(
    secretManager.load("hibernate/aes-keyset")
)
```

- keyset JSON은 secret key material이다. source, log, 평문 설정 파일에 넣지 않습니다.
- converter가 null이 아닌 값을 처리하기 전에 bootstrap을 끝냅니다. 설정이 없으면 `IllegalStateException`으로 실패합니다.
- 다른 keyset으로 기존 암호문을 복호화할 수 없습니다. 배포 전에 key rotation과 재암호화 절차를 정합니다.
- 검색이 필요하지 않다면 비결정적 AES converter를 우선합니다.

## Generic object 역직렬화는 피한다

generic ByteArray·Base64 converter는 arbitrary payload를 역직렬화하므로 deprecated이며 trusted-storage-only입니다. 외부 입력이나 변조 가능한 database를 통과할 때는 target type을 검사하는 `AbstractTypedObjectAsByteArrayConverter<T>` 또는 `AbstractTypedObjectAsBase64StringConverter<T>`와 secure serializer를 사용합니다.

압축 converter는 저장 공간을 줄일 수 있지만 작은 문자열에는 header와 CPU 비용이 더 클 수 있습니다. 실제 payload 분포로 압축률과 latency를 측정하고 알고리즘을 고정합니다.

## 실행 예제

```bash
./gradlew :bluetape4k-hibernate:test \
  --tests 'io.bluetape4k.hibernate.converter.EncryptedStringConverterTest'

./gradlew :bluetape4k-hibernate:test \
  --tests 'io.bluetape4k.hibernate.converter.JsonStringConverterTest'
```

## Source와 tests

- [`AbstractObjectAsJsonConverter.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/AbstractObjectAsJsonConverter.kt)
- [`EncryptedStringConverters.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/EncryptedStringConverters.kt)
- [`ObjectAsByteArrayConverter.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/ObjectAsByteArrayConverter.kt)
- [`ObjectAsBase64StringConverter.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/ObjectAsBase64StringConverter.kt)
- [`EncryptedStringConverterTest.kt`](../../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/converter/EncryptedStringConverterTest.kt)
