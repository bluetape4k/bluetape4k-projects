# Refreshing JDBC DataSource Design

Date: 2026-06-30
Repositories: `bluetape4k-projects`, `bluetape4k-aws`
Primary issue: bluetape4k-aws #295
Primary branch: `feat/projects-refreshing-jdbc-datasource`

## Problem

`bluetape4k-aws` issue #295 tracks the remaining ecosystem-reuse gap in the
RDS IAM JDBC path. `aws-exposed` currently owns an internal
`RdsIamRefreshingDataSource` that builds a fresh `Properties` object for every
physical JDBC connection, asks `AwsDatabasePasswordProvider.currentPassword()`
for the current token, injects that token as the JDBC password, and delegates
to `DriverManager.getConnection(url, properties)`.

The behavior is correct, but the low-level JDBC connection creation wrapper is
not AWS-specific. The reusable boundary belongs in `bluetape4k-jdbc`, while AWS
should only supply the username, base properties, token-producing function, and
AWS-specific error message for null tokens.

## Current Evidence

- `bluetape4k-aws/aws-exposed/src/main/kotlin/io/bluetape4k/aws/exposed/AwsJdbcDataSourceFactory.kt`
  contains the only production `DriverManager` use in the AWS worktree.
- `RdsIamRefreshingDataSource.getConnection()` creates per-call JDBC
  properties, sets `user`, sets a token-derived `password`, and calls
  `DriverManager.getConnection(url, connectionProperties)`.
- `RdsIamRefreshingDataSource.getConnection(username, password)` rejects
  caller-supplied credentials with `SQLException`.
- `data/jdbc` currently provides `hikariDataSourceOf`, `DataSource` execution
  extensions, transaction helpers, and JDBC driver constants, but no
  refresh-aware `DataSource` or `DriverManager` wrapper.
- `javap javax.sql.DataSource java.sql.DriverManager` confirms the needed
  public surface: `DataSource` has `getConnection()` and
  `getConnection(String, String)`, while `DriverManager` exposes
  `getConnection(String, Properties)`, log writer, and login timeout methods.
- `bluetape4k-aws` consumes `io.github.bluetape4k:bluetape4k-jdbc` through
  catalog version `1.11.0`. A new helper in `bluetape4k-projects` cannot be
  used by AWS remote CI until the helper is published in a version AWS can
  consume.
- Prior AWS docs for #269 explicitly kept JDBC password refresh,
  Hikari, and `DriverManager` behavior in `aws-exposed` and marked #295 as
  out of scope. Prior #294 review states the remaining direct `DriverManager`
  usage is intentionally isolated for this follow-up.

## Constraints

- Keep `bluetape4k-jdbc` generic. It must not depend on AWS, Exposed, Hikari,
  or token-specific types.
- Preserve per-physical-connection refresh semantics. The password supplier
  must run for every no-arg `getConnection()` call.
- Preserve caller-supplied credential rejection for this helper. Accepting
  `getConnection(username, password)` would bypass the refresh contract.
- Preserve existing `DriverManager` log writer and login timeout behavior.
- Keep connection properties per call so one connection cannot mutate state
  visible to later connections.
- Public API and KDoc must be English.
- `README.md` and `README.ko.md` for `data/jdbc` must remain source-equivalent.
- `bluetape4k-aws` cannot consume the helper from a normal PR until a new
  `bluetape4k-jdbc` version or snapshot containing the helper is available.

## Design Options

### Option A: Move `RdsIamRefreshingDataSource` Into `bluetape4k-jdbc`

Move the class almost verbatim and make it public.

Rejected. The class name and null-password error are RDS IAM specific, and a
move would leak AWS vocabulary into a generic JDBC module.

### Option B: Add A Generic `DriverManagerDataSource` With Optional Credentials

Expose a reusable `DataSource` that accepts optional static or dynamic
credentials and delegates to `DriverManager`.

