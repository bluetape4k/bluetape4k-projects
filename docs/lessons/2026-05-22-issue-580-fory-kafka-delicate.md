# Issue 580 Fory Kafka Trust Boundary

## Context

Issue #580 flagged that the Fory-backed Kafka codecs replaced obsolete JDK codecs
but still exposed an unregistered-class deserialization trust boundary.

## Decision

Mark `ForyKafkaCodec`, `LZ4ForyKafkaCodec`, `SnappyForyKafkaCodec`, and
`ZstdForyKafkaCodec` as `@BluetapeDelicateApi` in both Kafka 3 and Kafka 4
modules. Mark the corresponding `KafkaCodecs` registry properties as delicate
too, so callers using the registry see the same opt-in warning.

## Outcome

The public API now surfaces the Fory trust boundary without removing source
compatibility. README files document that Fory-backed codecs are for trusted
topics and brokers unless callers provide a class-registration-enforced custom
serializer.

## Verification

- IntelliJ diagnostics: no problems in touched production codec files.
- `./gradlew :bluetape4k-kafka:compileKotlin :bluetape4k-kafka:compileTestKotlin :bluetape4k-kafka4:compileKotlin :bluetape4k-kafka4:compileTestKotlin --continue --no-configuration-cache --max-workers=2`
- `./gradlew :bluetape4k-kafka:test --tests 'io.bluetape4k.kafka.codec.KafkaCodecTest' :bluetape4k-kafka4:test --tests 'io.bluetape4k.kafka.codec.KafkaCodecTest' --no-configuration-cache --max-workers=1`

## Future Guidance

When marking a public API with a Bluetape opt-in annotation, add the annotations
module as an `api` dependency if consumers must see the marker in signatures.
