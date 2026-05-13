# idgenerator Spring Boot demo rename

## Context

The Spring Boot idgenerator example originally lived under
`spring-boot/idgenerator-demo`, which produced the Gradle task
`:bluetape4k-spring-boot-idgenerator-demo`. After the Ktor example was renamed
to `idgenerator-ktor-demo`, the Spring Boot example needed the same
subject-first naming style.

## Decision

- Move the example to `examples/spring-boot/idgenerator-spring-boot-demo`.
- Register nested Spring Boot examples with
  `includeModules("examples/spring-boot", false, false)` so the Gradle task is
  `:idgenerator-spring-boot-demo`.
- Update Examples workflow, Nightly exclusion, README command snippets, and
  lessons to use the new path and task name.

## Outcome

The two idgenerator examples now use matching runnable example names:

- `:idgenerator-ktor-demo`
- `:idgenerator-spring-boot-demo`

Both live under `examples/**`, so root Gradle sample filtering excludes them
from publishing and Kover by project directory.

## Verification Evidence

```bash
./gradlew -q projects | rg "idgenerator-(ktor|spring-boot)-demo"
```

Expected result: both idgenerator demo projects are registered.

```bash
repo-test-summary -- ./gradlew :idgenerator-spring-boot-demo:compileKotlin :idgenerator-spring-boot-demo:compileTestKotlin :idgenerator-spring-boot-demo:test --parallel
```

Expected result: Spring Boot idgenerator demo tests pass.

## Future Guidance

Prefer subject-first example names such as `idgenerator-ktor-demo` and
`idgenerator-spring-boot-demo` for runnable examples that compare framework
integrations. Keep workflow task paths and README snippets in sync with the
Gradle project name in the same change.
