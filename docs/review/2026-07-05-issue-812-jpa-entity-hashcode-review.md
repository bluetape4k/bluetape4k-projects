# Issue #812 7-Tier 검토

## Scope

- Issue: #812, `P1: Fix transient equals/hashCode contract in Hibernate entity base`
- Files:
  - `data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/AbstractJpaEntity.kt`
  - `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/model/AbstractJpaEntityUnitTest.kt`

## 발견 사항

- P0/P1 findings: 0
- Contract: PASS. Equal transient entities now share the Hibernate entity class hash instead of identity hash.
- Persisted behavior: PASS. Persisted entities still hash by assigned identifier.
- Regression coverage: PASS. Unit tests cover equal transient hash equality, `HashSet` single logical element behavior, and persisted ID hash behavior.
- Kotlin/bluetape4k style: PASS. Tests use bluetape4k assertions with infix matchers where applicable and no method-local MockK setup.
- Public API documentation: PASS. Touched public KDoc is English and documents the transient hash contract.

## Tool Evidence

- CodeGraph incremental update: 2 files re-parsed, 26 nodes and 144 edges updated, no dependent files.
- CodeGraph change detection: 2 changed files, risk score 0.55, 0 affected flows.
- CodeGraph reported test gaps for `AbstractJpaEntity` and `hashCode`; reviewed as covered by the new direct unit tests in `AbstractJpaEntityUnitTest`.

## Verification

- PASS: `./gradlew :bluetape4k-hibernate:compileKotlin :bluetape4k-hibernate:compileTestKotlin :bluetape4k-hibernate:test --tests 'io.bluetape4k.hibernate.model.AbstractJpaEntityUnitTest' --no-build-cache --no-configuration-cache`
  - 15 tests passing.
- PASS: `./gradlew :bluetape4k-hibernate:compileKotlin :bluetape4k-hibernate:compileTestKotlin :bluetape4k-hibernate:test :bluetape4k-hibernate:koverXmlReport --no-build-cache --no-configuration-cache`
  - 498 main tests passing.
  - 3 consumer runtime tests passing.
  - Kover XML report generated.
