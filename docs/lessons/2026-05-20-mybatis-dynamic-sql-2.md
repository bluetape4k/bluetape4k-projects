# MyBatis Dynamic SQL 2

## Context

MyBatis Dynamic SQL 2.0 changed the independent where rendering APIs used by
the Vert.x SQL client integration.

## Decision

Adapt the existing Vert.x rendering bridge to the 2.0 API instead of keeping a
compatibility shim around removed 1.x overloads.

## Outcome

- `WhereModel.renderForVertx()` now renders through `RenderingContext` and
  returns `Optional<FragmentAndParameters>`.
- Insert model extension generics are constrained with `T : Any` to match 2.0
  signatures.
- Kotlin count/delete DSL wrappers now use the package-level DSL types required
  by MyBatis Dynamic SQL 2.0.
- AWS SDK Java, AWS SDK Kotlin, and Fory Kotlin were also materialized from the
  central catalog after the related Dependabot PRs were folded into the central
  upgrade batch.
- Fory 0.17 keeps only the single-size `buildThreadSafeForyPool(int)` builder
  API, so local serializers preserve the former max-pool size as the pool size.
- Timefold catalog aliases that no longer exist in 2.x were removed from the
  materialized catalog.

## Verification

- `./gradlew :bluetape4k-vertx:compileTestKotlin --no-daemon`
- `./gradlew :bluetape4k-io:compileTestKotlin --no-daemon`
- `./gradlew :bluetape4k-io:test --no-daemon`
- `./gradlew build -x test --parallel --no-daemon`

Existing unrelated deprecation warnings remained in tests and infrastructure
code.
