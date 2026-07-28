# Review - Hibernate Reactive Vert.x Alignment (2026-06-26)

Issue: #912
Branch: `fix/hibernate-reactive-vertx`
Affected modules: `:bluetape4k-hibernate`, `:bluetape4k-hibernate-reactive`, `:bluetape4k-hibernate-cache-lettuce`

## Scope

- Aligned Hibernate ORM from `7.3.4.Final` to `7.4.2.Final`.
- Aligned Hibernate Reactive from `4.3.3.Final` to `4.5.0.Final`.
- Kept the repository-wide Vert.x line at `5.1.3`.

## 7-Tier 검토

| Tier | Result | Evidence |
|---|---:|---|
| Correctness | PASS | `:bluetape4k-hibernate-reactive:test` no longer fails with Vert.x `RowBase` runtime API mismatch. |
| Compatibility | PASS | Hibernate ORM and Reactive are aligned to the dependency line declared by Hibernate Reactive `4.5.0.Final`. |
| Boundary behavior | PASS | The change is catalog-only; public APIs and source code are unchanged. |
| Test coverage | PASS | Hibernate core, Hibernate Reactive, and Hibernate cache Lettuce tests pass locally. |
| Simplicity | PASS | Two catalog version entries changed; no dependency override or workaround was added. |
| Documentation | PASS | Lesson artifact records the ORM/Reactive/Vert.x coupling. |
| Regression risk | PASS | Resolved dependency versions match the intended line: ORM `7.4.2.Final`, Reactive `4.5.0.Final`, Vert.x SQL `5.1.3`. |

## 발견 사항

P0: 0
P1: 0

P2/P3: none requiring code changes before PR.

## 검증 Evidence

- Reproduced before fix:
  - Data CI candidate command failed at `:bluetape4k-hibernate-reactive:test`.
  - Root error: `NoSuchMethodError: io.vertx.sqlclient.impl.RowBase.<init>(java.util.Collection)`.
- Candidate rejected:
  - `hibernate-reactive = 4.5.0.Final` alone failed with `NoClassDefFoundError: org/hibernate/service/internal/ChangesetCoordinatorInitiator`.
  - Hibernate Reactive `4.5.0.Final` POM expects Hibernate ORM `7.4.2.Final`.
- After fix:
  - `./gradlew :bluetape4k-hibernate-reactive:test --max-workers=1 --no-configuration-cache`
  - Result: PASS.
  - `./gradlew :bluetape4k-hibernate:test --max-workers=1 --no-configuration-cache`
  - Result: PASS.
  - `./gradlew :bluetape4k-hibernate-cache-lettuce:test --max-workers=1 --no-configuration-cache`
  - Result: PASS.
  - `./gradlew :bluetape4k-jdbc:test :bluetape4k-hibernate:test :bluetape4k-hibernate-cache-lettuce:test :bluetape4k-hibernate-reactive:test :bluetape4k-r2dbc:test :bluetape4k-mongodb:test :bluetape4k-cassandra:test --max-workers=1 --no-configuration-cache`
  - Result: PASS.
  - `./gradlew :bluetape4k-hibernate-reactive:dependencyInsight --configuration testRuntimeClasspath ...`
  - Result: resolved `hibernate-reactive-core:4.5.0.Final`, `hibernate-core:7.4.2.Final`, `vertx-sql-client:5.1.3`, and `vertx-mysql-client:5.1.3`.
