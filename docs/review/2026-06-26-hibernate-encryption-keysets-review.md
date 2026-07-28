# Review - Hibernate Encryption Keysets (2026-06-26)

Issue: #816
Branch: `fix/hibernate-encryption-keysets`
Module: `:bluetape4k-hibernate`

## Scope

- `EncryptedStringConverterKeysets` now requires explicit persisted keyset JSON before built-in encrypted converters
  process non-null values.
- `AESStringConverter` and `DeterministicAESStringConverter` no longer use generated process-local default
  `TinkEncryptors`.
- README files document the external key material requirement.
- Converter tests cover unconfigured fail-fast, same-key cross-instance success, and different-key failure.

## 7-Tier 검토

| Tier | Result | Evidence |
|---|---:|---|
| Correctness | PASS | Non-null conversion resolves encryptors lazily from configured keysets; null values still return null. |
| Security | PASS | Process-local generated keysets are no longer used by built-in persistent converters. |
| Public API | PASS | New public configuration surface accepts `String` keyset JSON; `TinkEncryptor` helper methods remain internal. |
| Persistence compatibility | PASS | Existing JPA converter no-arg constructors remain available and targeted Hibernate converter tests pass. |
| Test coverage | PASS | Added fail-fast, same-key restart-safe, and different-key restart-unsafe tests for AES-GCM and AES-SIV. |
| Documentation | PASS | `README.md` and `README.ko.md` describe key material storage and bootstrap requirements. |
| Regression risk | PASS | Targeted module tests pass; full module test has an unrelated pre-existing `org.hibernate.KeyType` blocker on `develop`. |

## 발견 사항

P0: 0
P1: 0

P2/P3: none requiring code changes before PR.

## 검증 Evidence

- `./gradlew :bluetape4k-hibernate:compileKotlin :bluetape4k-hibernate:compileTestKotlin --no-build-cache`
  - Result: PASS.
- `./gradlew :bluetape4k-hibernate:test --tests 'io.bluetape4k.hibernate.converter.EncryptedStringConverterTest' --tests 'io.bluetape4k.hibernate.converter.ConverterTest' --tests 'io.bluetape4k.hibernate.mapping.embeddable.EmbeddableTest' --no-build-cache`
  - Result: PASS.
- `./gradlew :bluetape4k-hibernate:test --no-build-cache`
  - Result: FAIL, unrelated existing natural-id tests fail with `NoClassDefFoundError: org/hibernate/KeyType`.
  - Develop verification: the same two natural-id tests fail on clean `develop` with the same `org.hibernate.KeyType` error.