Rejected. A broad credential model expands the contract beyond #295 and makes
the dangerous overload behavior ambiguous. For RDS IAM, caller-supplied
credentials must be rejected, not optionally merged.

### Option C: Add A Focused Refreshing Password `DataSource`

Add a generic `RefreshingJdbcPasswordDataSource` to
`io.bluetape4k.jdbc.datasource`
that accepts:

- `config: RefreshingJdbcPasswordDataSourceConfig`
- `passwordProvider: JdbcPasswordProvider`

The helper loads the optional JDBC driver class in `init`, builds a new
`Properties` object on each no-arg `getConnection()`, copies base properties,
sets `user`, obtains the current password, fails with `SQLException` when the
provider returns null, sets `password`, and delegates to
`DriverManager.getConnection(url, properties)`. It rejects
`getConnection(username, password)`.

Selected. This keeps the reusable behavior generic, preserves AWS semantics,
and keeps token redaction responsibility outside `bluetape4k-jdbc`.

## API Shape

Package: `io.bluetape4k.jdbc.datasource`

- `fun interface JdbcPasswordProvider`
  - `fun currentPassword(): String?`
  - Public-facing contract says the function is called for every physical
    connection opened through `RefreshingJdbcPasswordDataSource`.
  - It is called synchronously during `getConnection()`. Provider exceptions
    propagate as connection acquisition failures, and providers must not log
    secrets.
  - Providers must be thread-safe. Connection pools may open multiple physical
    connections concurrently during warm-up or scale-out, and expensive token
    generation should be cached or coalesced outside the generic DataSource.
- `data class RefreshingJdbcPasswordDataSourceConfig`
  - `url: String`
  - `driverClassName: String? = null`
  - `username: String`
  - `dataSourceProperties: Map<String, String> = emptyMap()`
  - `nullPasswordMessage: String = "JDBC password provider returned null."`
  - The constructor validates `url`, `username`, and `nullPasswordMessage` as
    nonblank values using bluetape4k validation helpers.
  - `driverClassName`, when present, is validated as nonblank.
  - `dataSourceProperties` keys are validated as nonblank. Values are copied
    without semantic validation because JDBC drivers own vendor-specific
    option interpretation.
  - The config implements `Serializable` and defines `serialVersionUID`.
- `class RefreshingJdbcPasswordDataSource : DataSource`
  - Public construction uses a config object plus `passwordProvider` to avoid
    same-type positional mistakes.
  - Constructor copies `config.dataSourceProperties` to an immutable `Map`.
  - `getConnection()` obtains the latest password and delegates to
    `DriverManager.getConnection(url, properties)`.
  - `getConnection(username, password)` throws `SQLException` with
    `"Refreshing JDBC password data source does not accept caller-supplied credentials."`
    and never includes supplied username, supplied password, URL, or properties.
  - `getLogWriter`, `setLogWriter`, `getLoginTimeout`, and `setLoginTimeout`
    delegate to `DriverManager`; KDoc and README must say these methods read
    or mutate process-wide `DriverManager` state, not per-instance state.
  - `toString()` reports only class name, sanitized URL summary, and username.
    It must not include full JDBC URLs, URL query strings, URL userinfo, the
    current password, or any password-like values.
  - `getParentLogger()` returns `Logger.getGlobal()` as the current AWS class
    does.
  - `unwrap` and `isWrapperFor` support unwrapping to the helper instance only.
  - The DataSource and provider do not implement `Serializable`; only the
    immutable config value object does.

AWS migration after the helper is released:

- Remove the internal `RdsIamRefreshingDataSource` class from
  `AwsJdbcDataSourceFactory.kt`.
- Replace it with `RefreshingJdbcPasswordDataSource`, adapting
  `AwsDatabasePasswordProvider.currentPassword()?.reveal()` to the generic
  `String?` provider.
- Keep the AWS-specific null-token message:
  `"RDS IAM password provider returned null."`
