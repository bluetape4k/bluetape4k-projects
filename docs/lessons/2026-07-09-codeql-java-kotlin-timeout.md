# CodeQL Java/Kotlin Timeout

## Context

After re-enabling the CodeQL `java-kotlin` matrix with a workflow-local Kotlin
`2.3.21` pin, scheduled CodeQL runs were cancelled after about six hours.

## Decision

Keep the repository catalog on Kotlin `2.4.0` and keep the CodeQL-only
`2.3.21` rewrite, but narrow the manual Java/Kotlin build from full
`assemble` to scoped generated library compiler tasks.

## Outcome

The workflow now asks CodeQL to trace library production source compilation in
separate Java/Kotlin scopes instead of examples, demos, benchmarks, archive
tasks, distribution tasks, resource processing, and aggregate assembly. Testing
helpers are kept in a `testing-core` scope. The `bluetape4k-testcontainers`
module is temporarily excluded because its single `compileKotlin` task stayed
in `Build with Gradle` after about 31 minutes under CodeQL tracing; issue #999
tracks restoring that coverage. The Gradle build step also has an explicit
120-minute timeout so future regressions do not consume the default 360-minute
GitHub Actions timeout.

## Future Guidance

For CodeQL Kotlin analysis, do not assume a successful normal Gradle build means
the same command is appropriate under CodeQL tracing. Build only the source set
needed for extraction, keep the workflow-local Kotlin pin isolated, and verify
with a live CodeQL workflow dispatch.
