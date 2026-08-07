---
manualId: bluetape4k-spring-boot-hibernate-lettuce
title: "Spring Boot Hibernate 캐시 통합"
description: "Spring Boot 4에서 Hibernate 2차 캐시용 Lettuce Near Cache를 자동 구성하는 조건, 설정, 수명주기와 운영 방법을 설명합니다."
kind: library
group: spring
learningOrder: 950
---

# Spring Boot Hibernate 캐시 통합

## 제공하는 기능 {#problem}

`bluetape4k-spring-boot-hibernate-lettuce`는 Spring Boot 4 애플리케이션에서 Hibernate 2차 캐시를 `LettuceNearCacheRegionFactory`로 연결합니다. `application.yml`의 설정을 Hibernate property로 옮기고, 조건이 맞으면 Actuator endpoint와 Micrometer gauge도 등록합니다.

이 모듈은 캐시 자체를 다시 구현하지 않습니다. Caffeine L1과 Redis L2의 저장·무효화 동작은 [`bluetape4k-hibernate-cache-lettuce`](./bluetape4k-hibernate-cache-lettuce.md)가 맡고, 이 모듈은 Spring Boot의 property binding과 auto-configuration 경계를 담당합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- 애플리케이션이 Spring Boot 4와 Hibernate ORM을 사용하는지 확인합니다.
- Hibernate 2차 캐시의 일관성 전략과 캐시할 entity·collection을 정합니다.
- Redis 장애를 애플리케이션 장애로 볼지, cache miss로 우회할지 하위 cache provider의 계약을 검토합니다.
- Redis 연결 정보, codec, L1 크기, L1 만료와 Redis TTL의 운영 값을 정합니다.
- Actuator endpoint를 외부에 노출할지, Micrometer 통계를 수집할지 결정합니다.

Spring을 쓰지 않는 Hibernate 애플리케이션은 하위 [`bluetape4k-hibernate-cache-lettuce`](./bluetape4k-hibernate-cache-lettuce.md)를 직접 구성합니다. ORM이 필요하지 않은 일반 Near Cache라면 `bluetape4k-cache-lettuce`가 더 작은 선택입니다.

## 의존성 추가 {#coordinates}

사용자는 `bluetape4k-dependencies` 버전만 관리합니다. Spring Boot, Hibernate와 하위 bluetape4k 모듈 버전을 따로 적지 않습니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))

    implementation("io.github.bluetape4k:bluetape4k-spring-boot-hibernate-lettuce")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-hibernate")

    runtimeOnly("org.postgresql:postgresql") // 사용하는 driver로 교체
}
```

Actuator와 Micrometer 연동이 필요하면 `spring-boot-starter-actuator`를 추가합니다. 이 모듈의 Spring Boot·Hibernate·Micrometer 의존성은 대부분 `compileOnly`이므로, 실제 기능을 쓰는 애플리케이션이 필요한 starter를 제공해야 합니다.

## 첫 자동 구성 {#quick-start}

```yaml
bluetape4k:
  cache:
    lettuce-near:
      redis-uri: redis://localhost:6379
      local:
        max-size: 10000
        expire-after-write: 30m
      redis-ttl:
        default: 120s
      metrics:
        enabled: true
        enable-caffeine-stats: true
