# 이슈 #813 Stateless session resource binding

## 배경

`StatelessSessionFactoryBean`은 provider가 만든 `StatelessSession` instance를
`SessionFactory` 자체를 key로 사용해 Spring transaction resource map에 저장했다.
Spring JPA transaction infrastructure도 자체 resource binding에 factory key를
사용하므로 stateless proxy path가 active `EntityManager` lifecycle과 충돌할 수
있었다.

## 결정

Stateless session에는 전용 transaction resource key를 사용한다. 이 key는 정확한
`SessionFactory` identity에 묶이지만 별도 key type을 가지므로 Spring의 일반
`SessionFactory` 또는 `EntityManager` resource와 충돌할 수 없다.

## 결과

- 주입된 `StatelessSession` proxy는 하나의 transaction 안에서 같은 stateless session을 재사용한다.
- Proxy는 더 이상 raw `SessionFactory` key 아래에 `StatelessSession`을 bind하지 않는다.
- Factory가 만든 정확한 resource는 transaction 완료 시 unbind되고 닫힌다.
- Active transaction 밖의 호출은 계속 fail fast한다.

## 향후 규칙

Custom resource를 Spring `TransactionSynchronizationManager`와 통합할 때는
framework-owned resource key를 다른 lifecycle participant에 재사용하지 않는다.
전용 key type을 사용하고 same-transaction reuse와 after-completion cleanup을 모두
테스트한다.
