---
title: 자동 구성 조건과 활성화 순서
description: Hibernate, Metrics, Actuator 자동 구성이 언제 등록되고 물러나는지 1.11.0 조건으로 확인합니다.
manualId: bluetape4k-spring-boot-hibernate-lettuce
chapterId: auto-configuration-conditions
---

# 자동 구성 조건과 활성화 순서

## 세 구성 클래스의 역할

`AutoConfiguration.imports`에는 다음 순서로 세 클래스가 등록됩니다.

1. `LettuceNearCacheHibernateAutoConfiguration`
2. `LettuceNearCacheMetricsAutoConfiguration`
3. `LettuceNearCacheActuatorAutoConfiguration`

첫 클래스는 `HibernatePropertiesCustomizer`를 만들고, 나머지 둘은 Hibernate 설정 뒤에 관측 기능을 붙입니다. Metrics와 Actuator 구성은 Spring Boot의 JPA 자동 구성 뒤에 실행되도록 `after` 관계도 선언합니다.

## Hibernate 구성 조건

Hibernate 구성에는 두 가지 조건이 있습니다.

- classpath에 `LettuceNearCacheRegionFactory`와 `EntityManagerFactory`가 있어야 합니다.
- `bluetape4k.cache.lettuce-near.enabled`가 `true`이거나 생략되어야 합니다.

`EntityManagerFactory` bean을 기다리는 조건은 없습니다. 이 구성은 factory가 만들어지기 전에 Hibernate property를 보태야 하기 때문입니다. `enabled=false`이면 customizer 자체가 등록되지 않는다는 사실을 `ApplicationContextRunner` 테스트가 확인합니다.

```yaml
bluetape4k:
  cache:
    lettuce-near:
      enabled: false
```

## Metrics 구성 조건

Metrics binder는 `LettuceNearCacheRegionFactory`, `EntityManagerFactory`, `MeterRegistry` 클래스가 모두 있어야 하며, 실제 `EntityManagerFactory`와 `MeterRegistry` bean도 필요합니다. 여기에 `bluetape4k.cache.lettuce-near.metrics.enabled=true` 조건이 붙습니다. 이 값은 생략해도 true로 간주합니다.

Actuator starter가 없거나 애플리케이션이 `MeterRegistry`를 만들지 않으면 cache 자체는 활성화되더라도 binder는 생기지 않습니다. 이는 cache 실패가 아니라 선택 기능의 back-off입니다.

## Actuator 구성 조건

Endpoint 구성은 `Endpoint`, RegionFactory와 `EntityManagerFactory` 클래스, 실제 `EntityManagerFactory` bean, 그리고 전체 `enabled` 설정을 확인합니다. metrics 설정은 보지 않습니다. 따라서 metrics를 끈 상태에서도 endpoint bean은 등록될 수 있고, Hibernate statistics가 꺼져 있으면 L2 통계 필드가 `null`일 수 있습니다.

HTTP 노출은 bean 등록과 별개입니다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,nearcache
```

## 조건을 진단하는 순서

1. Spring Boot condition evaluation report에서 세 auto-configuration 이름을 찾습니다.
2. 전체 `enabled`와 metrics `enabled`를 구분해 확인합니다.
3. `spring-boot-hibernate`, JPA starter, Actuator가 runtime classpath에 있는지 봅니다.
4. `EntityManagerFactory`와 `MeterRegistry` bean 존재 여부를 확인합니다.
5. bean이 있어도 endpoint가 HTTP에 안 보이면 management exposure를 확인합니다.

## 실행 근거

- [`AutoConfiguration.imports`](../../../../../spring-boot/hibernate-lettuce/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
- [`LettuceNearCacheHibernateAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt)
- [`LettuceNearCacheMetricsAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsAutoConfiguration.kt)
- [`LettuceNearCacheActuatorAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorAutoConfiguration.kt)
- [`LettuceNearCacheAutoConfigurationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt)
