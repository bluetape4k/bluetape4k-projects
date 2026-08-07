---
title: 우산 의존성 계약
description: bluetape4k-redis가 내보내는 두 artifact와 우산 모듈이 제공하지 않는 기능을 구분합니다.
manualId: bluetape4k-redis
chapterId: umbrella-dependency-contract
---

# 우산 의존성 계약

## 두 줄이 전체 공개 계약이다

1.12.1의 [`infra/redis/build.gradle.kts`](../../../../../infra/redis/build.gradle.kts)는 다음 두 의존성만 선언합니다.

```kotlin
dependencies {
    api(project(":bluetape4k-lettuce"))
    api(project(":bluetape4k-redisson"))
}
```

Gradle의 `api` 의존성이므로 `bluetape4k-redis`를 사용하는 module은 Lettuce와 Redisson의 공개 type을 compile classpath에서 볼 수 있고, 두 artifact는 runtime classpath에도 들어옵니다. 우산 artifact는 두 하위 artifact의 버전을 별도로 고르는 기능이 아닙니다. 중앙 `bluetape4k-dependencies` BOM이 같은 배포 묶음으로 정렬합니다.

## 우산 모듈에 없는 것

`infra/redis`에는 `src/main`과 `src/test`의 Kotlin·Java 파일이 없습니다. 따라서 다음 기능을 우산 모듈의 API로 설명하면 안 됩니다.

- 공통 `RedisClient` facade나 client 자동 선택
- Lettuce와 Redisson 사이의 failover
- 공통 Codec 또는 key migration
- Spring bean·property·auto-configuration
- Spring Cache provider나 database loader/writer
- 우산 계층을 검증하는 전용 테스트 suite

실제 client API와 실패 계약은 [Lettuce 매뉴얼](../bluetape4k-lettuce.md)과 [Redisson 매뉴얼](../bluetape4k-redisson.md)에 있습니다.

## 언제 한 좌표가 유용한가

두 client 계열을 이미 함께 사용하는 애플리케이션은 의존성 좌표를 당장 바꾸지 않고 기존 코드를 유지할 수 있습니다. Redisson에서 Lettuce로, 또는 반대 방향으로 이전할 때도 전환 기간에는 두 API를 compile할 수 있습니다.

반대로 새 서비스가 Lettuce command API만 쓴다면 우산 좌표는 사용하지 않는 Redisson과 transitive dependency까지 가져옵니다. 이 경우 처음부터 작은 좌표를 선택하는 편이 build와 운영 책임을 더 정확히 드러냅니다.

## build에서 확인할 항목

```bash
./gradlew :application:dependencies --configuration runtimeClasspath
```

dependency report에서 `bluetape4k-lettuce`와 `bluetape4k-redisson`이 함께 들어오는지 확인합니다. 클래스가 보인다는 사실과 runtime에서 client instance를 생성한다는 사실은 다릅니다. source 사용처와 Spring bean 구성을 따로 조사해야 합니다.

## Release sources

- [`infra/redis/build.gradle.kts`](../../../../../infra/redis/build.gradle.kts)
- [`infra/redis/README.ko.md`](../../../../../infra/redis/README.ko.md)
- [`infra/lettuce/build.gradle.kts`](../../../../../infra/lettuce/build.gradle.kts)
- [`infra/redisson/build.gradle.kts`](../../../../../infra/redisson/build.gradle.kts)
