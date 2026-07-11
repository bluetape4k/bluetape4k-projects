---
title: 시작하기
description: bluetape4k-projects 모듈을 선택하고 설치한 뒤 가이드, 매뉴얼, 실행 예제로 이동합니다.
---

# 시작하기

## 1. BOM 가져오기

애플리케이션에 사용할 저장소 버전을 선택하고 BOM으로 bluetape4k artifact 버전을 정렬합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
}
```

## 2. 작업에 맞는 모듈 하나 추가하기

필요한 기능을 소유하는 가장 작은 모듈을 선택합니다. 각 모듈 매뉴얼에 정확한 artifact 좌표와 호환성 정보가 있습니다.

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-core")
}
```

## 3. 근거를 따라가기

가이드에서 방향을 잡고, 모듈 매뉴얼에서 계약과 실패 동작을 확인한 뒤 연결된 example 또는 workshop을 실행합니다. README 요약이 모호할 때는 매뉴얼의 source와 test 링크를 최종 근거로 사용합니다.

## 4. 실행 환경 확인하기

이 저장소는 Java 21과 Kotlin 2.3을 사용합니다. Spring Boot, Ktor, 데이터베이스, container, native 조건은 해당 매뉴얼에서 더 좁게 설명합니다.
