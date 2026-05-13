# idgenerator Spring Boot demo review follow-ups

## Context

Post-merge review of PR #422 found no blocking runtime issue, but it did surface
small review follow-ups in the new Spring Boot idgenerator demo:

- `IdGeneratorEntry` was a `data class` even though its supplier lambda is an
  implementation detail rather than value semantics.
- `IdGeneratorProperties` accepted invalid batch-size combinations until a
  request reached the service layer.
- Main-source KDoc in the demo module was Korean, while contributor-facing
  public KDoc in this repository is expected to be English.

## Decision

- Keep REST response DTOs as data classes, but keep registry entries as plain
  classes when they wrap behavior or private suppliers.
- Validate configuration-property invariants at construction time when a bad
  value would make every normal request fail later.
- Treat demo module main-source KDoc as contributor-facing code documentation
  and write it in English; keep README locale pairs multilingual.
- When post-merge review produces a corrective PR, include a lesson document in
  the same PR so future agents can find the review outcome.

## Outcome

PR #426 applies the follow-ups from PR #422 review:

- `IdGeneratorEntry` no longer exposes generated `copy`/component/equality
  semantics around the ID supplier.
- `IdGeneratorProperties` rejects non-positive or inconsistent batch limits.
- The Spring Boot idgenerator demo main-source KDoc now uses English.
- Regression coverage was added for invalid batch limit combinations.

## Verification Evidence

```bash
repo-test-summary -- ./gradlew :idgenerator-spring-boot-demo:compileKotlin :idgenerator-spring-boot-demo:compileTestKotlin :idgenerator-spring-boot-demo:test --parallel
```

Result: 8 tests passing.

```bash
git diff --check
```

Result: no whitespace errors.

## Future Guidance

For example modules, review registry/helper holder classes for accidental data
class semantics before merging. If a class wraps a lambda, resource, generator,
or other behavior, prefer a plain class unless value equality and `copy` are
part of the explicit contract.

For configuration properties, fail fast when invalid values make the default
endpoint behavior unusable. Add a direct regression test for each invariant.

For post-merge review fixes, include both the corrective code change and a
lesson entry in the follow-up PR.
