# 이슈 839 - Hibernate Lettuce demo README dependency

## 배경

`spring-boot/hibernate-lettuce-demo` README dependency example은
`Libs.springBootStarter(...)` 같은 bluetape4k 내부 Gradle helper를 사용했다. 이 helper는
consumer build에서 사용할 수 없으므로, 복사한 example은 demo를 실행하기 전에 실패한다.

## 결정

consumer 관점에서 demo dependency를 문서화한다.

- `platform(...)`으로 `org.springframework.boot:spring-boot-dependencies`를 import한다.
- published coordinate인
  `io.github.bluetape4k:bluetape4k-spring-boot-hibernate-lettuce`를 사용한다.
- 표준 Spring Boot starter와 H2 coordinate를 사용한다.
- `spring-boot-starter-data-jpa`가 demo runtime을 제공하므로 stale explicit
  `compileOnly` Hibernate helper note를 제거한다.

## 후속 가드

`ReadmeDependencyContractTest`는 양쪽 README locale을 읽고 internal dependency helper가
consumer-facing snippet에 다시 들어오면 실패한다.
