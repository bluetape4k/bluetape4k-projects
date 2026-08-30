---
title: 자동 구성 조건과 활성화 순서
description: 2.0의 root, metrics, registry, Actuator, exposure 조건이 각 Hibernate-Lettuce 자동 구성 단계에 어떻게 적용되는지 설명합니다.
manualId: bluetape4k-spring-boot-hibernate-lettuce
chapterId: auto-configuration-conditions
---

# 자동 구성 조건과 활성화 순서

> 계약 범위: `develop`의 **2.0.0 current contract**입니다. 안정 릴리스 rollback
> 기준은 [1.12.1](https://github.com/bluetape4k/bluetape4k-projects/releases/tag/1.12.1)입니다.

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

Metrics binder는 `LettuceNearCacheRegionFactory`, `EntityManagerFactory`,
`MeterRegistry` 클래스가 모두 있어야 하며, 실제 `EntityManagerFactory`와
`MeterRegistry` Bean도 필요합니다. `bluetape4k.cache.lettuce-near.enabled`와
`bluetape4k.cache.lettuce-near.metrics.enabled`가 모두 `true`이거나 생략돼야
합니다. root 설정을 끄면 metrics 설정만으로 binder를 다시 활성화할 수 없습니다.

애플리케이션이 `MeterRegistry`를 만들지 않으면 cache 자체는 활성화되더라도
binder는 생기지 않습니다. 이는 cache 실패가 아니라 선택 기능의 back-off입니다.
Actuator 의존성은 별도로 endpoint 등록 여부를 제어합니다.

## Actuator 구성 조건

Endpoint 구성은 `Endpoint`, RegionFactory, `EntityManagerFactory` 클래스와 실제
`EntityManagerFactory` Bean을 요구합니다. binder와 같은 root/metrics property
조건을 적용하지만 `MeterRegistry` Bean은 요구하지 않습니다. 둘 중 한 property라도
끄면 endpoint Bean이 등록되지 않습니다.

HTTP 노출은 Bean 등록과 별개입니다. endpoint Bean이 있어도
`management.endpoints.web.exposure.include`에 `nearcache`를 넣거나 동등한
exposure 규칙을 적용해야 HTTP로 조회할 수 있습니다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,nearcache
```

## 활성화 matrix

아래 matrix는 앞에서 설명한 classpath 조건을 충족한다고 가정합니다.

| Root `enabled` | `metrics.enabled` | `EntityManagerFactory` | `MeterRegistry` | Actuator | 결과 |
| --- | --- | --- | --- | --- | --- |
| `false` | 무관 | 무관 | 무관 | 무관 | customizer, binder, endpoint Bean 모두 없음 |
| `true` | `false` | 있음 | 있거나 없음 | 있거나 없음 | Hibernate customizer만 있음 |
| `true` | `true` | 없음 | 무관 | 무관 | binder와 endpoint Bean 모두 없음 |
| `true` | `true` | 있음 | 없음 | 있음 | endpoint Bean만 있고 Micrometer binder는 없음 |
| `true` | `true` | 있음 | 있음 | 없음 | Metrics binder만 있고 endpoint Bean은 없음 |
| `true` | `true` | 있음 | 있음 | 있음 | binder와 endpoint Bean 모두 있음. HTTP 노출은 별도 설정 |

## 조건을 진단하는 순서

1. Spring Boot condition evaluation report에서 세 auto-configuration 이름을 찾습니다.
2. `bluetape4k.cache.lettuce-near.enabled`를 먼저 확인하고
   `bluetape4k.cache.lettuce-near.metrics.enabled`를 확인합니다.
3. `spring-boot-hibernate`, JPA starter, Actuator가 runtime classpath에 있는지 봅니다.
4. `EntityManagerFactory`와 `MeterRegistry` bean 존재 여부를 확인합니다.
5. endpoint Bean이 있어도 HTTP에서 보이지 않으면
   `management.endpoints.web.exposure.include`를 확인합니다.

## 실행 근거

- [`AutoConfiguration.imports`](../../../../../spring-boot/hibernate-lettuce/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
- [`LettuceNearCacheHibernateAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt)
- [`LettuceNearCacheMetricsAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsAutoConfiguration.kt)
- [`LettuceNearCacheActuatorAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorAutoConfiguration.kt)
- [`LettuceNearCacheAutoConfigurationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt)
