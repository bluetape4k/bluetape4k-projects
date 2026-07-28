# Hibernate Lettuce classpath guard

## 배경

이슈 #945는 Hibernate Lettuce Spring Boot auto-configuration이 optional compile-only
Hibernate, Actuator, Micrometer integration에 direct class reference를 사용한다는
점을 확인했다.

## 결정

Optional integration metadata는 string 기반으로 유지한다.

- optional classpath probe에는 `@ConditionalOnClass(name = [...])`를 사용한다.
- optional bean type에는 `@ConditionalOnBean(type = [...])`를 사용한다.
- optional ordering target에는 `@AutoConfiguration(afterName = [...])`를 사용한다.

## 결과

`FilteredClassLoader` slice test는 Hibernate customizer, Actuator endpoint
annotation, Micrometer registry type이 없을 때 configuration이 깔끔하게 back off함을
증명한다.

## 검증

- `./gradlew :bluetape4k-spring-boot-hibernate-lettuce:test --tests 'io.bluetape4k.spring.boot.autoconfigure.cache.lettuce.LettuceNearCacheAutoConfigurationTest'`

## 향후 지침

Auto-configuration이 `compileOnly` integration을 import한다면 annotation metadata의
class literal을 피하고, integration을 classpath-safe로 표시하기 전에 missing-class
`ApplicationContextRunner` test를 추가한다.
