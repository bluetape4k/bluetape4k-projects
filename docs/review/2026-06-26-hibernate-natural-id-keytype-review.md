# Review - Hibernate Natural Id KeyType (2026-06-26)

Issue: #908
Branch: `fix/hibernate-natural-id-keytype`
Module: `:bluetape4k-hibernate`

## Scope

- Removed the runtime dependency on `org.hibernate.KeyType` from natural-id helper methods.
- Updated simple and composite natural-id lookup helpers to use Hibernate 7 natural-id loader APIs.

## 7-Tier 검토

| Tier | Result | Evidence |
|---|---:|---|
| Correctness | PASS | `findBySimpleNaturalId` delegates to `bySimpleNaturalId(...).load(...)`; composite lookup delegates to `byNaturalId(...).using(...).load()`. |
| Compatibility | PASS | Verified Hibernate `7.2.7.Final` test runtime exposes the loader APIs and does not provide `org.hibernate.KeyType`. |
| Validation | PASS | Empty natural-id map and blank attribute-name guards remain unchanged. |
| Test coverage | PASS | Targeted EntityManager and Session natural-id tests pass; full `:bluetape4k-hibernate:test` passes. |
| API surface | PASS | Public extension function signatures are unchanged. |
| Documentation | PASS | KDoc notes the Hibernate 7 `KeyType.NATURAL` removal path. |
| Regression risk | PASS | Single production file touched, no unrelated behavior changes. |

## 발견 사항

P0: 0
P1: 0

P2/P3: none requiring code changes before PR.

## 검증 Evidence

- Reproduced before fix:
  - `./gradlew :bluetape4k-hibernate:test --tests 'io.bluetape4k.hibernate.EntityManagerSupportTest.entity manager natural id helpers 는 simple natural id 조회를 지원한다' --tests 'io.bluetape4k.hibernate.SessionSupportTest.session natural id helpers 는 simple natural id 조회를 지원한다' --no-build-cache`
  - Result: FAIL with `NoClassDefFoundError: org/hibernate/KeyType`.
- After fix:
  - `./gradlew :bluetape4k-hibernate:compileKotlin :bluetape4k-hibernate:compileTestKotlin --no-build-cache`
  - Result: PASS.
  - `./gradlew :bluetape4k-hibernate:test --tests 'io.bluetape4k.hibernate.EntityManagerSupportTest.entity manager natural id helpers 는 simple natural id 조회를 지원한다' --tests 'io.bluetape4k.hibernate.SessionSupportTest.session natural id helpers 는 simple natural id 조회를 지원한다' --no-build-cache`
  - Result: PASS.
  - `./gradlew :bluetape4k-hibernate:test --no-build-cache`
  - Result: PASS.
