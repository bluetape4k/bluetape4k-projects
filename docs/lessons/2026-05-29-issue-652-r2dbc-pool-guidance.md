# 이슈 652 R2DBC pool guidance

## 배경

bounded pending acquire 때문에 overload가 빠르게 실패하면 R2DBC pool benchmark score가
오해를 줄 수 있다. 높은 JMH operation score는 성공한 SQL work가 아니라 거절된 acquire
attempt를 의미할 수 있다.

## 결정

pool contention benchmark에서는 throughput 옆에 acquired count와 failed count를 함께
보고한다. benchmark evidence와 compatibility review가 runtime contract 변경을
정당화하지 않는 한 production default는 변경하지 않는다.

## 결과

H2, PostgreSQL, MySQL acquire benchmark는 순차적으로 완료됐다. H2 contention은 이제
default profile과 high-throughput profile을 비교하고, bounded queue overload를 failure
count로 보이게 한다.

## 검증

- `./gradlew :bluetape4k-r2dbc:compileBenchmarkKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-r2dbc:benchmarkH2PoolContention --no-configuration-cache --quiet`
- `./gradlew :bluetape4k-r2dbc:benchmarkPoolConfig --no-configuration-cache --quiet`
- `./gradlew :bluetape4k-r2dbc:benchmarkH2PoolAcquire --no-configuration-cache --quiet`
- `./gradlew :bluetape4k-r2dbc:benchmarkPostgresPoolAcquire --no-configuration-cache --quiet`
- `./gradlew :bluetape4k-r2dbc:benchmarkMysql8PoolAcquire --no-configuration-cache --quiet`

## 향후 지침

Testcontainers-backed R2DBC benchmark는 순차 실행한다. benchmark가 bounded pending
acquire를 가진다면 reader가 빠른 reject를 완료된 database throughput으로 해석하지
않도록 JMH score와 함께 success/failure count를 publish한다.
