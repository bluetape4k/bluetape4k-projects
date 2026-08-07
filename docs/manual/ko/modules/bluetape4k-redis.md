---
manualId: bluetape4k-redis
title: "Redis 명령 추상화"
description: "Lettuce와 Redisson을 함께 내보내는 Redis 우산 모듈의 의존성 계약, 선택 기준과 마이그레이션 경계를 설명합니다."
kind: library
group: caching
learningOrder: 550
---

# Redis 명령 추상화

## 제공하는 기능 {#problem}

`bluetape4k-redis`는 `bluetape4k-lettuce`와 `bluetape4k-redisson`을 한 좌표로 가져오는 우산 모듈입니다. 자체 Kotlin API를 추가하지 않고 두 하위 모듈을 Gradle `api` 의존성으로 내보냅니다. 기존 애플리케이션이 두 client 계열을 함께 사용하거나, 두 계열 사이에서 점진적으로 이전할 때 의존성 진입점을 하나로 유지할 수 있습니다.

이 이름만 보고 Spring Data Redis, Spring Cache provider나 client 자동 설정까지 포함한다고 해석하면 안 됩니다. 그런 기능은 각각 `bluetape4k-spring-boot-redis`, `bluetape4k-cache-lettuce`, `bluetape4k-cache-redisson`이 담당합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- 한 애플리케이션이 Lettuce와 Redisson API를 실제로 모두 사용하는지 확인합니다.
- Redis command와 coroutine adapter가 중심이면 Lettuce만으로 충분한지 검토합니다.
- 분산 lock·객체·Stream, Redisson Near Cache가 필요하면 Redisson을 선택합니다.
- client lifecycle과 codec wire format을 두 구현 사이에서 공유한다고 가정하지 않습니다.
- Spring Data Redis serializer나 Spring Cache abstraction이 목적이면 별도 모듈에서 시작합니다.
- 마이그레이션 기간이 끝나면 우산 좌표를 필요한 client 한 개로 줄일지 결정합니다.

## 의존성 추가 {#coordinates}

사용자는 Lettuce, Redisson과 개별 bluetape4k 모듈 버전을 맞추지 않고 `bluetape4k-dependencies` BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-redis")
}
```

이 좌표 하나로 두 하위 artifact가 compile/runtime classpath에 들어옵니다. 하나만 사용한다면 [선택 의존성으로 전환](./bluetape4k-redis/selective-dependency-migration.md)해 classpath와 운영 책임을 줄입니다.

## 첫 선택 {#quick-start}

두 구현을 모두 쓰는 전환기에는 우산 모듈을 유지하되 사용 경계를 package나 adapter로 분리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-redis")
}
```

새 코드가 Lettuce만 사용하고 기존 Redisson 호출을 모두 제거했다면 다음 단계에서 좌표를 `bluetape4k-lettuce`로 바꿉니다. 우산 모듈에는 호출할 factory나 초기화 함수가 없으므로 client 생성 예제는 [Lettuce 매뉴얼](./bluetape4k-lettuce.md)과 [Redisson 매뉴얼](./bluetape4k-redisson.md)에서 확인합니다.

## 작업별 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 모듈 | 선택 경계 |
| --- | --- | --- |
| 두 client 계열을 함께 쓰는 전환기 | `bluetape4k-redis` | 우산 모듈은 의존성만 묶습니다. |
| Redis command, pipeline, `RedisFuture`·coroutine | `bluetape4k-lettuce` | client와 cached connection의 종료 주체를 정합니다. |
| 분산 객체·lock·Stream, Redisson transaction | `bluetape4k-redisson` | `RedissonClient.shutdown()`과 Codec을 배포 계약으로 둡니다. |
| 함수 결과·cache-aside abstraction | `bluetape4k-cache-lettuce` 또는 `bluetape4k-cache-redisson` | client helper와 cache provider를 구분합니다. |
| Spring `RedisTemplate` serializer | `bluetape4k-spring-boot-redis` | 우산 모듈이 Spring bean을 만들지 않습니다. |

## 학습 경로 {#concepts}

아래 장은 1.12.1 배포의 실제 Gradle 계약에서 출발합니다. 우산 모듈에 없는 API나 테스트를 설명하는 대신, 하위 모듈을 안전하게 선택하고 줄이는 방법을 구체적인 build 예제와 연결합니다.

1. [우산 의존성 계약](./bluetape4k-redis/umbrella-dependency-contract.md) — `api`로 내보내는 두 artifact와 우산 모듈이 하지 않는 일을 확인합니다.
2. [Lettuce와 Redisson 선택](./bluetape4k-redis/lettuce-vs-redisson.md) — command 중심 client와 분산 객체 중심 client를 요구사항으로 비교합니다.
3. [선택 의존성으로 전환](./bluetape4k-redis/selective-dependency-migration.md) — 사용처를 조사하고 한 client 좌표로 줄이는 순서를 설명합니다.
4. [Lifecycle과 Codec 경계](./bluetape4k-redis/lifecycle-codec-boundaries.md) — client 종료, coroutine, keyspace와 wire format을 구현별로 관리합니다.
5. [Cache provider와 Spring Data Redis 분리](./bluetape4k-redis/cache-spring-separation.md) — client helper, cache abstraction, serializer 모듈을 구분합니다.
6. [테스트·운영·생태계 경로](./bluetape4k-redis/testing-operations-ecosystem.md) — 하위 모듈 테스트와 운영 지표에서 workshop·Exposed cache로 이어갑니다.

