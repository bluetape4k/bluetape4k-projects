# Issue 492 Codec Trust Profiles Plan

## Tasks

1. Add `SerializationTrustProfile` to `bluetape4k-io`.
   - English KDoc for each profile.
   - Include a stable display name for public docs.
   - Add a focused unit test for profile names.

2. Harden `RedissonProtobufCodec`.
   - Add `allowedClassPrefixes` with default
     `ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES`.
   - Add `ALLOW_ALL_CLASSES_UNSAFE` as the explicit legacy bypass.
   - Use `RedissonCodecs.Kryo5` as the default non-Protobuf fallback.
   - Add a public `allowedClassPrefixes` constructor so README snippets compile.
   - Validate `Any.typeUrl` class names before `Class.forName`.
   - Reject blank prefixes and package-prefix spoofing.
   - Preserve fallback decoding for non-protobuf payloads.
   - Rethrow `SecurityException`; do not silently fall back after a trust
     boundary violation.
   - Preserve Redisson class-loader constructors.

3. Tighten `ProtobufSerializer` allowlist matching.
   - Reject blank custom prefixes.
   - Match exact class names or package-boundary prefixes only.
   - Add prefix-spoofing regression tests.

4. Add representative tests.
   - Default codec rejects an untrusted protobuf `Any.typeUrl`.
   - Explicit unsafe opt-in attempts legacy class loading; for a missing class
     it falls through to the existing fallback path instead of throwing the
     allowlist `SecurityException`.
   - Public `allowedClassPrefixes` constructor supports named-argument examples.
   - Prefix spoofing and blank prefixes are rejected.
   - Existing protobuf and fallback round-trip tests continue to pass.

5. Update public docs.
   - Add `docs/security/serialization-trust-profiles.md`.
   - Update `io/io` README pair for profile vocabulary.
   - Update `io/protobuf` README pair for protobuf and Redisson protobuf
     defaults.
   - Update `infra/redisson` README pair for trusted-internal vs allowlisted
     codecs.
   - Update Kafka and Kafka4 README pairs with secure and unsafe legacy
     examples; fix Kafka4 `emptySet()` drift.

6. Run verification.
   - IntelliJ diagnostics/reformat/imports for touched Kotlin files.
   - `./gradlew :bluetape4k-io:test --tests 'io.bluetape4k.io.serializer.SerializationTrustProfileTest' --console=plain --no-configuration-cache`
   - `./gradlew :bluetape4k-protobuf:test --tests 'io.bluetape4k.protobuf.serializers.redis.RedissonProtobufCodecTest' --console=plain --no-configuration-cache`
   - Targeted Kafka/Kafka4 codec tests after README/source consistency review if no code changed there.
   - `git diff --check`.

7. Run current-session Step 6-R review, write lesson, commit, push, create PR,
   add PR review comment, and wait for CI.

## Risks And Rollback

| Risk | Mitigation |
|---|---|
| Redisson protobuf default reject breaks deployments using non-bluetape4k package protobuf classes. | Document `allowedClassPrefixes` and `ALLOW_ALL_CLASSES_UNSAFE` migration path. |
| Constructor change breaks Redisson dynamic codec creation. | Keep class-loader constructors and add tests around default construction behavior through existing codec list. |
| Docs drift from Kafka behavior. | Grep source for `allowedTypePackages` and `ALLOW_ALL_TYPES_UNSAFE`; update both Kafka README pairs. |

## Step 3-R Session Review

External Claude/Codex CLI advisor passes are intentionally skipped by user
directive for this session. Review was performed in the current Codex session.

| Perspective | P0 | P1 | P2/P3 | Plan edit |
|---|---:|---:|---|---|
| Implementer | 0 | 0 | P2: constructor compatibility needs explicit task. | Added class-loader constructor preservation. |
| Test engineer | 0 | 0 | P2: unsafe opt-in should be tested without requiring a real untrusted class. | Test expects no allowlist exception and fallback path behavior. |
| Architect | 0 | 0 | P2: shared enum should not force immediate serializer interface refactor. | Non-goal recorded in spec. |
| Delivery/docs | 0 | 0 | P2: README localized pairs must be updated together. | README pair list included. |

Step 3-R convergence: `P0 = 0`, `P1 = 0`.
