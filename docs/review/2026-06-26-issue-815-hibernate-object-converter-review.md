# Issue 815 - Hibernate Object Converter 검토

## Scope

- `ObjectAsByteArrayConverter.kt`
- `ObjectAsBase64StringConverter.kt`
- converter unit tests and consumer runtime test fixtures
- `data/hibernate` README files

## 발견 사항

No P0/P1 findings found in the local review pass.

## 증거

- `compileTestKotlin --warning-mode all` passed. New generic converter deprecation warnings are suppressed only in legacy converter declarations and legacy test fixtures.
- Targeted converter tests passed: 77 tests, including malformed payload, unexpected payload type, and secure Kryo/Fory disallowed payload cases.
- Full `:bluetape4k-hibernate:test --rerun-tasks` passed: 494 tests.
- `:bluetape4k-hibernate:consumerRuntimeTest` passed: 3 tests.
- `:bluetape4k-hibernate:check` passed.
- `git diff --check` passed.
- CodeGraph `detect_changes` reported risk score 0.00 and 0 test gaps.

## Residual Risk

The legacy generic `Any?` converters remain available for binary compatibility. They are deprecated and documented as trusted-storage-only, but existing downstream consumers must migrate deliberately to the typed converter bases.
