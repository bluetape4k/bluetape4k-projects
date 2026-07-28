# Public numeric parameter의 data validation fail-fast

## 배경

이슈 #947과 #955는 public data API가 잘못된 numeric parameter를 받아들이고,
downstream JDBC modulo operation이나 SQL/CQL builder가 늦고 신호가 약한 실패를
드러내게 한다는 점을 확인했다.

## 결정

Numeric input은 public entrypoint에서 bluetape4k `require*` helper로 검증한다.

- JDBC batch size는 batch loop나 modulo operation 전에 positive여야 한다.
- R2DBC query limit은 positive여야 한다.
- R2DBC query offset은 zero or positive여야 한다.
- Cassandra keyspace replication factor는 positive여야 한다.

## 검증

- `./gradlew :bluetape4k-jdbc:test --tests 'io.bluetape4k.jdbc.sql.DataSourceTransactionExtensionsTest'`
- `./gradlew :bluetape4k-r2dbc:test --tests 'io.bluetape4k.r2dbc.query.QueryBuilderTest'`
- `./gradlew :bluetape4k-cassandra:test --tests 'io.bluetape4k.cassandra.CassandraAdminTest'`
- `git diff --check`

## 향후 지침

Generated SQL/CQL이나 loop arithmetic을 형성하는 public data API parameter는 driver
builder나 iteration logic에 위임하기 전에 검증한다. Parameter가 numeric이면 raw
`require`보다 `requirePositiveNumber`와 `requireZeroOrPositiveNumber`를 우선한다.