- AWS users see no configuration or public API change after the
  `aws-exposed` upgrade. RDS IAM mode must keep the Hikari `dataSource` path
  and must not set Hikari `jdbcUrl`, `username`, or `password` directly for
  RDS IAM.
- RDS IAM mode must not set `HikariConfig.username`,
  `HikariConfig.password`, `HikariConfig.credentialsProvider`,
  `HikariConfig.jdbcUrl`, or `HikariConfig.dataSourceClassName`; setting any of
  these can make Hikari call `DataSource.getConnection(username, password)` or
  bypass the refresh-aware DataSource path. The only allowed Hikari connection
  source for RDS IAM is `dataSource = RefreshingJdbcPasswordDataSource(...)`.
- Keep `AwsDatabasePasswordProvider` and `AwsDatabasePasswordProviders` in
  `aws-exposed`; only the JDBC connection wrapper moves.

## Behavior

- `passwordProvider.currentPassword()` is called once per no-arg
  `getConnection()` invocation.
- Connection pools call the helper only when they open a physical connection.
  Reusing an already-open physical connection from a pool must not trigger a
  password lookup.
- `passwordProvider.currentPassword()` is never called from `toString()`,
  wrapper inspection, log-writer methods, or the rejected credential overload.
- The AWS adapter reveals `AwsSecretString` before calling the generic helper.
  `bluetape4k-jdbc` may hold the resulting raw password string only long
  enough to construct the per-call JDBC `Properties` passed to
  `DriverManager`; it must not log, cache, store, expose, or include that
  password in diagnostics.
- A fresh `Properties(dataSourceProperties.size + 2)` instance, or an
  equivalent pre-sized fresh instance, is created for each call. The helper
  must not pass a shared or defaults-backed `Properties` object to JDBC drivers.
- Base `dataSourceProperties` are copied before setting `user` and `password`.
  The generated `user` and `password` values intentionally override same-named
  base entries to keep the constructor's `username` and current password
  authoritative.
- The provider may return a cached token, a newly generated token, or null.
  Null results in `SQLException` with the configured `nullPasswordMessage`.
- The helper does not cache passwords, schedule refreshes, manage pools, or
  close connections. Hikari remains the pool owner in `aws-exposed`.
- The helper does not implement `Closeable` or `AutoCloseable` and owns no
  resources. Returned `Connection` lifecycle belongs to the caller or pool;
  `JdbcPasswordProvider`, JDBC driver, and AWS SDK/RDS utilities lifecycle
  remain caller-owned.
- Failure messages must not include password values or caller-supplied
  credentials. Null password uses `config.nullPasswordMessage`; rejected
  caller credentials use the stable message above; unwrap failures use
  `"Not a wrapper for <fqcn>."`; driver class loading failures may include the
  driver class name but not credentials.
- `nullPasswordMessage` must be a static, secret-free diagnostic message. It
  must not interpolate provider return values, URL query values,
  `dataSourceProperties`, or caller-supplied credential values.
- Null password changing from the current AWS internal `requireNotNull` path to
  `SQLException` is an intentional JDBC API correction, not strict exception
  preservation. AWS migration tests must pin the new behavior.
- Exception contract:
  - Config validation failures throw `IllegalArgumentException`.
  - Optional driver class loading uses `Class.forName(driverClassName)` in the
    constructor. `ClassNotFoundException` may propagate directly or be wrapped
    in `IllegalArgumentException` with the driver class name and cause, but the
    message must not include URL, properties, or credentials.
  - Null password throws `SQLException(config.nullPasswordMessage)`.
  - Provider failure is wrapped as a secret-free `SQLException` with cause, so
    `DataSource.getConnection()` consistently exposes connection acquisition
    failure as `SQLException`.
  - `DriverManager.getConnection(...)` `SQLException` is propagated unchanged.
  - Rejected caller credentials throw the stable `SQLException` message above.
  - `unwrap` failure throws `SQLException("Not a wrapper for <fqcn>.")`.
