# Issue #881 Science NetCDF API Migration Review

## Scope

- `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`

## Verdict

Local 7-tier equivalent review: APPROVE.

P0/P1 findings: 0.

## Review Notes

| Lens | Finding | Severity | Resolution |
|---|---|---:|---|
| API migration | Kotlin `v.attributes` called deprecated `Variable.getAttributes()`. | P1 | Registration now reads variable metadata through the current `Variable.attributes()` API. |
| API migration | Kotlin `nc.dimensions` called deprecated `NetcdfFile.getDimensions()`. | P1 | Registration now reads root dimensions through `nc.rootGroup.getDimensions()`. |
| Output compatibility | The migration must keep JSONB domain shapes stable. | P1 | `NetCdfVariableInfo.attributes`, `NetCdfFileRecord.dimensions`, and `globalAttrs` mapping logic is unchanged. |
| Scope control | Gradle 10 cleanup warnings are out of scope. | P3 | No Gradle build-script cleanup was included. |
| Regression evidence | Representative registration and persistence paths must stay equivalent. | P1 | `NetCdfCatalogServiceTest` and `NetCdfTableTest` passed with 37 tests. |

## Verification

- `./gradlew :bluetape4k-science:compileKotlin --warning-mode all` passed.
- `./gradlew :bluetape4k-science:compileTestKotlin --warning-mode all` passed.
- `./gradlew :bluetape4k-science:test --tests '*NetCdfCatalogServiceTest' --tests '*NetCdfTableTest'` passed:
  - `NetCdfCatalogServiceTest`: 33 tests, 0 failures, 0 errors, 0 skipped.
  - `NetCdfTableTest`: 4 tests, 0 failures, 0 errors, 0 skipped.
- `rg "\.attributes\b|\.dimensions\b" utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt` returned no matches.
- `git diff --check` passed.
