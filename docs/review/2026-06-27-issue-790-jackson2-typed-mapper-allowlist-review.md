# Issue 790 Review: Jackson2 typed mapper allowlist preservation

## Scope

- `Jackson.createTypedJsonMapper(...)`
- Legacy `Jackson.typedJsonMapper` default typing compatibility path
- Jackson2 typed mapper regression tests
- Jackson2 README locale pair

## Findings

No P0/P1 findings.

## Checks

- `createTypedJsonMapper(...)` now installs Jackson's property-based default typing with the configured `PolymorphicTypeValidator` intact.
- The trusted package list is enforced as subtype package prefixes with `allowIfSubType(...)`.
- Disallowed root `Any` polymorphic payloads are rejected instead of bypassing the allowlist.
- Disallowed nested payloads are rejected, and allowed payloads still round-trip with `@class` property type ids.
- Deprecated `typedJsonMapper` remains a legacy compatibility path and is no longer promoted in the class KDoc or README examples.

## Verification Evidence

- Red test before implementation: `createTypedJsonMapper - denied root payload is rejected` failed with `Expected InvalidTypeIdException but no exception was thrown`.
- Initial nested denied-package test passed before the fix; it was retained for nested-property coverage but was not the root reproducer.
- Targeted `createTypedJsonMapper*` Jackson tests: passed.
- Full `JacksonTest`: passed.
- Full `:bluetape4k-jackson2:test`: 433 tests passed.
- `:bluetape4k-jackson2:compileTestKotlin --warning-mode all --rerun-tasks`: passed; only existing root Gradle Kotlin DSL deprecation warnings were reported.
- `git diff --check`: passed.

## Residual Risk

The public parameter name `allowedBasePackages` is historical. The safe factory now treats it as trusted subtype package prefixes to satisfy the security contract, and the KDoc clarifies that behavior. Downstream callers that relied on the safe factory accepting arbitrary external root `Any` subtype ids must migrate those payloads to trusted packages or use an explicit legacy compatibility mapper only for trusted JSON.

## Concurrency Helper Gate

No `MultithreadingTester`, `SuspendedJobTester`, or `StructuredTaskScopeTester` was added. The change is a synchronous Jackson mapper construction and deserialization trust-boundary fix with no shared mutable state, coroutine lifecycle, virtual-thread behavior, or structured task scope behavior.
