# Issue #825 R2DBC Auto-Configuration 검토

Date: 2026-07-04
Repo: `bluetape4k-projects`
Scope: `data/r2dbc` auto-configuration classpath guards and bean backoff

## Gate

- P0: 0
- P1: 0
- Verdict: PASS

## 7-Tier 검토

### Tier 1 - Correctness

- PASS. `R2dbcClientAutoConfiguration` now requires every Spring R2DBC type used
  by the bean signature before activation.
- PASS. `R2dbcClient` auto-registration now backs off when an application
  provides its own `R2dbcClient` bean.

### Tier 2 - API and Compatibility

- PASS. No public `R2dbcClient` API was changed.
- PASS. The auto-configuration remains registered through the existing
  `AutoConfiguration.imports` and legacy `spring.factories` entries.

### Tier 3 - Security and Privacy

- PASS. No credential, SQL, logging, or user-data handling changed.

### Tier 4 - Kotlin and bluetape4k Patterns

- PASS. The fix uses Spring Boot conditional annotations directly and keeps the
  implementation narrowly scoped.
- PASS. Tests use `bluetape4k-assertions` and infix identity assertion style.
- PASS. No MockK operation mocking was introduced.

### Tier 5 - Tests

- PASS. Added `ApplicationContextRunner` coverage for successful default bean
  registration, custom bean backoff, and missing Spring Data R2DBC classpath
  behavior.
- PASS. The partial-classpath test validates startup without failure and no
  accidental `R2dbcClient` bean registration.

### Tier 6 - Operations

- PASS. No workflow, CI, module registration, or dependency catalog changes were
  needed.

### Tier 7 - Documentation and Evidence

- PASS. Public KDoc for the auto-configuration now states activation and backoff
  contracts in English.
- PASS. This review and the issue lesson are committed with the implementation.

## Verification Evidence

- Compile validation:
  `./gradlew :bluetape4k-r2dbc:compileKotlin :bluetape4k-r2dbc:compileTestKotlin --no-build-cache --no-configuration-cache` passed.
- Module validation:
  `./gradlew :bluetape4k-r2dbc:test --no-build-cache --no-configuration-cache` passed with 188 tests.
- Coverage report:
  `./gradlew :bluetape4k-r2dbc:koverXmlReport --no-build-cache --no-configuration-cache` passed and generated `data/r2dbc/build/reports/kover/report.xml`.
- `git diff --check` passed.

## Residual Risk

- Consumers that still rely on legacy `spring.factories` auto-configuration are
  preserved, but future Spring Boot cleanup should re-evaluate whether the
  legacy entry is still needed.
