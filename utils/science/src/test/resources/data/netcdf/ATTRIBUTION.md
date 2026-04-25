# NetCDF 테스트 샘플 — 출처 / Attribution

본 디렉토리에는 [`NetCdfCatalogService`](../../../../main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt)
의 CF-1.x 회귀 검증용 NetCDF 샘플 파일이 포함되어 있습니다.

## sresa1b_ncar_ccsm3-example.nc

- **다운로드 URL**: <https://www.unidata.ucar.edu/software/netcdf/examples/sresa1b_ncar_ccsm3-example.nc>
- **출처**: [Unidata Program Center](https://www.unidata.ucar.edu/) — NetCDF 공식 examples 페이지
- **원본 데이터**: NCAR Community Climate System Model 3 (CCSM3) — IPCC AR4 SRES A1B scenario simulation
- **다운로드 일자**: 2026-04-25
- **크기**: ~37 KB
- **포맷**: NetCDF-3 Classic, CF-1.x conventions

### 사용 권한 / 라이선스

Unidata 의 NetCDF examples 페이지에는 명시적 라이선스 문구가 없습니다.
Unidata 와 NCAR 가 공개 교육·연구 목적으로 배포하는 샘플 데이터로 일반적으로 자유롭게 재배포·재사용이 가능하다고 알려져 있으나
공식 라이선스 텍스트는 부재하므로 본 프로젝트는 다음 원칙을 따릅니다:

1. 본 파일은 **테스트 회귀 검증 용도** 로만 사용합니다.
2. 본 파일을 재배포하지 않습니다 — 외부에 노출되는 최종 산출물 (Maven 패키지 등) 에서 제외됩니다 (`src/test/resources` 는 production jar 에 포함되지 않음).
3. 사용 권한 / 라이선스 명확화가 필요하면 Unidata 에 문의해야 합니다.

자세한 출처 / 사용 정책 관련 문의는 다음 사이트를 참조하세요:

- <https://www.unidata.ucar.edu/policies/> — Unidata 공식 정책
- <https://www.unidata.ucar.edu/software/netcdf/> — NetCDF 프로젝트
- <https://www.cesm.ucar.edu/projects/ccsm-3.0> — CCSM3 (원본 모델)

### Disclaimer

본 파일은 원저작권자 (Unidata / NCAR) 가 변경 / 삭제 요청 시 즉시 본 디렉토리에서 제거됩니다.
또한 본 attribution 의 정확성은 다운로드 시점 기준이며,
원본 페이지의 라이선스 정책이 변경된 경우 사용자가 별도로 확인해야 합니다.
