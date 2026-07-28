# 이슈 810: Cassandra bootstrap builder

## 배경

`CqlSessionProvider`는 caller builder block을 적용하기 전에 keyspace bootstrap용 admin
session을 만들었다. 최종 session builder에 올바른 credential, TLS, contact point,
driver option이 있어도 secured cluster에서 실패할 수 있었다.

## 결정

Shared builder block을 bootstrap session과 final session 양쪽에 적용한다. Bootstrap
뒤 provider-managed keyspace를 bind하고, keyspace 생성 전에는 invalid이지만 final
session에는 필요한 setting을 가진 호출자를 위해 명시적인 bootstrap/session builder
overload를 노출한다.

## 결과

Regression test는 bare `CqlSessionBuilder` supplier를 사용하고 contact point와
datacenter는 caller builder block에 의존한다. Bootstrap builder application이 없으면
실패하고 새 동작에서는 통과한다.

## 향후 지침

Administrative side effect를 final-session-only builder 뒤에 숨기지 않는다. Helper가
client/session을 반환하기 전에 DDL을 수행한다면 어떤 builder setting이 admin path에
적용되는지 문서화하고, 일부 setting이 phase-specific이면 별도 hook을 제공한다.