처음 도입할 때는 1→2를 읽고 필요한 client만 선택합니다. 이미 우산 좌표를 사용 중이라면 3→4 순서로 실제 사용처와 호환성 경계를 고정한 뒤 의존성을 줄입니다.

## 권장 패턴 {#patterns}

새 애플리케이션은 요구사항이 분명하면 Lettuce 또는 Redisson 좌표를 직접 선택합니다. 두 구현을 함께 쓰는 이유가 있는 경우에도 같은 key를 서로 다른 Codec으로 읽지 않도록 key prefix와 wire format을 분리합니다. client는 request마다 만들지 않고 application lifecycle에 묶어 한 번 생성하고 종료합니다.

우산 모듈은 마이그레이션을 쉽게 시작하게 하지만 완료를 보장하지는 않습니다. 한 구현의 호출이 사라졌는지 source와 runtime classpath에서 확인한 뒤 좌표를 줄입니다.

## 연동 {#integrations}

1.12.1 build가 선언하는 연동은 다음 두 줄뿐입니다.

```kotlin
api(project(":bluetape4k-lettuce"))
api(project(":bluetape4k-redisson"))
```

두 모듈의 transitive·optional runtime 경계는 각 상세 매뉴얼을 따릅니다. 우산 모듈은 Spring Boot starter, Spring Cache provider, database loader/writer를 추가하지 않습니다.

## 설정 {#configuration}

`bluetape4k-redis` 자체에는 설정 class, bean, property나 resource가 없습니다. Lettuce URI·`ClientResources`, Redisson `Config`, timeout·retry·pool, Codec은 실제 client를 소유한 component에서 구성합니다. 두 구현의 설정 이름과 기본값이 같다고 가정하지 않습니다.

## 실패 동작 {#failures}

우산 모듈은 예외를 변환하거나 retry·fallback을 추가하지 않습니다. 연결 실패, timeout, cancellation, Codec decode 오류는 선택한 하위 client의 계약대로 나타납니다. 두 구현을 함께 쓰면 장애 surface도 두 개가 되므로 한쪽 실패를 다른 쪽으로 자동 fallback하지 말고 데이터 일관성과 중복 실행 조건을 먼저 정의합니다.

## 운영 {#operations}

사용 중인 client별 connection 수, reconnect, command latency, timeout, queue와 shutdown을 따로 관찰합니다. 같은 Redis를 쓰더라도 Lettuce와 Redisson pool은 별도입니다. 우산 좌표를 유지하는 동안에는 dependency tree와 client instance 수를 배포 점검 항목에 넣습니다.

## 테스트 {#testing}

우산 모듈에는 production source와 전용 테스트가 없습니다. `:bluetape4k-redis:test`가 존재하더라도 자체 동작을 검증하는 테스트 suite로 해석하면 안 됩니다. 실제 계약은 사용한 하위 모듈과 애플리케이션 통합 테스트에서 검증합니다.

```bash
./gradlew :bluetape4k-lettuce:test --no-build-cache --no-configuration-cache
./gradlew :bluetape4k-redisson:test --no-build-cache --no-configuration-cache
```

두 task는 Redis Testcontainers를 사용하므로 다른 heavy database suite와 병렬 실행하지 않습니다. 문서만 수정할 때는 1.12.1 build·README 링크와 구조 검증으로 충분합니다.

## 워크숍 {#workshops}

우산 모듈 전용 workshop은 없습니다. Lettuce의 `LettuceClientsTest`, `RedisFutureSupportTest`와 Redisson의 `RedissonClientSupportTest`, `RStreamSupportTest`를 작은 실행 예제로 사용합니다. cache와 database를 연결하는 시나리오는 [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)과 [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)으로 이어갑니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 배포 commit의 `infra/redis/build.gradle.kts`를 기준으로 합니다. 우산 모듈은 두 하위 artifact를 함께 제공할 뿐, 공통 facade·자동 client 선택·Spring 설정·cache provider·통합 테스트를 제공하지 않습니다.

README의 `:bluetape4k-redis:test` 안내는 Gradle task 실행 방법이지 우산 모듈 자체에 테스트 코드가 있다는 뜻이 아닙니다. 하위 모듈의 이후 변경을 1.12.1 기능으로 소급해 설명하지 않습니다.

## Source와 links {#sources}

- [`infra/redis/build.gradle.kts`](../../../../infra/redis/build.gradle.kts)
- [`infra/redis/README.ko.md`](../../../../infra/redis/README.ko.md)
- [`bluetape4k-lettuce` 매뉴얼](./bluetape4k-lettuce.md)
- [`bluetape4k-redisson` 매뉴얼](./bluetape4k-redisson.md)
- [`bluetape4k-cache-lettuce` 매뉴얼](./bluetape4k-cache-lettuce.md)
- [`bluetape4k-cache-redisson` 매뉴얼](./bluetape4k-cache-redisson.md)
- [`bluetape4k-spring-boot-redis` 매뉴얼](./bluetape4k-spring-boot-redis.md)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### 모듈 의존성 구조 다이어그램

[![모듈 의존성 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redis-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redis-diagram-01.svg)

_배포본 README: [`infra/redis/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/redis/README.ko.md)_

### 노출 API Surface 다이어그램

[![노출 API Surface 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redis-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redis-diagram-02.svg)

_배포본 README: [`infra/redis/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/redis/README.ko.md)_

<!-- release-readme-diagrams:end -->
