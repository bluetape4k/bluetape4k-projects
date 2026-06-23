# Issue #881 Science NetCDF API Migration

Issue #881 tracked Java deprecation warnings in `NetCdfCatalogService` caused by
metadata access through deprecated NetCDF-Java bean properties:

- `Variable.getAttributes()`, surfaced in Kotlin as `v.attributes`
- `NetcdfFile.getDimensions()`, surfaced in Kotlin as `nc.dimensions`

## Decision

Use the NetCDF-Java 5.9.1 replacement API where the current source points:

- variable attributes come from `Variable.attributes()`
- root-file dimensions come from `nc.rootGroup.getDimensions()`

The service still writes the same bluetape4k domain shape:

- `NetCdfVariableInfo.attributes`
- `NetCdfFileRecord.dimensions`
- `NetCdfFileRecord.globalAttrs`

No repository, schema, import, or Gradle 10 cleanup behavior changed.

## Lessons

- Kotlin bean-property syntax can hide deprecated Java getters. When a Java
  library deprecates a getter, prefer explicit method calls or a local helper
  that names the intended API.
- `NetcdfFile.getDimensions()` is a global view that NetCDF-Java discourages.
  For the current registration behavior, root group dimensions preserve the
  existing root-file metadata shape without recursing through nested groups.
- The simple source grep is useful for this issue because it catches accidental
  reintroduction of the deprecated Kotlin property forms.

## Verification

- `./gradlew :bluetape4k-science:compileKotlin --warning-mode all`
- `./gradlew :bluetape4k-science:compileTestKotlin --warning-mode all`
- `./gradlew :bluetape4k-science:test --tests '*NetCdfCatalogServiceTest' --tests '*NetCdfTableTest'`
  passed with 37 tests.
- `rg "\.attributes\b|\.dimensions\b" utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`
  returned no matches.
- `git diff --check` passed.
