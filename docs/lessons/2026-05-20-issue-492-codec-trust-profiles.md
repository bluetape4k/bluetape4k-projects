# Issue 492 Codec Trust Profiles

## Context

Issue #492 needed a shared trust-profile vocabulary, explicit unsafe legacy
codec modes, Kafka secure/legacy examples, and tests for one representative
codec.

## Decision

Add `SerializationTrustProfile` as shared public vocabulary and document codec
defaults in `docs/security/serialization-trust-profiles.md`. Harden protobuf
dynamic type loading by making `RedissonProtobufCodec` default to
`AllowListedTypes` with Kryo5 fallback and by tightening package-prefix matching
in both `RedissonProtobufCodec` and `ProtobufSerializer`.

## Outcome

Kafka/Kafka4 docs now describe `emptySet()` as deny-all and show explicit
allowlist and unsafe legacy examples. Redisson protobuf unsafe legacy behavior
is available only through `ALLOW_ALL_CLASSES_UNSAFE`. Prefix spoofing and blank
allowlist entries are rejected by tests.

## Verification

- IntelliJ reformat/import optimization ran for touched Kotlin files.
- `./gradlew :bluetape4k-io:test --tests 'io.bluetape4k.io.serializer.SerializationTrustProfileTest' :bluetape4k-protobuf:test :bluetape4k-kafka:test --tests 'io.bluetape4k.kafka.codec.JacksonKafkaCodecSecurityTest' :bluetape4k-kafka4:test --tests 'io.bluetape4k.kafka.codec.JacksonKafkaCodecSecurityTest' --tests 'io.bluetape4k.kafka.codec.KafkaCodecAllowlistTest' --console=plain --no-configuration-cache` passed.
- `javap` confirmed `RedissonProtobufCodec` keeps no-arg, `Codec`, `Set`, `(Codec, Set)`, and Redisson class-loader constructors.
- `git diff --check` passed.

## Next Time

When documenting deny-all defaults, scan existing README snippets for examples
that still imply default polymorphic round-trips. Also check named-argument
constructor examples against the actual public Kotlin API before publishing
migration docs.
