# 이슈 539 - Binary Kafka Codec

## 배경

`JdkKafkaCodec`와 compressed JDK-backed Kafka codec은 untrusted bytes에 대한 JDK deserialization이
unsafe하기 때문에 이미 deprecated 상태였다. Issue #539는 final removal 전에 migration을 staging하라고
요청했다.

## 결정

JDK-backed kafka/kafka4 codec API에 새 `@BluetapeObsoleteApi` marker를 적용하고 Kotlin deprecation을
`DeprecationLevel.ERROR`로 올린다. Registry entry는 explicit compatibility bridge로만 유지하고 caller를
Fory variant로 안내한다.

## 결과

- `bluetape4k-annotations`를 kafka/kafka4의 API dependency로 추가.
- Direct 및 compressed JDK codec class와 registry property를 obsolete/error-gated로 표시.
- Codec round-trip test에서 JDK codec coverage 제거. Safe Kryo/Fory variant coverage는 유지.
- README codec table과 deprecated inventory note 갱신.

## 검증

- `./gradlew :bluetape4k-kafka:compileKotlin :bluetape4k-kafka:test --tests 'io.bluetape4k.kafka.codec.KafkaCodecTest' --console=plain --no-configuration-cache`
- `./gradlew :bluetape4k-kafka:compileKotlin :bluetape4k-kafka4:compileKotlin :bluetape4k-kafka4:test --tests 'io.bluetape4k.kafka.codec.KafkaCodecTest' --console=plain --no-configuration-cache`

## 향후 가이드

#474에서는 JDK deserialization을 다시 test하지 말고 breaking cleanup slice에서 이 compatibility API를
제거한다. 새 example과 migration doc에는 Fory를 우선 사용한다.
