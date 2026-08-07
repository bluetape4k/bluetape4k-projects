---
manualId: bluetape4k-examples-jpa-blazepersistence-demo
title: "JPA와 Blaze-Persistence 예제"
description: "이 모듈은 Blaze Persistence를 사용한 JPA 쿼리 예제입니다. Criteria Builder, Entity Views, offset pagination, keyset pagination, count metadata를 다룹니다."
kind: example
group: examples
learningOrder: 1440
---

# JPA와 Blaze-Persistence 예제

## 해결하는 문제 {#problem}

이 모듈은 Blaze Persistence를 사용한 JPA 쿼리 예제입니다. Criteria Builder, Entity Views, offset pagination, keyset pagination, count metadata를 다룹니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 실행 entry point, 필요한 service, 기대 동작, 예제가 보여 주는 production pattern이 필요할 때 `bluetape4k-examples-jpa-blazepersistence-demo`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

이 example project는 Maven artifact로 게시하지 않습니다. 저장소에서 실행하고 명령을 선택하기 전에 Gradle task를 확인합니다.

Gradle project path는 `:bluetape4k-examples-jpa-blazepersistence-demo`, source directory는 `examples/jpa-blazepersistence-demo`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `BlazePersistenceConfiguration`, `MemberPage`, `MemberSearchCondition`, `Member`, `Team`, `MemberBlazeRepository`, `MemberSummaryView`, `MemberTeamView`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

example이나 benchmark를 실행하기 전에 project task를 확인합니다.

```bash
./gradlew :bluetape4k-examples-jpa-blazepersistence-demo:tasks --all
```

그다음 모듈 README에 기록된 명령을 사용하고 필요한 외부 service는 격리합니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`BlazePersistenceConfiguration`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/config/BlazePersistenceConfiguration.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MemberPage`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/dto/MemberPage.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MemberSearchCondition`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/dto/MemberSearchCondition.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`Member`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/model/Member.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`Team`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/model/Team.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MemberBlazeRepository`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/repository/MemberBlazeRepository.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MemberSummaryView`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/view/MemberSummaryView.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MemberTeamView`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/view/MemberTeamView.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **Blaze Persistence를 사용하는 이유**, **예제 범위**, **도메인 모델과 조회 경로**, **핵심 사용법**, **Entity View 등록**, **동적 Criteria Query**, **Entity View Pagination**, **Querydsl Migration Notes**, **의존성**, **실행 방법** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
implementation(project(":bluetape4k-hibernate"))
implementation(libs.jakarta.annotation.api)
implementation(libs.jakarta.persistence.api.v32)
implementation(libs.hibernate.core)
implementation(libs.blaze.persistence.core.api.jakarta)
runtimeOnly(libs.blaze.persistence.core.impl.jakarta)
implementation(libs.blaze.persistence.entity.view.api.jakarta)
runtimeOnly(libs.blaze.persistence.entity.view.impl.jakarta)
implementation(libs.blaze.persistence.jpa.criteria.api.jakarta)
runtimeOnly(libs.blaze.persistence.jpa.criteria.impl.jakarta)
runtimeOnly(libs.blaze.persistence.integration.hibernate7)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

격리된 환경에서 example을 실행하고 startup, dependency health, request, shutdown을 확인합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-examples-jpa-blazepersistence-demo:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractBlazePersistenceTest`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/AbstractBlazePersistenceTest.kt)
- [`BlazePersistenceApplication`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/BlazePersistenceApplication.kt)
- [`TestEntityManager`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/TestEntityManager.kt)
- [`AbstractDomainTest`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/AbstractDomainTest.kt)
- [`MemberBlazeRepositoryTest`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/repository/MemberBlazeRepositoryTest.kt)
- [`InitMemberService`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/services/InitMemberService.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Domain Model and Read Path 다이어그램

[![Domain Model and Read Path 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-jpa-blazepersistence-demo-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-jpa-blazepersistence-demo-diagram-01.svg)

_배포본 README: [`examples/jpa-blazepersistence-demo/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/examples/jpa-blazepersistence-demo/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../examples/jpa-blazepersistence-demo/README.ko.md)
- [모듈 build](../../../../examples/jpa-blazepersistence-demo/build.gradle.kts)
- [`BlazePersistenceConfiguration`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/config/BlazePersistenceConfiguration.kt)
- [`MemberPage`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/dto/MemberPage.kt)
- [`MemberSearchCondition`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/dto/MemberSearchCondition.kt)
- [`Member`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/model/Member.kt)
- [`Team`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/model/Team.kt)
- [`MemberBlazeRepository`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/repository/MemberBlazeRepository.kt)
- [`MemberSummaryView`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/view/MemberSummaryView.kt)
- [`MemberTeamView`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/view/MemberTeamView.kt)
- [`AbstractBlazePersistenceTest`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/AbstractBlazePersistenceTest.kt)
- [`BlazePersistenceApplication`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/BlazePersistenceApplication.kt)
- [`TestEntityManager`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/TestEntityManager.kt)
- [`AbstractDomainTest`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/AbstractDomainTest.kt)