- `DriverManager` log writer and login timeout methods are process-global.
  Tests that touch these methods must save and restore previous values, and
  provider/property hot-path tests must not use these global methods as part of
  their measurement or assertions.

## Tests

`bluetape4k-projects:data/jdbc` tests:

- `getConnection()` opens an H2 connection and calls the password provider.
- repeated `getConnection()` calls invoke the provider for each physical
  connection.
- caller-supplied credential overload throws `SQLException`.
- null password throws `SQLException` with the configured message.
- base properties are copied per call and cannot leak mutations across
  invocations.
- concurrent `getConnection()` calls are covered with
  `MultithreadingTester` and a fake registered `Driver`; the test asserts the
  provider count equals physical connection attempts, every call receives a
  distinct `Properties`, and driver-side mutations do not leak between calls.
- a fake registered `Driver` captures received `Properties`; tests assert base
  properties containing stale `user` and `password` are overridden by the
  constructor username and provider password.
- fake-driver tests deregister the driver and restore any touched
  `DriverManager` global state after execution.
- wrapper methods report `isWrapperFor` and `unwrap` behavior.
- `toString()` does not invoke the provider and does not expose password
  values, full URL query strings, URL userinfo, or credential-bearing JDBC URL
  parts. Sentinel tests include URL query parameters such as `password`,
  `token`, and `sslpassword`.
- failure messages for null password, rejected caller credentials, unwrap
  failure, and driver loading do not expose password values.
- null-password tests use sentinel URL parameters and secret-bearing
  `dataSourceProperties` to prove `nullPasswordMessage` remains static and
  secret-free.

`bluetape4k-aws` tests after consumption:

- existing RDS IAM DataSource test still opens H2 with provider token.
- add a Hikari boundary test that proves per-physical-connection refresh by
  holding connection #1 while opening connection #2 with `maximumPoolSize = 2`,
  then asserting the provider count reaches 2. Do not treat two sequential
  logical borrows as evidence because Hikari may reuse one physical connection.
- add a logical-borrow reuse assertion when practical: returning and borrowing
  the same pooled physical connection should not force another token lookup.
- assert RDS IAM mode still configures Hikari through `dataSource` and does not
  set Hikari `jdbcUrl`, `username`, `password`, `credentialsProvider`,
  or `dataSourceClassName` directly.
- run the AWS Hikari boundary test through `HikariAwsJdbcDataSourceFactory`,
  not only by instantiating the generic DataSource directly.
- add an explicit caller-supplied credential rejection assertion if it is not
  already covered by the generic helper tests.
- existing redaction tests remain in AWS because `bluetape4k-jdbc` only sees
  revealed JDBC password strings at the driver boundary.

## Documentation Requirements

- `data/jdbc/README.md` and `data/jdbc/README.ko.md` must include
  source-equivalent sections for the refresh-aware `DataSource` helper.
- README examples must include imports, a runnable-style Kotlin snippet using
  `RefreshingJdbcPasswordDataSourceConfig`, and a Hikari wrapping example that
  sets `dataSource = RefreshingJdbcPasswordDataSource(...)`.
- README/KDoc must warn that `getConnection(username, password)` is rejected,
  and that `getLogWriter` / `setLogWriter` / `getLoginTimeout` /
  `setLoginTimeout` use process-wide `DriverManager` state.
- README/KDoc must list unsupported cases: this helper is not a connection
  pool, scheduled refresh service, async password provider, generic static
  credential helper, or caller-supplied credential override path.
- README/KDoc must say `dataSourceProperties` may contain vendor driver
  options, but secret-bearing entries are not diagnostic-safe; `user` and
  `password` entries are overwritten by the constructor username and current
  provider password.
- README/KDoc examples must import from `io.bluetape4k.jdbc.datasource`.
- New public KDoc for this helper must be English. Existing Korean KDoc in
  unrelated `data/jdbc` files is not part of this cleanup.

