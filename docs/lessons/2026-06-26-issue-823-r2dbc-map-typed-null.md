# 교훈: R2DBC map typed null binding (2026-06-26)

**이슈**: #823
**모듈**: `:bluetape4k-r2dbc`

## L1: map 기반 null binding에는 명시적인 타입이 필요하다

### 문제

Map helper는 raw null 값을 받아 binding 경계에서 기본 R2DBC type을 임의로 만들었다.
Named/indexed map helper는 `String`을 사용했고, `Update.set(parameters)`는
`setNullable<Any>`를 통해 `Any`를 사용했다.

### 교훈

Raw map null은 typed NULL parameter와 같지 않다. 명시적인 `Parameter` 값은
보존하고 raw null entry는 거부해서, 호출자가 API 경계에서 database type을 제공하게
해야 한다.

### 향후 방지책

Parameter-map helper를 변경할 때는 `bindMap`으로 흘러가는 모든 public surface를
테스트한다. 직접 named binding, 직접 indexed binding, update-map setter, 실행 전에
map entry를 저장하는 insert/update DSL 경로를 모두 포함한다.
