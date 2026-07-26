# Refreshing JDBC DataSource Implementation Plan

Date: 2026-06-30 Repository: `bluetape4k-projects`
Branch: `feat/projects-refreshing-jdbc-datasource`
Spec: `docs/superpowers/specs/2026-06-30-refreshing-jdbc-datasource-design.md`

## Goal

Add a reusable `bluetape4k-jdbc` `DataSource` that obtains the current JDBC password for every physical connection and delegates to `DriverManager`, so
`bluetape4k-aws` can later remove its internal RDS IAM JDBC wrapper after a published helper artifact exists.

## Stop Conditions

- Stop the upstream PR after `bluetape4k-projects` tests, docs, and CI are green.
- Do not edit `bluetape4k-aws` source to consume the helper until a snapshot or stable `bluetape4k-jdbc` artifact containing it is published and dependency resolution is proven.
- If the helper API shape needs to change after tests start, update the spec before continuing implementation.

## Implementation Sequence

### 1. Add Failing Tests First

File:
`data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/datasource/RefreshingJdbcPasswordDataSourceTest.kt`

Test cases:

- H2 smoke: `getConnection()` opens a connection and calls
  `JdbcPasswordProvider.currentPassword()`.
- Repeated physical connections: two direct `getConnection()` calls invoke the provider twice.
- Rejected overload: `getConnection(username, password)` throws `SQLException`
  with the stable secret-free message.
- Null password: provider returns null and `SQLException(config.nullPasswordMessage)`
  is thrown without leaking URL, properties, or sentinel secrets.
- Validation: blank URL, blank username, blank null message, blank driver class name, and blank property keys fail with `IllegalArgumentException`.
- Fake Driver capture:
    - register a local `Driver` for a sentinel JDBC URL;
    - capture each received `Properties` instance;
    - prove base `user` and `password` entries are overwritten;
    - prove each call receives a distinct `Properties` instance;
    - mutate the first captured properties and prove the mutation does not leak into later calls;
    - deregister the driver in teardown.
- Concurrent fake Driver calls: use `MultithreadingTester` and assert provider calls equal physical connection attempts and every call receives a distinct
  `Properties` instance.
- Wrapper methods: `isWrapperFor`, successful `unwrap`, and failed `unwrap`
  with stable `SQLException("Not a wrapper for <fqcn>.")`.
- Secret-free `toString()`: provider is not called and output excludes full URL query strings, URL userinfo, `password`, `token`, and `sslpassword` sentinel values.
- Provider failure: provider exception is wrapped in a secret-free `SQLException`
  with the cause preserved.
- Driver class load failure: failure includes the class name but not URL, properties, or credentials.
- DriverManager global methods: log writer and login timeout delegate to process-wide `DriverManager`; save and restore original state.

Expected first result: tests fail to compile because the new public API does not exist.

### 2. Implement The Generic Helper

File:
`data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/datasource/RefreshingJdbcPasswordDataSource.kt`

Public API:

- `fun interface JdbcPasswordProvider`
- `data class RefreshingJdbcPasswordDataSourceConfig : Serializable`
- `class RefreshingJdbcPasswordDataSource : DataSource`

Implementation rules:

- Validate config with existing bluetape4k validation helpers.
- Copy `dataSourceProperties` in the constructor.
- Load optional `driverClassName` in the constructor.
- Build a fresh per-call `Properties` object and write base properties first, then `user`, then `password`.
- Reject caller-supplied credentials before invoking the provider.
- Wrap provider failures as secret-free `SQLException` with cause.
- Propagate `DriverManager.getConnection` `SQLException` unchanged.
- Delegate log writer/login timeout to `DriverManager`.
- Implement wrapper methods only for the helper instance.
- Keep `toString()` sanitized and provider-free.
- Do not implement `Closeable`, `AutoCloseable`, or `Serializable` on the
  `DataSource` class or provider interface.

### 3. Update Documentation

Files:

- `data/jdbc/README.md`
- `data/jdbc/README.ko.md`

Required content:

- Source-equivalent sections for `RefreshingJdbcPasswordDataSource`.
- Imports from `io.bluetape4k.jdbc.datasource`.
- Runnable-style Kotlin snippet using
  `RefreshingJdbcPasswordDataSourceConfig`.
- Hikari wrapping example using
  `dataSource = RefreshingJdbcPasswordDataSource(...)`.
- Warning that `getConnection(username, password)` is rejected.
- Warning that log writer/login timeout methods use process-wide
  `DriverManager` state.
- Unsupported cases: no pooling, no scheduled refresh, no async provider, no generic static credential helper, no caller-supplied credential override.
- Note that `dataSourceProperties` can contain vendor driver options, but secret-bearing entries are not diagnostic-safe; `user` and `password` are overwritten.

### 4. Local Verification

Run in `bluetape4k-projects` worktree:

```bash
./gradlew :bluetape4k-jdbc:test --no-daemon --stacktrace
./gradlew :bluetape4k-jdbc:koverXmlReport --no-daemon --stacktrace
git diff --check
```

If tests fail:

- fix implementation or tests;
- rerun the smallest failing command first;
- then rerun the full verification set above.

### 5. PR Preparation

- Commit spec, review artifact, plan, implementation, tests, and README updates with Lore-protocol commit messages.
- Create a `bluetape4k-projects` PR against `develop`.
- Set assignee `debop`.
- Reference `bluetape4k-aws#295` and explain that AWS consumption is blocked until publication.
- Keep the final PR body section as `## DoD Status`.
- Verify PR body via `gh pr view --json body` before reporting the PR ready.
- Verify PR checks include the `test-data` lane that runs
  `:bluetape4k-jdbc:test` and `:bluetape4k-jdbc:koverXmlReport`.

## Downstream AWS Follow-Up

After a consumable helper artifact exists:

- update `bluetape4k-aws` catalog to the selected snapshot or stable version;
- prove dependency resolution from the expected repository;
- replace the internal `RdsIamRefreshingDataSource`;
- keep RDS IAM Hikari configuration on the `dataSource` path only;
- run `:bluetape4k-aws-exposed:test`;
- open the AWS PR for issue #295.
