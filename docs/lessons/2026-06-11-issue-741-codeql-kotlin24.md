# Issue #741 CodeQL Kotlin 2.4

## Context

Kotlin 2.4.0 landed before CodeQL Java/Kotlin extractor support caught up.
Nightly and Publish Snapshot were green on the same head, but CodeQL failed only
on the `java-kotlin` axis.

## Lesson

When a security scanner has a language support-window mismatch, keep unaffected
scanner axes running and disable only the incompatible axis. Do not downgrade
the project toolchain just to satisfy scanner lag.

## Follow-up Guard

When re-enabling CodeQL `java-kotlin`, use a true compile-only Gradle command
such as `assemble`. `build -x test` is not enough in this repository because
custom `Test` tasks can still enter the task graph.

## Evidence

- CodeQL run `27250113912`: `actions` and `python` passed, `java-kotlin`
  failed.
- Nightly run `27299345368`: succeeded on the same SHA.
- Publish Snapshot run `27299715400`: succeeded on the same SHA.
- Local workflow validation: `actionlint .github/workflows/codeql.yml`.
