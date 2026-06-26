# Lessons Learned - Hibernate Encryption Keysets (2026-06-26)

Related issue: #816
Affected module: `:bluetape4k-hibernate`

## L1: Persistent converters must not create process-local keys

### Problem

`AESStringConverter` and `DeterministicAESStringConverter` were documented as persistent entity-field converters, but
their default encryptors used generated in-process Tink keysets. That made same-process tests pass while persisted
ciphertext could become unreadable after restart or in another application instance.

### Lesson

Persistent encryption converters need explicit externally stored key material. Tests must cover both restart-safe
positive cases with the same persisted keyset and restart-unsafe negative cases with a different keyset.

### Future guard

When adding a persistent encryption API, do not validate only same-instance round trips. Add a cross-instance or
cross-keyset regression test and document where key material must live.
