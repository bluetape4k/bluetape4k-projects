# 교훈: R2DBC SQL identifier guard (2026-06-26)

**이슈**: #822
**모듈**: `:bluetape4k-r2dbc`

## L1: helper coverage만으로 public DSL 안전성을 보장할 수 없다

### 문제

`requireValidIdentifier(...)`는 이미 존재했고 unit test도 있었지만, public
insert/update builder는 해당 helper를 호출하기 전에 field name을 저장했다.
`QueryBuilder.whereGroup(...)`도 임의의 non-blank operator를 받아 condition
사이에 보간했다.

### 교훈

SQL DSL에 validation helper가 있으면, helper 자체뿐 아니라 identifier나 operator를
보간하는 public builder 경로를 테스트해야 한다. Helper-level test는 predicate를
증명하지만, public-path test는 guard가 실제 DSL에 연결되어 있는지 증명한다.

### 향후 방지책

R2DBC SQL DSL 변경에서는 문자열 인자가 bound value가 아니라 SQL syntax가 되는
fluent API entrypoint를 직접 겨냥한 회귀 테스트를 추가한다.
