# Hibernate natural-id KeyType 교훈 (2026-06-26)

관련 이슈: #908
영향 module: `:bluetape4k-hibernate`

## L1: Hibernate 7 natural-id helper는 loader API를 사용해야 한다

### 문제

`Session.findBySimpleNaturalId()`와 `Session.findByNaturalId()`가
`Session.find(..., KeyType.NATURAL)`을 사용했다. module test가 사용하는 Hibernate 7.2
runtime에는 `org.hibernate.KeyType`이 없어서 helper test가 `NoClassDefFoundError`로
실패했다.

### 교훈

natural-id lookup에는 Hibernate natural-id loader API를 사용한다.

- simple natural id: `Session.bySimpleNaturalId(entityClass).load(value)`
- composite natural id: `Session.byNaturalId(entityClass).using(values).load()`

### 향후 가드

helper가 Hibernate-specific API를 감쌀 때는 제거된 compatibility constant나 overload를
사용하기 전에 정확한 `testRuntimeClasspath` Hibernate jar를 기준으로 검증한다.
