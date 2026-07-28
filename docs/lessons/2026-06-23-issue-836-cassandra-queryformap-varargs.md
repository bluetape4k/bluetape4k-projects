# Cassandra queryForMap varargs 교훈 (#836, 2026-06-23)

관련 이슈: #836
영향 module: `:bluetape4k-spring-boot-cassandra`

## L1: vararg bug report에는 실제 bind marker 증거가 필요하다

issue는 `queryForMap(cql, args)`가 positional bind argument를 펼치지 않고 단일 array
value로 전달한다고 의심했다. 새 unit contract와 두 bind marker를 가진 실제 Cassandra
integration test는 production change 전에도 모두 통과했다. 즉 원래 runtime failure는
local에서 재현되지 않았다.

수정은 public helper를 명시적으로 만들기 위해 `queryForMap(cql, *args)`를 호출했고,
KDoc도 그 contract와 맞췄다. Java varargs 주변 Kotlin wrapper를 바꿀 때는 database
binding bug가 의심된다면 MockK vararg verification과 실제 bind-marker query를 모두
포함해야 한다.

## L2: mock이 오해를 주는 shape를 encoding하지 않게 한다

기존 unit fixture는 `any<Array<*>>()`를 사용해 의심스러운 array-shaped forwarding을
강화했다. 검증된 path에서는 wrapper가 올바르게 동작했는데도 test가 잘못 읽혔다. Java
vararg API test fixture는 public contract처럼 보이도록 `*anyVararg()` 또는 concrete
positional argument를 사용해야 한다.
