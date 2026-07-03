# Data validation fail-fast for public numeric parameters

## Context

Issues #947 and #955 found public data APIs that accepted invalid numeric
parameters and let downstream JDBC modulo operations or SQL/CQL builders expose
late, low-signal failures.

## Decision

Validate numeric inputs at the public entrypoint with bluetape4k `require*`
helpers:

- JDBC batch size must be positive before any batch loop or modulo operation.
- R2DBC query limit must be positive.
- R2DBC query offset must be zero or positive.
- Cassandra keyspace replication factor must be positive.

## Verification

- `./gradlew :bluetape4k-jdbc:test --tests 'io.bluetape4k.jdbc.sql.DataSourceTransactionExtensionsTest'`
- `./gradlew :bluetape4k-r2dbc:test --tests 'io.bluetape4k.r2dbc.query.QueryBuilderTest'`
- `./gradlew :bluetape4k-cassandra:test --tests 'io.bluetape4k.cassandra.CassandraAdminTest'`
- `git diff --check`

## Future guidance

For public data API parameters that shape generated SQL/CQL or loop arithmetic,
validate before delegating to driver builders or iteration logic. Prefer
`requirePositiveNumber` and `requireZeroOrPositiveNumber` over raw `require`
when the parameter is numeric.