```

캐시할 entity에는 JPA `@Cacheable`과 Hibernate `@Cache`를 명시합니다.

```kotlin
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "product")
class Product(
    @Id @GeneratedValue
    var id: Long? = null,
    var name: String = "",
)
```

설정이 활성화되면 `HibernatePropertiesCustomizer`가 RegionFactory, 2차 캐시, Redis와 L1 설정을 Hibernate에 전달합니다. entity에 캐시 annotation이 없으면 모듈을 추가했더라도 해당 entity가 자동으로 캐시되지는 않습니다.

## 작업별 안내 {#api-by-task}

| 필요한 작업 | 시작할 구성 요소 | 확인할 경계 |
| --- | --- | --- |
| Hibernate 2차 캐시 연결 | `LettuceNearCacheHibernateAutoConfiguration` | classpath와 `enabled` 조건, property mapping |
| Spring 설정 정의 | `LettuceNearCacheSpringProperties` | 기본값, duration, region별 TTL |
| 전체·개별 region 조회 | `LettuceNearCacheActuatorEndpoint` | endpoint 노출 정책과 null 통계 |
| aggregate gauge 등록 | `LettuceNearCacheMetricsBinder` | `MeterRegistry` 존재와 등록 실패 처리 |
| auto-configuration 탐색 | `AutoConfiguration.imports` | 세 구성 클래스의 등록 순서와 조건 |
| 실제 캐시 저장·무효화 | `LettuceNearCacheRegionFactory` | Hibernate·Redis resource 수명주기 |

## 학습 경로 {#concepts}

각 장은 개념 설명, 설정 예제, 잘못 쓰기 쉬운 지점과 1.12.1 배포 소스·테스트를 함께 제공합니다.

1. [자동 구성 조건과 활성화 순서](./bluetape4k-spring-boot-hibernate-lettuce/auto-configuration-conditions.md) — 세 auto-configuration이 언제 등록되고 물러나는지 확인합니다.
2. [설정과 Hibernate property 매핑](./bluetape4k-spring-boot-hibernate-lettuce/properties-and-hibernate-mapping.md) — 기본값, duration과 region별 TTL 변환을 다룹니다.
3. [캐시 수명주기와 소유권](./bluetape4k-spring-boot-hibernate-lettuce/cache-lifecycle-ownership.md) — Spring customizer, Hibernate RegionFactory, Redis 자원의 책임을 구분합니다.
4. [Actuator와 Micrometer 관측](./bluetape4k-spring-boot-hibernate-lettuce/observability-actuator-metrics.md) — endpoint 결과와 두 aggregate gauge의 의미를 설명합니다.
5. [테스트와 실패 동작](./bluetape4k-spring-boot-hibernate-lettuce/testing-and-failure-modes.md) — context test, Redis 통합 test와 조용히 비활성화되는 경로를 확인합니다.
6. [생태계에서 다음 선택](./bluetape4k-spring-boot-hibernate-lettuce/ecosystem-paths.md) — 하위 cache 모듈, demo, Hibernate·Redis 공식 문서로 이어집니다.

처음 도입한다면 1→2→3 순서로 읽습니다. 운영 대시보드와 장애 대응을 준비할 때는 4장과 5장을 함께 확인합니다.

## 권장 패턴 {#patterns}

auto-configuration은 애플리케이션 설정을 Hibernate에 전달하는 adapter로 봅니다. entity별 concurrency strategy와 region 이름은 domain·조회 패턴에 맞춰 명시하고, L1 만료는 Redis TTL보다 짧거나 같게 설계해 오래된 local entry가 예상보다 오래 남지 않도록 합니다.

설정값을 한 번에 크게 잡지 않습니다. region별 hit·miss, local size와 eviction을 관찰하면서 L1 크기와 TTL을 조정합니다. cache가 database의 정합성 규칙을 대신한다고 가정하지 말고, transaction commit과 invalidation 시점을 실제 통합 테스트로 확인합니다.

## 연동 {#integrations}

이 모듈은 `bluetape4k-hibernate-cache-lettuce`를 API dependency로 제공하고, Spring Boot 4의 auto-configuration·Hibernate integration·JPA starter, Hibernate ORM, Micrometer와 Actuator를 선택적으로 연동합니다. 직렬화에는 Fory와 Zstd runtime이 포함됩니다.

Spring Boot 4에서는 `HibernatePropertiesCustomizer` 패키지가 `org.springframework.boot.hibernate.autoconfigure`입니다. Spring Boot 3 문서의 이전 패키지나 없어진 모듈 경로를 복사하지 않습니다.

## 설정 {#configuration}

주요 기본값은 `enabled=true`, `redis-uri=redis://localhost:6379`, `codec=lz4fory`, `use-resp3=true`, `local.max-size=10000`, `local.expire-after-write=30m`, `redis-ttl.default=120s`입니다. metrics와 Caffeine stats도 기본으로 활성화됩니다.

초 단위로 나누어떨어지는 duration은 `120s`처럼, 나머지는 `500ms`처럼 Hibernate property로 전달됩니다. 점이 들어간 region 이름은 Spring 설정에서 대괄호 map key로 적습니다.

```yaml
redis-ttl:
  regions:
    "[com.example.Product]": 300s
```

전체 표와 변환 규칙은 [설정 장](./bluetape4k-spring-boot-hibernate-lettuce/properties-and-hibernate-mapping.md)에 정리했습니다.

## 실패 동작 {#failures}

`enabled=false`이면 Hibernate customizer와 Actuator endpoint가 등록되지 않습니다. metrics만 끄면 binder와 Hibernate statistics 설정은 빠지지만, 전체 캐시 auto-configuration과 endpoint 조건은 그대로 평가됩니다.

