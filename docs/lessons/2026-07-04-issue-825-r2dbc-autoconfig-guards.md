# Lessons Learned - Issue #825 R2DBC Auto-Configuration Guards

## Context

`R2dbcClientAutoConfiguration` was guarded only by `DatabaseClient`, but the
auto-configured bean method also used `R2dbcEntityTemplate` and
`MappingR2dbcConverter`. Those Spring Data R2DBC types are compile-only for the
published module.

## Lesson

Spring Boot auto-configuration classes should guard every compile-only type that
appears in bean method signatures. Use string-based `@ConditionalOnClass` names
when the guard exists to prevent class-loading failures before the condition can
short-circuit.

## Outcome

The R2DBC auto-configuration now checks all Spring R2DBC signature types and
backs off when a user-defined `R2dbcClient` bean exists.

## Future Guard

When adding auto-configured beans with compile-only parameters, add
`ApplicationContextRunner` tests for both missing classpath behavior and custom
bean backoff.

## Verification

- `:bluetape4k-r2dbc:compileKotlin` and `:bluetape4k-r2dbc:compileTestKotlin`
  passed.
- `:bluetape4k-r2dbc:test` passed with 188 tests.
- `:bluetape4k-r2dbc:koverXmlReport` generated the XML coverage report.
- `git diff --check` passed.
