# 이슈 837 - Hibernate Lettuce README dependency

## 배경

`spring-boot/hibernate-lettuce` README dependency snippet은
`Libs.springBootStarter(...)`, `Libs.micrometer_core` 같은 repository-local Gradle
helper를 사용했다. 이 example은 consumer project에서 복사해 사용할 수 없다.

## 결정

module dependency를 public Gradle coordinate로 문서화한다.

- `platform(...)`을 통한 `org.springframework.boot:spring-boot-dependencies`
- `io.github.bluetape4k:bluetape4k-spring-boot-hibernate-lettuce`
- Data JPA와 Actuator용 Spring Boot starter coordinate
- `io.micrometer:micrometer-core`
- `HibernatePropertiesCustomizer`가 사용하는 Spring Boot 4 Hibernate integration API용
  `org.springframework.boot:spring-boot-hibernate`

## 후속 가드

`ReadmeDependencyContractTest`는 양쪽 README locale을 읽고, internal Gradle helper
snippet이 consumer-facing dependency example로 돌아오면 실패한다.
