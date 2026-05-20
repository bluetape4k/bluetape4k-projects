# Issue 539 - Binary Kafka Codecs

## Context

`JdkKafkaCodec` and compressed JDK-backed Kafka codecs were already deprecated
because JDK deserialization is unsafe for untrusted bytes. Issue #539 asked to
stage the migration before final removal.

## Decision

Apply the new `@BluetapeObsoleteApi` marker and raise Kotlin deprecation to
`DeprecationLevel.ERROR` for the JDK-backed kafka/kafka4 codec APIs. Keep the
registry entries only as explicit compatibility bridges and point callers to the
Fory variants.

## Outcome

- Added `bluetape4k-annotations` as an API dependency for kafka/kafka4.
- Marked direct and compressed JDK codec classes and registry properties as
  obsolete/error-gated.
- Removed JDK codec coverage from codec round-trip tests; safe Kryo/Fory
  variants remain covered.
- Updated README codec tables and the deprecated inventory note.

## Verification

- `./gradlew :bluetape4k-kafka:compileKotlin :bluetape4k-kafka:test --tests 'io.bluetape4k.kafka.codec.KafkaCodecTest' --console=plain --no-configuration-cache`
- `./gradlew :bluetape4k-kafka:compileKotlin :bluetape4k-kafka4:compileKotlin :bluetape4k-kafka4:test --tests 'io.bluetape4k.kafka.codec.KafkaCodecTest' --console=plain --no-configuration-cache`

## Future Guidance

For #474, remove these compatibility APIs in the breaking cleanup slice rather
than re-testing JDK deserialization. Prefer Fory for new examples and migration
docs.
