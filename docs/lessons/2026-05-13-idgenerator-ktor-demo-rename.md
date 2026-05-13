# idgenerator Ktor demo rename

## Context

Post-merge review of PR #421 found that the nested Ktor example was registered
as `:idgenerator-ktor`, while the rest of the runnable example modules use a
demo-oriented naming convention. The module also lives beside other examples
that are excluded from publishing and Kover by project directory.

## Decision

- Rename `examples/ktor/idgenerator-ktor` to
  `examples/ktor/idgenerator-ktor-demo`.
- Keep `includeModules("examples/ktor", false, false)` so the Gradle project
  name becomes `:idgenerator-ktor-demo`.
- Update Examples workflow and Nightly build exclusion commands to use the new
  task path.
- Update README and lesson command examples to use `:idgenerator-ktor-demo`.

## Outcome

The Ktor idgenerator example now has an explicit demo suffix in both directory
and Gradle task names. The root Gradle sample/project-dir filtering still
excludes it from publishing and Kover because the project directory remains
under `examples/**`.

## Verification Evidence

```bash
./gradlew -q projects | rg "idgenerator-ktor-demo"
```

Expected result: `:idgenerator-ktor-demo` is registered.

```bash
repo-test-summary -- ./gradlew :idgenerator-ktor-demo:compileKotlin :idgenerator-ktor-demo:test --parallel
```

Expected result: Ktor demo tests pass.

## Future Guidance

When adding runnable example modules, include `-demo` in the directory and
Gradle project name unless there is a deliberate user-facing reason not to.
After a rename, update workflow task paths, Nightly exclusions, README command
snippets, and lesson evidence strings together.
