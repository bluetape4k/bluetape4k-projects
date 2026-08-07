---
manualId: bluetape4k-bom
title: "Bluetape4k BOM"
description: 중앙 BOM이 내부에서 사용하는 bluetape4k-projects 모듈 제약 조건과 유지보수 방식을 설명합니다.
kind: library
group: foundation
learningOrder: 100
---

# Bluetape4k BOM

## 해결하는 문제 {#problem}

애플리케이션은 보통 bluetape4k 모듈을 여러 개 사용합니다. 모듈마다 버전을 반복하면 upgrade diff가 커지고, 함께 검증하지 않은 release 조합으로 classpath가 구성될 수 있습니다. `bluetape4k-bom`은 이 저장소에서 게시하는 모듈을 Gradle platform constraint로 묶습니다.

## 사용 시점 {#when-to-use}

일반 애플리케이션은 이 저장소 BOM을 직접 선택하지 않고 `bluetape4k-dependencies`를 가져옵니다. 중앙 BOM이 `bluetape4k-bom`을 비롯한 저장소별 BOM을 검증된 조합으로 묶어 줍니다.

`bluetape4k-bom`을 직접 다루는 경우는 저장소를 배포하거나 platform constraint를 점검하거나 dependency 충돌을 재현할 때입니다. 이 페이지의 나머지 내용도 그런 유지보수 관점에서 읽으면 됩니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-core")
    implementation("io.github.bluetape4k:bluetape4k-coroutines")
}
```

위 `<version>`은 `bluetape4k-dependencies`의 배포 버전입니다. `bluetape4k-bom`의 내부 버전을 애플리케이션에 따로 적지 않습니다.

애플리케이션에서 BOM constraint가 다른 모든 선택보다 우선해야 할 때만 `enforcedPlatform(...)`을 사용합니다. library는 consumer의 dependency resolution 권한을 남기기 위해 보통 `platform(...)`을 사용해야 합니다.

## 핵심 개념 {#concepts}

이 모듈은 Gradle `java-platform` plugin을 적용합니다. publication에는 runtime class가 아니라 dependency constraint가 들어갑니다. build는 root subproject에서 constraint를 만들고 BOM 자체, workshop, `examples`, `-demo` project는 제외합니다.

## 빠른 시작 {#quick-start}

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-logging")
}
```

BOM이 constraint를 제공하는 artifact만 버전을 생략합니다. 다른 library 버전은 별도의 platform이 관리하지 않는 한 명시해야 합니다.

## 작업별 API {#api-by-task}

| 작업 | Build 표현식 |
| --- | --- |
| downstream resolution을 허용하며 버전 정렬 | `platform(...)` |
| 애플리케이션에서 선택 버전 강제 | `enforcedPlatform(...)` |
| 특정 artifact의 선택 근거 확인 | `./gradlew dependencyInsight --dependency bluetape4k-core` |
| configuration의 전체 graph 확인 | `./gradlew dependencies --configuration runtimeClasspath` |

## 권장 패턴 {#patterns}

service 모듈이 공통으로 쓰는 convention plugin이나 version catalog alias에서 중앙 platform을 한 번 선언합니다. 그 경계에서는 `bluetape4k-dependencies` 버전만 명시하고 개별 bluetape4k dependency 버전은 생략합니다. 중앙 BOM upgrade는 하나의 review 단위로 만들고 애플리케이션 compile과 integration test를 함께 실행합니다.

## 연동 {#integrations}

Spring dependency-management plugin과 Gradle platform이 동시에 resolution에 관여할 수 있습니다. Spring build convention에 명확한 이유가 없다면 native Gradle platform을 우선합니다. version catalog는 BOM 좌표를 alias로 제공할 수 있지만 platform constraint를 대신하지 않습니다.

## 설정 {#configuration}

runtime property는 없습니다. SNAPSHOT을 사용할 때는 consumer build에 Sonatype Central snapshots repository를 추가해야 합니다. release artifact는 보통 Maven Central만으로 resolve됩니다.

## 실패 동작 {#failures}

repository가 없으면 compile 전에 dependency resolution이 실패합니다. 새 모듈을 포함하지 않은 예전 BOM을 가져오면 그 모듈에는 constraint가 없으므로 버전을 따로 적어야 합니다. 서로 충돌하는 enforced platform은 resolution을 실패시키거나 검증하지 않은 버전을 선택할 수 있습니다.

## 운영 {#operations}

monitoring할 runtime component는 없습니다. 운영 작업은 dependency governance에 가깝습니다. resolved version을 확인하고, 보안 upgrade를 추적하고, platform 버전을 바꿀 때 애플리케이션 전체를 테스트합니다.

## 테스트 {#testing}

이 모듈에는 Kotlin test source가 없습니다. Gradle dependency report와 publication metadata로 모델을 검증합니다.

```bash
./gradlew :bluetape4k-bom:dependencies --no-configuration-cache
./gradlew :bluetape4k-bom:generatePomFileForBluetape4kPublication --no-configuration-cache
```

두 번째 명령은 로컬에 publication metadata만 만들며 artifact를 게시하지 않습니다.

## 워크숍 {#workshops}

등록된 전용 workshop은 없습니다. BOM과 두 모듈을 사용하는 작은 consumer build를 만든 뒤 `runtimeClasspath`를 확인하고, BOM 버전만 올려 resolution 결과가 함께 바뀌는지 확인할 수 있습니다.

## 제한 사항 {#limitations}

BOM은 버전을 정렬하지만 각 모듈이 애플리케이션의 framework, JDK, database, native-image 조건에 맞는지 보장하지 않습니다. demo와 example은 게시 library가 아니므로 의도적으로 제외됩니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### bluetape4k-bom platform constraints 다이어그램

[![bluetape4k-bom platform constraints 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-bom-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-bom-diagram-01.svg)

_배포본 README: [`bluetape4k/bom/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/bom/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../bluetape4k/bom/README.ko.md)
- [Platform constraint build](../../../../bluetape4k/bom/build.gradle.kts)
- [저장소 publication group](../../../../gradle.properties)
- [매뉴얼 manifest](../../manifest.yaml)
