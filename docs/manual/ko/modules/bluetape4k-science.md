---
manualId: bluetape4k-science
title: "과학 계산 유틸리티"
description: "GIS 좌표 변환, Shapefile 처리, JTS 도형 연산, PostGIS 데이터베이스 파이프라인, NetCDF 메타데이터 카탈로그를 통합 제공하는 Kotlin 모듈입니다."
kind: library
group: utilities
learningOrder: 1280
---

# 과학 계산 유틸리티

## 해결하는 문제 {#problem}

GIS 좌표 변환, Shapefile 처리, JTS 도형 연산, PostGIS 데이터베이스 파이프라인, NetCDF 메타데이터 카탈로그를 통합 제공하는 Kotlin 모듈입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 입력 계약, value semantics, algorithm cost, deterministic output이 필요할 때 `bluetape4k-science`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-science")
}
```

Gradle project path는 `:bluetape4k-science`, source directory는 `utils/science`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `BoundingBox`, `BoundingBoxRelation`, `CoordConverters`, `DM`, `DMS`, `GeoLocation`, `UtmZone`, `UtmZoneSupport`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`BoundingBox`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/BoundingBox.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`BoundingBox`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/BoundingBox.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`BoundingBoxRelation`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/BoundingBoxRelation.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CoordConverters`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/CoordConverters.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`DM`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/DM.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`DMS`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/DMS.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`GeoLocation`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/GeoLocation.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`UtmZone`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/UtmZone.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`UtmZoneSupport`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/UtmZoneSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`Vector`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/Vector.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`NetCdfException`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/exposed/NetCdfException.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **개요**, **아키텍처**, **통합 모듈 구조**, **좌표 변환 흐름**, **PostGIS + NetCDF 데이터베이스 스키마**, **모듈 레이아웃**, **패키지 구조**, **핵심 기능**, **빠른 시작 (Quick Start)**, **5.1 GIS 좌표 변환** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(project(":bluetape4k-core"))
api(project(":bluetape4k-logging"))
api(libs.jts.core)
compileOnly(libs.proj4j)
compileOnly(libs.proj4j.epsg)
compileOnly(libs.esri.geometry.api)
compileOnly(libs.geotools.shapefile)
compileOnly(libs.geotools.referencing)
compileOnly(libs.geotools.epsg.hsql)
compileOnly(libs.ucar.cdm.core)
compileOnly(libs.ucar.netcdf4)
compileOnly(libs.guava)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

hot path를 측정하고 입력 크기를 제한하며 utility를 호출하는 application boundary에서 failure를 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-science:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`BoundingBoxRelationTest`](../../../../utils/science/src/test/kotlin/io/bluetape4k/science/coords/BoundingBoxRelationTest.kt)
- [`BoundingBoxTest`](../../../../utils/science/src/test/kotlin/io/bluetape4k/science/coords/BoundingBoxTest.kt)
- [`CoordConvertersTest`](../../../../utils/science/src/test/kotlin/io/bluetape4k/science/coords/CoordConvertersTest.kt)
- [`DmTest`](../../../../utils/science/src/test/kotlin/io/bluetape4k/science/coords/DmTest.kt)
- [`DmsTest`](../../../../utils/science/src/test/kotlin/io/bluetape4k/science/coords/DmsTest.kt)
- [`GeoLocationTest`](../../../../utils/science/src/test/kotlin/io/bluetape4k/science/coords/GeoLocationTest.kt)
- [`UtmZoneSupportTest`](../../../../utils/science/src/test/kotlin/io/bluetape4k/science/coords/UtmZoneSupportTest.kt)
- [`UtmZoneTest`](../../../../utils/science/src/test/kotlin/io/bluetape4k/science/coords/UtmZoneTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### science 아키텍처

[![science 아키텍처](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-science-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-science-diagram-01.svg)

_배포본 README: [`utils/science/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/science/README.ko.md)_

### Coordinate Transformation 처리 흐름

[![Coordinate Transformation 처리 흐름](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-science-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-science-diagram-02.svg)

_배포본 README: [`utils/science/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/science/README.ko.md)_

### PostGIS + NetCDF 다이어그램

[![PostGIS + NetCDF 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-science-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-science-diagram-03.svg)

_배포본 README: [`utils/science/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/science/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../utils/science/README.ko.md)
- [모듈 build](../../../../utils/science/build.gradle.kts)
- [`BoundingBox`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/BoundingBox.kt)
- [`BoundingBoxRelation`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/BoundingBoxRelation.kt)
- [`CoordConverters`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/CoordConverters.kt)
- [`DM`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/DM.kt)
- [`DMS`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/DMS.kt)
- [`GeoLocation`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/GeoLocation.kt)
- [`UtmZone`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/UtmZone.kt)
- [`UtmZoneSupport`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/UtmZoneSupport.kt)
- [`Vector`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/coords/Vector.kt)
- [`NetCdfException`](../../../../utils/science/src/main/kotlin/io/bluetape4k/science/exposed/NetCdfException.kt)
- [`BoundingBoxRelationTest`](../../../../utils/science/src/test/kotlin/io/bluetape4k/science/coords/BoundingBoxRelationTest.kt)
- [`BoundingBoxTest`](../../../../utils/science/src/test/kotlin/io/bluetape4k/science/coords/BoundingBoxTest.kt)
