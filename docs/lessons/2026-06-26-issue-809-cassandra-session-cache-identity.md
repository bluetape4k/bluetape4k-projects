# 이슈 809 - Cassandra session cache identity

## 배경

`CqlSessionProvider`는 keyspace name만으로 session을 cache했다. 같은 keyspace name을
tenant, endpoint, credential, client setting이 다른 context에서 사용하면 process가 첫 번째
session을 조용히 재사용할 수 있었다.

## 결정

cache key로 `CqlSessionIdentity`를 사용한다. compatibility overload는 builder supplier와
builder lambda에서 보수적인 per-call identity를 만들어, 다른 builder block이 keyspace만으로
충돌하지 않게 한다. call site 간 안정적인 same-context reuse가 필요하면 caller가 명시적
`CqlSessionIdentity` overload를 사용해야 한다.

## 결과

regression test는 같은 identity가 session을 재사용하고, 같은 keyspace라도 connection
context가 다르면 이전 session을 재사용하지 않음을 증명한다. README example은 explicit
identity를 언제 써야 하는지 문서화한다.

## 향후 지침

connection, credential, tenant context가 달라질 수 있으면 infrastructure client를 logical
namespace만으로 cache하지 않는다. implicit builder state에 기대기 전에 explicit identity
object를 추가한다.
