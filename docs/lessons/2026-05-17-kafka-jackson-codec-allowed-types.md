# Kafka JacksonKafkaCodec: allowedTypePackages 보안 기능으로 인한 테스트 실패 (2026-05-17)

## 근본 원인

`JacksonKafkaCodec`은 1.8.0에서 `allowedTypePackages` 허용 목록을 도입했다(기본값은 empty).
Kafka producer/consumer property에서 class reference로 codec을 구성하면:

```kotlin
this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonKafkaCodec::class.java
```

Kafka는 no-arg constructor로 class를 instantiate하고, 이 constructor는
`allowedTypePackages = emptySet()`을 설정한다. Deserialization 시점에 `getValueType()`은
Kafka type header의 모든 class name을 거부한다. Empty set과 match되는 것이 없기 때문에
`IllegalArgumentException`이 발생한다.

Spring Kafka는 deserializer exception을 catch하고 listener에 `KafkaNull` payload를 전달한다.
Listener method는 typed object(`Greeting`)를 기대하므로 Spring은
`MethodArgumentNotValidException: Payload value must not be empty`를 던진다.
Listener는 counter를 증가시키지 못하고, `await until { counter >= 2 }`가 timeout 된다.

CI failure는 `ConditionTimeoutException`(기본 10초)으로 표시되어 실제 원인을 가렸다.

## 수정

Class-reference 기반 serializer/deserializer configuration에서 instance 기반 configuration으로 전환해
`allowedTypePackages`를 명시적으로 설정할 수 있게 한다:

```kotlin
private fun valueCodec() = JacksonKafkaCodec(
    allowedTypePackages = AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE
)

@Bean
@Suppress("UNCHECKED_CAST")
fun producerFactory(): ProducerFactory<String, Greeting> {
    return KafkaServer.Launcher.Spring.getProducerFactory(
        keySerializer = StringKafkaCodec(),
        valueSerializer = valueCodec()
    ) as ProducerFactory<String, Greeting>
}
```

CI 부하를 위한 safety buffer로 Awaitility timeout도 기본 10초에서 30초로 늘렸다.

## 교훈

`JacksonKafkaCodec` 또는 `AbstractKafkaCodec` subclass를 Kafka property에서 class reference로
구성하면 no-arg constructor가 사용된다. 따라서 `allowedTypePackages`는 항상 empty다.
Class-reference 기반 codec config를 쓰는 test는 1.8.0 보안 upgrade 이후 조용히 실패할 수 있다.

**규칙**: `JacksonKafkaCodec`으로 typed object를 deserialize하는 test에서는
`*_CLASS_CONFIG = SomeCodec::class.java` 대신 instance 기반 factory configuration
(`KafkaServer.Launcher.Spring.getProducerFactory(serializer, ...)`)을 항상 사용한다.
