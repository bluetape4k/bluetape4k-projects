# 이슈 580 Fory Kafka Trust Boundary

## 배경

Issue #580은 obsolete JDK codec을 대체한 Fory-backed Kafka codec이 여전히 unregistered-class
deserialization trust boundary를 expose한다고 지적했다.

## 결정

Kafka 3과 Kafka 4 module 모두에서 `ForyKafkaCodec`, `LZ4ForyKafkaCodec`, `SnappyForyKafkaCodec`,
`ZstdForyKafkaCodec`을 `@BluetapeDelicateApi`로 표시한다. Registry를 사용하는 caller도 같은 opt-in
warning을 보도록 대응하는 `KafkaCodecs` registry property도 delicate로 표시한다.

## 결과

Public API는 source compatibility를 제거하지 않고 Fory trust boundary를 드러낸다. README는 caller가
class-registration-enforced custom serializer를 제공하지 않는 한 Fory-backed codec이 trusted topic과
broker용임을 문서화한다.

## 검증

- IntelliJ diagnostics: 수정한 production codec file 문제 없음.
- `./gradlew :bluetape4k-kafka:compileKotlin :bluetape4k-kafka:compileTestKotlin :bluetape4k-kafka4:compileKotlin :bluetape4k-kafka4:compileTestKotlin --continue --no-configuration-cache --max-workers=2`
- `./gradlew :bluetape4k-kafka:test --tests 'io.bluetape4k.kafka.codec.KafkaCodecTest' :bluetape4k-kafka4:test --tests 'io.bluetape4k.kafka.codec.KafkaCodecTest' --no-configuration-cache --max-workers=1`

## 향후 가이드

Public API에 Bluetape opt-in annotation을 표시하면 consumer가 signature에서 marker를 볼 수 있도록
annotations module을 `api` dependency로 추가한다.