## Publication Gate

AWS consumption is a separate downstream step and remains blocked until one of
these artifact paths is available and verified:

- Snapshot validation path: publish `io.github.bluetape4k:bluetape4k-jdbc:1.11.0-SNAPSHOT`
  from the upstream PR/ref, update AWS `gradle/libs.versions.toml` to
  `bluetape4k = "1.11.0-SNAPSHOT"` only for the pre-release validation PR, and
  prove dependency resolution from
  `https://central.sonatype.com/repository/maven-snapshots/`.
- Stable release path: publish a forward version such as `1.11.1` or the next
  explicitly approved release train, update AWS to that stable version, and
  verify resolution from Maven Central release repositories.

Do not edit AWS to consume an unreleased helper unless the selected snapshot or
stable artifact path has already been published and resolved in a clean build.
AWS `0.5.0` migration must update the catalog coordinate first, keep the
Central snapshots repository only for the snapshot path, run dependency
resolution evidence, run clean AWS CI, and only then remove the internal
wrapper.

## Rollback

- If AWS snapshot validation fails, revert the AWS catalog coordinate and keep
  the internal `RdsIamRefreshingDataSource` until a corrected snapshot is
  published and resolved.
- If a published stable helper is wrong, use a forward-only patch release and,
  when needed, deprecate the flawed API. Do not delete or rewrite stable
  artifacts.
- If the upstream `bluetape4k-projects` PR fails CI or review, no AWS change is
  attempted.

## Risks And Mitigations

| Risk | Mitigation |
|---|---|
| AWS cannot consume the helper until publication. | Split delivery into upstream `bluetape4k-projects` PR first, then AWS consumption after a published version or snapshot exists. |
| Generic helper leaks secret values through diagnostics. | Do not include password values in exception messages or `toString()`. Keep redaction tests in AWS value objects. |
| Existing AWS behavior changes around property precedence. | Pin precedence in tests: base properties copied first, `user` and `password` written last. |
| `getConnection(username, password)` ambiguity creates unsafe bypass. | Reject the overload consistently in the generic helper and keep AWS tests for the rejection. |
| Hikari integration changes lifecycle semantics. | Keep Hikari configuration unchanged; only the nested `dataSource` implementation changes after AWS consumes the helper. |

## Acceptance Criteria

- `data/jdbc` exposes a generic refresh-aware `DataSource` API with English
  KDoc.
- `data/jdbc` tests prove per-call password lookup, caller-supplied credential
  rejection, null-password failure, wrapper behavior, and no per-call property
  leakage.
- `data/jdbc/README.md` and `data/jdbc/README.ko.md` mention the new
  refresh-aware `DataSource` helper with runnable-style examples, imports,
  unsupported cases, Hikari wrapping, and process-global `DriverManager`
  method warnings.
- `bluetape4k-projects` targeted compile/tests for `:bluetape4k-jdbc` pass:
  `:bluetape4k-jdbc:test`, `:bluetape4k-jdbc:koverXmlReport`, and
  `git diff --check`.
- `bluetape4k-projects` PR CI includes the `test-data` path that runs
  `:bluetape4k-jdbc:test` and `:bluetape4k-jdbc:koverXmlReport`.
- AWS consumption is explicitly blocked until a consumable `bluetape4k-jdbc`
  version exists; once available, AWS replaces the internal DataSource wrapper
  without changing RDS IAM token provider/redaction semantics.
- AWS consumption verification includes dependency-resolution evidence for the
  selected artifact coordinate and `:bluetape4k-aws-exposed:test`.

## Out Of Scope

- Publishing a stable `bluetape4k-projects` release without an explicit release
  request.
- Changing `AwsDatabasePasswordProvider` caching semantics.
- Adding Hikari-specific token refresh APIs.
- Adding Spring Boot auto-configuration for this helper.
- Changing `bluetape4k-aws` catalog versions before the helper is actually
  available from a repository CI can resolve.
