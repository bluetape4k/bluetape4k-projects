# Issue #1343 결과 기록 — utils/science NetCDF 문서 정합화

이 문서는 Issue [#1343](https://github.com/bluetape4k/bluetape4k-projects/issues/1343)의
README 정합화 결과를 보존합니다. 초기 초안의 “전체 README 재작성” 범위는
현재 구현 계약을 설명하는 문서 변경으로 좁혔습니다.

## 범위와 비범위

- 변경 대상: [`utils/science/README.md`](../../utils/science/README.md),
  [`utils/science/README.ko.md`](../../utils/science/README.ko.md)
- 함께 정리한 기록: 이 문서
- 비범위: `utils/science` Kotlin 소스, 스키마, 테스트 로직, 공개 API 동작 변경
- 후속 기능: `CoordinateAxis2D`와 CF auxiliary-coordinate 격자 임포트는
  [#1352](https://github.com/bluetape4k/bluetape4k-projects/issues/1352)에서 별도 진행

## 현재 계약

README는 다음 기준 정보를 한·영 동일하게 설명합니다.

| 영역 | 현재 계약 | 근거 |
|------|-----------|------|
| 서비스 API | 동기(blocking) `registerFile()`과 `importGridValues()` | [`NetCdfCatalogService.kt`](../../utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt) |
| 변수 범위 | rank 1~4, 1차원 좌표축 | 서비스 KDoc 및 `VariableAxisMap` |
| 저장 의미 | rank별 `timeIdx`·`levelIdx`·`location` 매핑 | 서비스 KDoc, `NetCdfGridValueTable` |
| 재개·동시성 | `(fileId, variableName)`별 5분 heartbeat lease와 `lastSliceIdx` cursor | `NetCdfImportProgressRepository`, 서비스 테스트 |
| CRS | EPSG:4326/4269/3857/3031/3413 및 UTM 32601~32660·32701~32760 화이트리스트 | `CoordinateReprojector` 및 서비스 테스트 |
| 결측값 | NaN·`_FillValue` skip, `netcdf.import.nan.skipped` 계측 | 서비스 구현 및 테스트 |
| 의존성 | UCAR netCDF-Java 5.9.1 `cdm-core`·`netcdf4`, 모듈은 `compileOnly` | [`build.gradle.kts`](../../utils/science/build.gradle.kts) |
| 느린 회귀 | `slow-netcdf` 태그, 기본 제외·nightly 명시 실행 | [`NetCdfCatalogServiceTest.kt`](../../utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt), [`nightly-tests.yml`](../../.github/workflows/nightly-tests.yml) |

## README에 반영한 내용

1. 개요·모듈 레이아웃·기능 표의 NetCDF 상태를 구현 완료로 갱신했습니다.
2. `registerFile()`과 `importGridValues()`를 실제 생성자와 호출 순서로 보여주는
   빠른 시작 예제를 추가했습니다.
3. rank 1~4 저장 좌표, lease/resume, CRS 화이트리스트, NaN/`_FillValue`,
   `NetCdfException` 계약을 사용자 관점에서 설명했습니다.
4. 이전 aggregate 의존성·미구현 설명을 제거하고 5.9.1 모듈 좌표와
   `compileOnly` 런타임 제공 책임을 기록했습니다.
5. 기본 테스트와 `-PincludeTags=slow-netcdf` nightly 프로필을 분리해 설명했습니다.
6. EN/KO locale 링크와 기능 범위가 서로 대응하는지 확인할 수 있도록 같은 표와
   예제 구조를 유지했습니다.

## 실행 예시

```kotlin
val catalog = NetCdfCatalogService(
    fileRepo = NetCdfFileRepository(),
    progressRepo = NetCdfImportProgressRepository(),
)
val fileId = catalog.registerFile("/data/era5/ERA5_2024_01.nc")
catalog.importGridValues(fileId, variableName = "temperature")
```

호출 애플리케이션은 다음 런타임 의존성을 제공해야 합니다.

```kotlin
implementation("edu.ucar:cdm-core:5.9.1")
implementation("edu.ucar:netcdf4:5.9.1")
```

## 후속 연결

문서 계약이 확정되었으므로 Epic [#1421](https://github.com/bluetape4k/bluetape4k-projects/issues/1421)의
다음 단계는 #1352입니다. #1352에서는 2D 좌표축·CF auxiliary coordinate fixture,
셀 좌표 보존, 지원·비지원 CRS, 기존 rank 1~4 및 lease/resume 회귀를 코드와 테스트로
고정해야 합니다.

## DoD Status

- 상태: 문서 정합화 완료
- 코드 동작 변경: 없음
- EN/KO README 범위·의존성·quick start·테스트 프로필: 반영
- 후속 기능 #1352: 별도 대기
