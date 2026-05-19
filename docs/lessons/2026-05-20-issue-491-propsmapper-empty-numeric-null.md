# Issue 491 Java Properties Empty Numeric Nulls

## Context

`JavaPropsMapper` reads Java Properties values as untyped strings. Empty
properties such as `maxWaitMillis=` and `idleTimeout=` were deserialized into
nullable numeric Kotlin constructor parameters as `0`, which made generated
properties fail to round-trip against the original `null` model.

## Decision

Keep the fix local to `JacksonText.Props.defaultMapper` for Jackson 2 and
Jackson 3. Apply Jackson coercion metadata for numeric logical types and add a
Properties-only numeric module that maps an empty string to `null` for boxed
numeric types before parsing non-empty values normally.

## Outcome

The disabled named datasource properties round-trip tests are enabled in both
Jackson 2 and Jackson 3. Empty nullable numeric properties now deserialize as
`null`; other text formats and CSV mapper defaults are unchanged.

## Verification

`./gradlew :bluetape4k-jackson2:test --tests 'io.bluetape4k.jackson.text.datasources.ParseNamedDataSourcePropertiesTest' :bluetape4k-jackson3:test --tests 'io.bluetape4k.jackson3.text.datasources.ParseNamedDataSourcePropertiesTest' --console=plain --no-configuration-cache` passed with 4 tests per module.

`./gradlew :bluetape4k-jackson2:test :bluetape4k-jackson3:test --console=plain --no-configuration-cache` passed with 430 Jackson 2 tests and 432 Jackson 3 tests.

## Future Guard

Do not broaden Java Properties coercion fixes into CSV/TOML/YAML mappers unless
their own tests prove the same bug. Java Properties scalar handling is format
specific, so keep future coercion modules attached only to the affected mapper.