Actuator endpoint는 `EntityManagerFactory` unwrap, RegionFactory 조회 또는 통계 조회가 실패하면 예외를 외부로 던지지 않고 빈 map·`null` 또는 일부 `null` 필드를 반환합니다. metrics binder도 등록 실패를 warning으로 남기고 애플리케이션 시작을 계속합니다. 이 동작은 관측 기능의 실패가 cache 정상 여부를 증명하지 못한다는 뜻이므로, Redis와 실제 entity 조회 상태를 별도로 감시해야 합니다.

## 운영 {#operations}

Redis 연결·재연결, L1 hit/miss/eviction, Hibernate L2 hit/miss/put, region 수, local entry 수와 database query latency를 함께 봅니다. `/actuator/nearcache`는 Spring Boot exposure 설정에 추가해야 HTTP로 접근할 수 있습니다. endpoint에는 cache key와 값이 아니라 region 통계만 노출되지만, 운영망 접근 제어는 별도로 적용합니다.

종료 시 Redis client를 애플리케이션에서 중복으로 닫지 않습니다. 실제 client와 cache instance는 Hibernate RegionFactory가 만들고 닫으므로 `SessionFactory` 종료 순서에 맡깁니다.

## 테스트 {#testing}

`ApplicationContextRunner` 테스트는 Redis나 database 없이 condition과 property mapping을 검증합니다. 통합 테스트는 Redis Testcontainer와 H2를 사용해 miss→put→hit, endpoint, gauge와 병렬 조회를 확인합니다.

```bash
./gradlew :bluetape4k-spring-boot-hibernate-lettuce:test --no-build-cache --no-configuration-cache
```

Testcontainers가 필요한 전체 test는 다른 database·container test와 겹치지 않게 순차 실행합니다.

## 예제와 실습 {#workshops}

전용 workshop은 등록되지 않았지만 [`bluetape4k-spring-boot-hibernate-lettuce-demo`](./bluetape4k-spring-boot-hibernate-lettuce-demo.md)가 실행 가능한 애플리케이션 예제를 제공합니다. Product CRUD, cache 통계 endpoint와 `application.yml` 구성을 함께 볼 수 있습니다.

작은 단위로는 `LettuceNearCacheAutoConfigurationTest`에서 property 하나씩 바꿔 보고, 그다음 통합 테스트에서 실제 Hibernate statistics 변화를 확인하는 순서가 좋습니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 배포 commit의 소스와 테스트를 기준으로 합니다. 모듈은 Spring Boot 4 전용이며 Spring Boot 3 package·auto-configuration 경로를 지원하지 않습니다.

제공되는 gauge는 활성 region 수와 전체 local entry 수 두 개뿐입니다. region별 L1/L2 통계는 Actuator endpoint 또는 Hibernate statistics에서 읽습니다. endpoint와 binder는 관측 실패를 완화하므로, 값이 비거나 gauge가 없다는 사실만으로 cache backend 원인을 단정할 수 없습니다.

## Source와 tests {#sources}

- [`LettuceNearCacheHibernateAutoConfiguration.kt`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt)
- [`LettuceNearCacheSpringProperties.kt`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheSpringProperties.kt)
- [`LettuceNearCacheActuatorAutoConfiguration.kt`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorAutoConfiguration.kt)
- [`LettuceNearCacheActuatorEndpoint.kt`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorEndpoint.kt)
- [`LettuceNearCacheMetricsAutoConfiguration.kt`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsAutoConfiguration.kt)
- [`LettuceNearCacheMetricsBinder.kt`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsBinder.kt)
- [`LettuceNearCacheAutoConfigurationTest.kt`](../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt)
- [`LettuceNearCacheIntegrationTest.kt`](../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheIntegrationTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Spring Boot Hibernate Lettuce 클래스 구조도

[![Spring Boot Hibernate Lettuce 클래스 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-hibernate-lettuce-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-hibernate-lettuce-diagram-01.svg)

_배포본 README: [`spring-boot/hibernate-lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/hibernate-lettuce/README.ko.md)_

### Spring Boot Hibernate Lettuce auto-configuration 처리 흐름

[![Spring Boot Hibernate Lettuce auto-configuration 처리 흐름](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-hibernate-lettuce-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-hibernate-lettuce-diagram-02.svg)

_배포본 README: [`spring-boot/hibernate-lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/hibernate-lettuce/README.ko.md)_

<!-- release-readme-diagrams:end -->
